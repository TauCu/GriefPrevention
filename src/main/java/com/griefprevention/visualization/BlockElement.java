package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An element of a {@link BlockBoundaryVisualization}.
 */
public abstract class BlockElement
{

    protected final @NotNull Player player;
    protected final @NotNull World world;
    protected final @NotNull IntVector coordinate;

    /**
     * Construct a new {@code BlockElement} with the given coordinate.
     *
     * @param player the {@code Player} visualizing the element
     * @param coordinate the in-world coordinate of the element
     */
    public BlockElement(@NotNull Player player, @NotNull IntVector coordinate) {
        this.player = player;
        this.world = player.getWorld();
        this.coordinate = coordinate;
    }

    /**
     * Get the in-world coordinate of the element.
     *
     * @return the coordinate
     */
    public @NotNull IntVector getCoordinate()
    {
        return coordinate;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Display the element
     */
    protected abstract void draw();

    /**
     * Stop the display of the element
     */
    protected abstract void erase();

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other)
            return true;
        if (other == null || !getClass().equals(other.getClass()))
            return false;
        BlockElement that = (BlockElement) other;
        return player.equals(that.player) && coordinate.equals(that.coordinate);
    }

    @Override
    public int hashCode() {
        int result = player.hashCode();
        result = 31 * result + coordinate.hashCode();
        return result;
    }

}
