package com.griefprevention.visualization;

import com.griefprevention.events.BoundaryVisualizationEvent;
import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.CustomLogEntryTypes;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * A representation of a system for displaying rectangular {@link Boundary Boundaries} to {@link Player Players}.
 * <p>
 * This is used to display claim areas, conflicting claims, and more.
 */
public abstract class BoundaryVisualization
{

    protected final Collection<Boundary> boundaries = new ArrayList<>();
    protected final Collection<Boundary> elements = boundaries;
    protected final @NotNull Player player;
    protected final @NotNull World world;
    protected final @NotNull IntVector visualizeFrom;
    protected final int height;

    protected final int worldMaxHeight;
    protected final int worldMinHeight;

    /**
     * Construct a new {@code BoundaryVisualization}.
     *
     * @param player the {@link Player} this visualization is for
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     */
    protected BoundaryVisualization(@NotNull Player player, @NotNull IntVector visualizeFrom, int height) {
        this.player = player;
        this.world = player.getWorld();
        this.visualizeFrom = visualizeFrom;
        this.height = height;
        this.worldMaxHeight = world.getMaxHeight();
        this.worldMinHeight = world.getMinHeight();
    }

    /**
     * Check if a {@link Player} can visualize the {@code BoundaryVisualization}.
     *
     * @return true if able to visualize
     */
    protected boolean canVisualize() {
        return !boundaries.isEmpty() && Objects.equals(world, player.getWorld());
    }

    /**
     * Apply the {@code BoundaryVisualization} to a {@link Player}.
     */
    protected void apply() {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

        // Remember the visualization so it can be reverted.
        playerData.setVisibleBoundaries(this);

        // Apply all visualization elements.
        for (Boundary boundary : boundaries)
            draw(boundary);

        // Schedule automatic reversion.
        scheduleRevert();
    }

    /**
     * Draw a {@link Boundary} in the visualization.
     *
     * @param boundary the {@code Boundary} to draw
     */
    protected abstract void draw(@NotNull Boundary boundary);

    /**
     * Schedule automatic reversion of the visualization.
     *
     * <p>Some implementations may automatically revert without additional help and may wish to override this method to
     * prevent extra task scheduling.</p>
     *
     */
    protected void scheduleRevert() {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(
                GriefPrevention.instance,
                () -> {
                    // Only revert if this is the active visualization.
                    if (playerData.getVisibleBoundaries() == this) {
                        playerData.setVisibleBoundaries(null);
                    }
                },
                20L * 60);
    }

    public Collection<Boundary> getBoundaries() {
        return Collections.unmodifiableCollection(this.boundaries);
    }

    /**
     * Revert the visualization
     */
    public void revert() {
        // If the player cannot visualize the blocks, they should already be effectively reverted.
        if (!canVisualize()) {
            return;
        }

        // Revert data as necessary for any sent elements.
        for (Boundary boundary : boundaries)
            erase(boundary);
    }

    /**
     * Erase a {@link Boundary} in the visualization.
     *
     * @param boundary the {@code Boundary} to erase
     */
    protected abstract void erase(@NotNull Boundary boundary);

    /**
     * Checks for a valid floor traversing up and down between y - 64 and y + 16<br>
     * You may override this method to change the height limits used by the draw method as it calls this method.
     * @param x the x coordinate
     * @param y the starting y coordinate
     * @param z the z coordinate
     * @return the Y coordinate of the floor or y - 2 if no floor is found
     * @see #findFloor(int, int, int, int, int, int)
     * @see #isValidFloor(int, int, int, int)
     */
    public int findFloor(int x, int y, int z) {
        return findFloor(x, y, z, Math.max(worldMinHeight, y - 80), Math.min(worldMaxHeight, y + 64), y - 2);
    }

    /**
     * Checks for a valid floor traversing up and down within the specified limits
     * @param x the x coordinate
     * @param y the starting y coordinate
     * @param z the z coordinate
     * @param minY the minimum Y value that will be searched
     * @param maxY the maximum Y value that will be searched
     * @param def the default return value if no floor is found
     * @return the Y coordinate of the floor or def if no floor is found
     * @see #isValidFloor(int, int, int, int)
     * @see #isValidFloor(Block)
     */
    public int findFloor(int x, int y, int z, int minY, int maxY, int def) {
        // search up and down within the specified limits
        int ly = y + 1, gy = y - 1;
        int maxAbs = Math.max(Math.abs(minY), Math.abs(maxY));
        for (int i = 0; i <= maxAbs; i++) {
            if (ly > minY && isValidFloor(y, x, --ly, z))
                return ly;
            if (gy < maxY && isValidFloor(y, x, ++gy, z))
                return gy;
        }

        // if no floor is found return the default value
        return def;
    }

    /**
     * Returns if the coordinates are considered a valid floor by the {@link #findFloor(int, int, int, int, int, int) findFloor} method<br>
     * Override this method to define your own floor detection
     * @param originalY the original Y coordinate of the search
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return true if the coordinates are considered a valid floor, false otherwise
     * @see #isValidFloor(Block)
     * @see #findFloor(int, int, int, int, int, int)
     */
    public boolean isValidFloor(int originalY, int x, int y, int z) {
        return isValidFloor(world.getBlockAt(x, y, z));
    }

    /**
     * Returns if the block is considered a valid floor by the {@link #findFloor(int, int, int, int, int, int) findFloor} method<br>
     * @param block the block to check
     * @return true if the block is considered a valid floor, false otherwise
     * @see #isValidFloor(int, int, int, int)
     * @see #findFloor(int, int, int, int, int, int)
     */
    public boolean isValidFloor(Block block) {
        return true;
    }

    /**
     * Helper method for quickly visualizing an area.
     *
     * @param player the {@link Player} visualizing the area
     * @param boundingBox the {@link BoundingBox} being visualized
     * @param type the {@link VisualizationType type of visualization}
     */
    public static void visualizeArea(
            @NotNull Player player,
            @NotNull BoundingBox boundingBox,
            @NotNull VisualizationType type) {
        BoundaryVisualizationEvent event = new BoundaryVisualizationEvent(player,
                Set.of(new Boundary(boundingBox, type)),
                player.getLocation().getBlockY() - 1);
        callAndVisualize(event);
    }

    /**
     * Helper method for quickly visualizing a claim and all its children.
     *
     * @param player the {@link Player} visualizing the area
     * @param claim the {@link Claim} being visualized
     * @param type the {@link VisualizationType type of visualization}
     */
    public static void visualizeClaim(
            @NotNull Player player,
            @NotNull Claim claim,
            @NotNull VisualizationType type)
    {
        visualizeClaim(player, claim, type, player.getLocation().getBlockY() - 1);
    }

    /**
     * Helper method for quickly visualizing a claim and all its children.
     *
     * @param player the {@link Player} visualizing the area
     * @param claim the {@link Claim} being visualized
     * @param type the {@link VisualizationType type of visualization}
     * @param block the {@link Block} on which the visualization was initiated
     */
    public static void visualizeClaim(
            @NotNull Player player,
            @NotNull Claim claim,
            @NotNull VisualizationType type,
            @NotNull Block block)
    {
        visualizeClaim(player, claim, type, player.getLocation().getBlockY() - 1);
    }

    /**
     * Helper method for quickly visualizing a claim and all its children.
     *
     * @param player the {@link Player} visualizing the area
     * @param claim the {@link Claim} being visualized
     * @param type the {@link VisualizationType}
     * @param height the height at which the visualization was initiated
     */
    private static void visualizeClaim(
            @NotNull Player player,
            @NotNull Claim claim,
            @NotNull VisualizationType type,
            int height)
    {
        BoundaryVisualizationEvent event = new BoundaryVisualizationEvent(player, defineBoundaries(claim, type), height);
        callAndVisualize(event);
    }

    /**
     * Define {@link Boundary Boundaries} for a claim and its children.
     *
     * @param claim the {@link Claim}
     * @param type the {@link VisualizationType}
     * @return the resulting {@code Boundary} values
     */
    private static Collection<Boundary> defineBoundaries(Claim claim, VisualizationType type)
    {
        if (claim == null) return Set.of();

        boolean subdivision = claim.parent != null;
        if (subdivision) {
            if (type == VisualizationType.CONFLICT_ZONE) {
                return List.of(new Boundary(claim.parent, claim.parent.isAdminClaim() ? VisualizationType.ADMIN_CLAIM : VisualizationType.CLAIM), new Boundary(claim, type));
            } else {
                // For single claims, always visualize parent and children.
                claim = claim.parent;
            }
        }

        // Correct visualization type for claim type for simplicity.
        if (type == VisualizationType.CLAIM && claim.isAdminClaim()) type = VisualizationType.ADMIN_CLAIM;

        // Gather all boundaries. It's important that children override parent so
        // that users can always find children, no matter how oddly sized or positioned.
        List<Boundary> boundaries = new ArrayList<>(1 + claim.children.size());
        for (Claim child : claim.children) {
            boundaries.add(new Boundary(child, VisualizationType.SUBDIVISION));
        }
        boundaries.add(new Boundary(claim, type));
        return boundaries;
    }

    /**
     * Helper method for quickly visualizing a collection of nearby claims.
     *
     * @param player the {@link Player} visualizing the area
     * @param claims the {@link Claim Claims} being visualized
     * @param height the height at which the visualization was initiated
     */
    public static void visualizeNearbyClaims(
            @NotNull Player player,
            @NotNull Collection<Claim> claims,
            int height)
    {
        BoundaryVisualizationEvent event = new BoundaryVisualizationEvent(
                player,
                claims.stream().map(claim -> new Boundary(
                        claim,
                        claim.isAdminClaim() ? VisualizationType.ADMIN_CLAIM :  VisualizationType.CLAIM))
                        .collect(Collectors.toSet()),
                height);
        callAndVisualize(event);
    }

    /**
     * Call a {@link BoundaryVisualizationEvent} and use the resulting values to create and apply a visualization.
     *
     * @param event the {@code BoundaryVisualizationEvent}
     */
    public static void callAndVisualize(@NotNull BoundaryVisualizationEvent event) {
        Bukkit.getPluginManager().callEvent(event);

        Player player = event.getPlayer();
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        BoundaryVisualization currentVisualization = playerData.getVisibleBoundaries();

        Collection<Boundary> boundaries = event.getBoundaries();
        boundaries.removeIf(Objects::isNull);

        if (currentVisualization != null
                && currentVisualization.elements.equals(boundaries)
                && currentVisualization.visualizeFrom.distanceSquared(event.getCenter()) < 165)
        {
            // Ignore visualizations with duplicate boundaries if the viewer has moved fewer than 15 blocks.
            return;
        }

        BoundaryVisualization visualization = event.getProvider().create(player, event.getCenter(), event.getHeight());
        visualization.boundaries.addAll(boundaries);

        // If they have a visualization active, clear it first.
        playerData.setVisibleBoundaries(null);

        // If they are online and in the same world as the visualization, display the visualization next tick.
        if (visualization.canVisualize())
        {
            GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(
                    GriefPrevention.instance,
                    new DelayedVisualizationTask(visualization, event),
                    1L);
        }
    }

    private record DelayedVisualizationTask(
            @NotNull BoundaryVisualization visualization,
            @NotNull BoundaryVisualizationEvent event)
            implements Runnable
    {

        @Override
        public void run()
        {
            try
            {
                visualization.apply();
            }
            catch (Exception exception)
            {
                if (event.getProvider() == BoundaryVisualizationEvent.DEFAULT_PROVIDER)
                {
                    // If the provider is our own, log normally.
                    GriefPrevention.instance.getLogger().log(Level.WARNING, "Exception visualizing claim", exception);
                    return;
                }

                // Otherwise, add an extra hint that the problem is not with GP.
                GriefPrevention.AddLogEntry(
                        String.format(
                                "External visualization provider %s caused %s: %s",
                                event.getProvider().getClass().getName(),
                                exception.getClass().getName(),
                                exception.getCause()),
                        CustomLogEntryTypes.Exception);
                GriefPrevention.instance.getLogger().log(
                        Level.WARNING,
                        "Exception visualizing claim using external provider",
                        exception);

                // Fall through to default provider.
                BoundaryVisualization fallback = BoundaryVisualizationEvent.DEFAULT_PROVIDER
                        .create(event.getPlayer(), event.getCenter(), event.getHeight());
                event.getBoundaries().stream().filter(Objects::nonNull).forEach(fallback.boundaries::add);
                fallback.apply();
            }
        }

    }

}
