package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.Boundary;
import com.griefprevention.visualization.EntityBlockBoundaryVisualization;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * This visualization uses fake entities that glow (can be seen through other blocks)<br>
 * It requires ProtocolLib to work.
 * @author <a href="https://github.com/TauCu">TauCubed</a>
 */
public class FakeBlockDisplayVisualization extends EntityBlockBoundaryVisualization<FakeBlockDisplayElement> {

    private static final HashMap<BlockData, Boolean> FLOOR_BLOCK_CACHE = new HashMap<>(1024, 0.5F);

    /**
     * Construct a new {@link FakeBlockDisplayVisualization}.
     *
     * @param player         the {@link Player} to visualize for
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height        the height of the visualization
     */
    public FakeBlockDisplayVisualization(@NotNull Player player, @NotNull IntVector visualizeFrom, int height) {
        super(player, visualizeFrom, height);
    }

    public FakeBlockDisplayVisualization(Player player, IntVector visualizeFrom, int height, int step, int displayZoneRadius) {
        super(player, visualizeFrom, height, step, displayZoneRadius);
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary) {
        return switch (boundary.type()) {
            case ADMIN_CLAIM ->
                    addElement(Material.ORANGE_STAINED_GLASS.createBlockData(), ChatColor.GOLD.asBungee().getColor());
            case SUBDIVISION ->
                    addElement(Material.WHITE_STAINED_GLASS.createBlockData(), ChatColor.WHITE.asBungee().getColor());
            case INITIALIZE_ZONE ->
                    addElement(Material.LIGHT_BLUE_STAINED_GLASS.createBlockData(), ChatColor.AQUA.asBungee().getColor());
            case CONFLICT_ZONE ->
                    addElement(Material.RED_STAINED_GLASS.createBlockData(), ChatColor.RED.asBungee().getColor());
            default ->
                    addElement(Material.YELLOW_STAINED_GLASS.createBlockData(), ChatColor.YELLOW.asBungee().getColor());
        };
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary) {
        return addCornerElements(boundary);
    }

    protected @NotNull Consumer<@NotNull IntVector> addElement(@NotNull BlockData blockData, @NotNull Color color) {
        return vector -> {
            // don't draw over existing elements in the same position
            entityElements.putIfAbsent(vector, new FakeBlockDisplayElement(player, vector, color, blockData));
        };
    }

    @Override
    public void revert() {
        FakeBlockDisplayElement.eraseAllEntities(player, entityElements.values());
    }

    @Override
    public boolean isValidFloor(int originalY, int x, int y, int z) {
        return isFloor(world, originalY, x, y, z);
    }

    public static boolean isFloor(World world, int originalY, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        return isFloorBlock(block) && (!isFloorBlock(block.getRelative(BlockFace.UP)) || !isFloorBlock(block.getRelative(BlockFace.DOWN)));
    }

    public static boolean isFloorBlock(Block block) {
        Boolean isFullBlock = FLOOR_BLOCK_CACHE.get(block.getBlockData());
        if (isFullBlock == null) {
            isFullBlock = !block.isPassable()
                    && !Tag.LEAVES.isTagged(block.getType());
            if (isFullBlock) {
                Collection<BoundingBox> aabbs = block.getCollisionShape().getBoundingBoxes();
                if (!aabbs.isEmpty()) {
                    isFullBlock = aabbs.stream().mapToDouble(org.bukkit.util.BoundingBox::getVolume).sum() >= 0.8;
                }
                isFullBlock = isFullBlock && block.getBoundingBox().getVolume() >= 0.8;
            }
            // ensure that the same blockData is returned, otherwise this cache won't work and might memory leak
            if (block.getBlockData().hashCode() == block.getBlockData().hashCode())
                FLOOR_BLOCK_CACHE.put(block.getBlockData(), isFullBlock);
        }
        return isFullBlock;
    }

    @Override
    public boolean isValidFloor(Block block) {
        throw new UnsupportedOperationException("not implemented. use isValidFloor(org.bukkit.World, int, int, int, int)");
    }

}
