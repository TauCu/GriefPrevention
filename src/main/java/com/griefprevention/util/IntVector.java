package com.griefprevention.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An immutable integer-based vector.
 */
public record IntVector(int x, int y, int z)
{

    /**
     * Construct a new {@code IntVector} representing the specified {@link Block}.
     *
     * @param block the {@code Block}
     */
    public IntVector(@NotNull Block block)
    {
        this(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Construct a new {@code IntVector} representing the specified {@link Location}.
     *
     * @param location the {@code Location}
     */
    public IntVector(@NotNull Location location)
    {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Construct a new {@code IntVector} representing the specified {@link Vector}.
     *
     * @param vector the {@code Vector}
     */
    public IntVector(@NotNull Vector vector)
    {
        this(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    /**
     * Get a {@link Block} representing the {@code IntVector} in the specified {@link World}.
     *
     * @param world the {@code World}
     * @return the corresponding {@code Block}
     */
    public @NotNull Block toBlock(@NotNull World world)
    {
        return world.getBlockAt(x(), y(), z());
    }

    /**
     * Get a {@link Location} representing the {@code IntVector} in the specified {@link World}, if any.
     *
     * @param world the optional {@code World}
     * @return the corresponding {@code Location}
     */
    @Contract("_ -> new")
    public @NotNull Location toLocation(@Nullable World world)
    {
        return new Location(world, x(), y(), z());
    }

    /**
     * Get a {@link Vector} representing the {@code IntVector}.
     *
     * @return the corresponding {@code Vector}
     */
    @Contract(" -> new")
    public @NotNull Vector toVector()
    {
        return new Vector(x(), y(), z());
    }

    /**
     * Create a new {@code IntVector} with the specified offset.
     *
     * @param dX the X coordinate offset
     * @param dY the Y coordinate offset
     * @param dZ the Z coordinate offset
     * @return the {@code IntVector} created
     */
    @Contract("_, _, _ -> new")
    public @NotNull IntVector add(int dX, int dY, int dZ)
    {
        return new IntVector(x() + dX, y() + dY, z() + dZ);
    }

    /**
     * Create a new {@code IntVector} with the specified offset.
     *
     * @param other the offset {@code IntVector}
     * @return the {@code IntVector} created
     */
    @Contract("_ -> new")
    public @NotNull IntVector add(@NotNull IntVector other)
    {
        return new IntVector(x() + other.x(), y() + other.y(), z() + other.z());
    }

    /**
     * Create a new {@code IntVector} with the specified offset.
     *
     * @param dX the X coordinate offset
     * @param dY the Y coordinate offset
     * @param dZ the Z coordinate offset
     * @return the {@code IntVector} created
     */
    @Contract("_, _, _ -> new")
    public @NotNull IntVector subtract(int dX, int dY, int dZ)
    {
        return new IntVector(x() - dX, y() - dY, z() - dZ);
    }

    /**
     * Create a new {@code IntVector} with the specified offset.
     *
     * @param other the offset {@code IntVector}
     * @return the {@code IntVector} created
     */
    @Contract("_ -> new")
    public @NotNull IntVector subtract(@NotNull IntVector other)
    {
        return new IntVector(x() - other.x(), y() - other.y(), z() - other.z());
    }

    public int dot(IntVector other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public IntVector cross(IntVector other) {
        return new IntVector(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    /**
     * Calculate the squared distance to another {@code IntVector}.
     *
     * @param other the other {@code IntVector}
     * @return the squared distance
     */
    public int distanceSquared(@NotNull IntVector other)
    {
        int dX = x() - other.x();
        int dY = y() - other.y();
        int dZ = z() - other.z();
        return dX * dX + dY * dY + dZ * dZ;
    }

    /**
     * Check if the {@link org.bukkit.Chunk Chunk} containing the {@link Block} at the {@code IntVector's} coordinates
     * is loaded.
     *
     * @param world the {@link World}
     * @return true if the block is loaded
     */
    public boolean isChunkLoaded(@NotNull World world)
    {
        // Note: Location#getChunk et cetera load the chunk!
        // Only World#isChunkLoaded is safe without a Chunk object.
        return world.isChunkLoaded(x() >> 4, z() >> 4);
    }

}
