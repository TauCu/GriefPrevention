package me.ryanhamshire.GriefPrevention.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic class containing utilities to work with the minecraft protocol.
 * @author <a href="https://github.com/TauCu">TauCubed</a>
 */
public class ProtocolUtil {

    private static Method m_craftBlockData_getState = null;
    private static Method m_nmsBlock_getId = null;
    private static AtomicInteger nmsEntity_entityCounter = null;

    /**
     * Gets the NMS registry state ID for the given block data.
     * @param data the block data
     * @return the NMS registry state ID.
     * @deprecated no longer used.
     */
    @Deprecated(forRemoval = true)
    public static int getBlockStateId(BlockData data) {
        try {
            if (m_craftBlockData_getState == null || m_nmsBlock_getId == null) {
                // use reflection to obtain the <nms.BlockState> getState method in CraftBlockData.
                Class<?> craftBlockDataClazz = MinecraftReflection.getCraftBukkitClass("block.data.CraftBlockData");
                Method M_craftBlockData_getState = craftBlockDataClazz.getMethod("getState");
                M_craftBlockData_getState.setAccessible(true);
                ProtocolUtil.m_craftBlockData_getState = M_craftBlockData_getState;

                // use fuzzy reflection to find getId method in the nms.Block class.
                // this will lookup and return the registry state ID for the given nms.BlockState reference.
                // we'll just have to hope there isn't another public static method that returns int and accepts exactly nms.BlockState in the nms.Block class.
                FuzzyReflection blockReflector = FuzzyReflection.fromClass(MinecraftReflection.getBlockClass());
                m_nmsBlock_getId = blockReflector.getMethod(FuzzyMethodContract.newBuilder()
                        .banModifier(Modifier.PRIVATE)
                        .banModifier(Modifier.PROTECTED)
                        .requireModifier(Modifier.STATIC)
                        .parameterExactArray(MinecraftReflection.getIBlockDataClass())
                        .returnTypeExact(int.class)
                        .build());
            }

            // invoke getState to get the nms.BlockState of the CraftBlockData
            Object nmsState = m_craftBlockData_getState.invoke(data);
            // invoke getId to get the nms registry state ID of the nms.BlockState
            return (int) m_nmsBlock_getId.invoke(null, nmsState);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Increments and gets the next server entity ID.
     * @return the next server entity ID.
     */
    public static int nextEntityId() {
        if (nmsEntity_entityCounter == null) {
            try {
                // the entity class has a static final AtomicInteger that is incremented each time an entity is spawned.
                // we'll use fuzzy reflection to obtain & store the reference to the object, so we can call it with no reflective overhead.
                FuzzyReflection entityReflector = FuzzyReflection.fromClass(MinecraftReflection.getEntityClass(), true);
                List<Field> possibleFields = entityReflector.getFieldList(
                        FuzzyFieldContract.newBuilder()
                                .requireModifier(Modifier.STATIC)
                                .requireModifier(Modifier.FINAL)
                                .typeDerivedOf(AtomicInteger.class)
                                .build());

                World world = Bukkit.getWorlds().get(0);
                Location entityLoc = new Location(world, 0, world.getMinHeight() - 2, 0);

                // it's possible that there is more than one static final AtomicInteger in the entity class.
                // to make sure it's the right one, create a bunch of entities and make sure their ids == the counter's id.
                for (Field check : possibleFields) {
                    check.setAccessible(true);
                    AtomicInteger possibleCounter = (AtomicInteger) check.get(null);

                    // some stuff might be creating new entity instances on different threads, thus check a few times
                    int matchedTimes = 0;
                    for (int i = 0; i < 1000; i++) {
                        BlockDisplay entity = world.createEntity(entityLoc, BlockDisplay.class);

                        if (entity.getEntityId() == possibleCounter.get()) {
                            matchedTimes++;
                        }
                    }

                    // make sure the apparent consistency is >95%
                    if (matchedTimes > 950) {
                        nmsEntity_entityCounter = possibleCounter;
                        break;
                    }
                }

                // if we didn't find it, it has likely changed between versions, perhaps now it's an AtomicLong.
                // in this case, we'll just make one up. It might cause conflicts, oh well.
                if (nmsEntity_entityCounter == null) {
                    Logger.getLogger("GriefPrevention").log(Level.WARNING, "Failed to find nmsEntity_entityCounter. We'll create a fallback but it might cause id conflicts causing things such as ghost entities.");
                    // use max value so it will flip negative and be unlikely to cause conflicts with real entities.
                    // obviously if two plugins do this it will be a shit shoot.
                    nmsEntity_entityCounter = new AtomicInteger(Integer.MAX_VALUE);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        // increment and get entity counter
        return nmsEntity_entityCounter.incrementAndGet();
    }

    /**
     * Extracts the data from all data watchers in the given entity and returns it as a list of WrappedDataValue<br>
     * This method also extracts and sets the serializers for each data value.
     * @param entity the entity to extract (doesn't need to be alive or spawned)
     * @return a list of all the data in the entity
     */
    public static List<WrappedDataValue> extractWatchableDataFrom(Entity entity) {
        return WrappedDataWatcher.getEntityWatcher(entity)
                .getWatchableObjects().stream()
                .map(obj -> new WrappedDataValue(obj.getIndex(), obj.getWatcherObject().getSerializer(), obj.getRawValue()))
                .toList();
    }

    /**
     * Extracts all watchable data from the given entity and creates a ENTITY_METADATA packet containing
     * that data, and the entityId.
     * @param entity the entity to create the packet for
     * @return a PacketContainer containing the entity's watchable data and the entityId.
     */
    public static PacketContainer createMetadataPacketFor(Entity entity) {
        return createMetadataPacketFor(entity, entity.getEntityId());
    }

    /**
     * Extracts all watchable data from the given entity and creates a ENTITY_METADATA packet containing
     * that data, and the entityId.
     * @param entity the entity to create the packet for
     * @param entityId the entity id this packet targets
     * @return a PacketContainer containing the entity's watchable data and the entityId.
     */
    public static PacketContainer createMetadataPacketFor(Entity entity, int entityId) {
        PacketContainer entityMeta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        entityMeta.getIntegers().write(0, entityId);
        entityMeta.getDataValueCollectionModifier().write(0, extractWatchableDataFrom(entity));
        return entityMeta;
    }

    /**
     * Fairly self-explanatory, instructs the players' client to destroy entities with the given IDs
     * @param whom the player to send the packet to.
     * @param entityIds the entity IDs to destroy
     */
    public static void destroyEntitiesFor(Player whom, List<Integer> entityIds) {
        if (!entityIds.isEmpty()) {
            PacketContainer destroyEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyEntity.getIntLists().write(0, entityIds); // list of entityId's to destroy
            ProtocolLibrary.getProtocolManager().sendServerPacket(whom, destroyEntity);
        }
    }

}
