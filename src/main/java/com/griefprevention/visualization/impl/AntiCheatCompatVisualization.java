package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * A {@link FakeBlockVisualization} with maximum anti-cheat compatibility.
 */
public class AntiCheatCompatVisualization extends FakeBlockVisualization
{

    /**
     * Construct a new {@code AntiCheatCompatVisualization}.
     *
     * @param player the {@link Player} to visualize for
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     */
    public AntiCheatCompatVisualization(@NotNull Player player, @NotNull IntVector visualizeFrom, int height)
    {
        super(player, visualizeFrom, height);
    }

    @Override
    public boolean isValidFloor(Block block)
    {
        Collection<BoundingBox> boundingBoxes = block.getCollisionShape().getBoundingBoxes();
        // Decide transparency based on whether block physical bounding box occupies the entire block volume.
        return boundingBoxes.isEmpty() || !boundingBoxes.stream().allMatch(box -> box.getVolume() == 1.0);
    }

}
