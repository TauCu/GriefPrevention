package com.griefprevention.visualization.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.FakeEntityElement;
import me.ryanhamshire.GriefPrevention.util.ProtocolUtil;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;
import java.util.Objects;

/**
 * Fake block display visualization element for boundary visualization.<br>
 * Used to spawn fake block displays that have the glowing effect
 * @author <a href="https://github.com/TauCu">TauCubed</a>
 */
public class FakeBlockDisplayElement extends FakeEntityElement {

    protected IntVector toCoordinate;
    Color color;
    BlockData blockData;
    float scale;

    public FakeBlockDisplayElement(Player player, IntVector coordinate, Color color, BlockData blockData) {
        this(player, coordinate, coordinate, color, blockData, 0.998F);
    }

    public FakeBlockDisplayElement(Player player, IntVector from, IntVector to, Color color, BlockData blockData, float scale) {
        super(player, from);
        this.toCoordinate = to;
        this.color = color;
        this.blockData = blockData;
        this.scale = scale;
    }

    @Override
    protected void onDraw() {
        Vector centerOffset = new Vector(0.5, 0.5, 0.5);
        Vector fromVec = getCoordinate().toVector().add(centerOffset);
        Vector toVec = getToCoordinate().toVector().add(centerOffset);

        // create unspawned entity and setup data
        BlockDisplay entity = world.createEntity(fromVec.toLocation(world), BlockDisplay.class);
        entity.setBlock(blockData);
        entity.setGlowing(true);
        entity.setGlowColorOverride(org.bukkit.Color.fromARGB(color.getRGB()));
        entity.setViewRange(1000F);
        entity.setShadowStrength(0F);
        entity.setBrightness(new Display.Brightness(15, 15));

        // are we drawing a point or a line?
        Vector dir = toVec.clone().subtract(fromVec);
        double length = dir.length();
        if (length < 0.00001) {
            entity.setTransformation(new Transformation(
                    new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()
            ));
        } else {
            Vector3f forward = dir.clone().multiply(1.0 / length).toVector3f();

            boolean isVertical = Math.abs(forward.x) < 0.000001 && Math.abs(forward.z) < 0.000001;
            Vector3f right = new Vector3f(0, isVertical ? 0 : 1, isVertical ? 1 : 0)
                    .cross(forward).normalize();
            Vector3f up = new Vector3f(forward).cross(right);
            Matrix3f basis = new Matrix3f(
                    right.x, right.y, right.z,
                    up.x, up.y, up.z,
                    forward.x, forward.y, forward.z
            );
            Quaternionf rotation = new Quaternionf().setFromNormalized(basis);

            entity.setTransformation(new Transformation(
                    new Vector3f(-scale / 2, -scale / 2, -scale / 2).rotate(rotation),
                    rotation,
                    new Vector3f(scale, scale, scale + (float) length),
                    new Quaternionf()
            ));
        }

        /* debugging
        System.out.println("Dumping metadata packet:");
        for (WrappedDataValue val : ProtocolUtil.createMetadataPacketFor(entity, entityId).getDataValueCollectionModifier().read(0)) {
            System.out.println("DataWatcher [%s]: %s".formatted(val.getIndex(), val.getRawValue()));
        }
         */

        this.entityId = entity.getEntityId();
        this.entityUid = entity.getUniqueId();

        // create spawn packet
        PacketContainer addEntity = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        addEntity.getEntityTypeModifier().write(0, EntityType.BLOCK_DISPLAY);
        addEntity.getIntegers().write(0, entityId);
        addEntity.getUUIDs().write(0, entityUid);
        addEntity.getDoubles()
                .write(0, fromVec.getX())
                .write(1, fromVec.getY())
                .write(2, fromVec.getZ());

        // create metadata packet from entity
        PacketContainer metadataPacket = ProtocolUtil.createMetadataPacketFor(entity);

        // must send addEntity before meta
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, addEntity);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, metadataPacket);
    }

    public IntVector getToCoordinate() {
        return toCoordinate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FakeBlockDisplayElement that = (FakeBlockDisplayElement) o;
        return Float.compare(scale, that.scale) == 0 && toCoordinate.equals(that.toCoordinate) && color.equals(that.color) && blockData.equals(that.blockData)
                && player.equals(that.player) && entityId == that.entityId && drawn == that.drawn && Objects.equals(entityUid, that.entityUid);
    }

    @Override
    public int hashCode() {
        int result = (coordinate.hashCode() + toCoordinate.hashCode());
        result = 31 * result + color.hashCode();
        result = 31 * result + blockData.hashCode();
        result = 31 * result + Float.hashCode(scale);
        return result;
    }

}
