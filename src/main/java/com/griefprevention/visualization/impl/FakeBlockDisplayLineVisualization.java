package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.Boundary;
import com.griefprevention.visualization.BoundaryVisualization;
import com.griefprevention.visualization.FakeEntityElement;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import me.ryanhamshire.GriefPrevention.util.LineCuller;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This visualization uses fake entities that glow (can be seen through other blocks)<br>
 * It requires ProtocolLib to work.
 * @author <a href="https://github.com/TauCu">TauCubed</a>
 */
public class FakeBlockDisplayLineVisualization extends BoundaryVisualization {

    protected Map<Boundary, Collection<FakeBlockDisplayElement>> bound2Elements = new HashMap<>(4);
    protected int step2d, step3d;
    protected BoundingBox displayZone;

    /**
     * Construct a new {@code FakeFallingBlockVisualization}.
     *
     * @param player         the {@link Player} to visualize for
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height        the height of the visualization
     */
    public FakeBlockDisplayLineVisualization(@NotNull Player player, @NotNull IntVector visualizeFrom, int height) {
        this(player, visualizeFrom, height, player.getWorld().getViewDistance() * 16, 16, 16);
    }

    public FakeBlockDisplayLineVisualization(Player player, IntVector visualizeFrom, int height, int displayZoneRadius, int step2d, int step3d) {
        super(player, visualizeFrom, height);
        this.step2d = step2d;
        this.step3d = step3d;
        this.displayZone = new BoundingBox(
                visualizeFrom.add(-displayZoneRadius, -displayZoneRadius, -displayZoneRadius),
                visualizeFrom.add(displayZoneRadius, displayZoneRadius, displayZoneRadius));
    }

    @Override
    protected void draw(Boundary boundary) {
        // cull some duplicate elements with HashMap
        var map = new HashMap<FakeBlockDisplayElement, LineCuller.Entry>(32);
        Consumer<FakeBlockDisplayElement> collector = e -> map.put(e, LineCuller.Entry.of(e.getCoordinate(), e.getToCoordinate()));

        if (boundary.bounds().getMaxY() < Claim._2D_HEIGHT) {
            draw3d(player, boundary, collector);
        } else {
            draw2d(player, boundary, collector);
        }

        // cull remaining with line culler
        map.entrySet().removeIf(e -> LineCuller.shouldCull(e.getValue(), map.values()));
        bound2Elements.put(boundary, map.keySet());
    }

    @Override
    protected void apply() {
        super.apply();
        for (var value : bound2Elements.values()) {
            for (var element : value)
                element.draw();
        }
    }

    public void draw2d(Player player, Boundary boundary, Consumer<FakeBlockDisplayElement> collector) {
        var bounds = boundary.bounds();
        int step = step2d;

        int minX = bounds.getMinX();
        int minZ = bounds.getMinZ();

        int maxX = bounds.getMaxX();
        int maxZ = bounds.getMaxZ();

        var rayResult = world.rayTraceBlocks(player.getLocation(), new Vector(0, -1, 0), 32, player.isInWater() ? FluidCollisionMode.NEVER : FluidCollisionMode.SOURCE_ONLY, true);
        int y = (rayResult != null && rayResult.getHitBlock() != null) ? rayResult.getHitBlock().getY() + 1 : height;

        var gen = elementGeneratorFor(boundary, player, collector);
        BiConsumer<IntVector, IntVector> con = (from, to) -> drawSteppedLine(gen, step, from, to);

        // west -> east
        con.accept(new IntVector(minX, y, minZ), new IntVector(maxX, y, minZ));

        // north -> south
        con.accept(new IntVector(maxX, y, minZ), new IntVector(maxX, y, maxZ));

        // east -> west
        con.accept(new IntVector(maxX, y, maxZ), new IntVector(minX, y, maxZ));

        // south -> north
        con.accept(new IntVector(minX, y, maxZ), new IntVector(minX, y, minZ));
    }

    public void draw3d(Player player, Boundary boundary, Consumer<FakeBlockDisplayElement> collector) {
        var bounds = boundary.bounds();
        int step = step3d;

        int minX = bounds.getMinX();
        int minY = bounds.getMinY();
        int minZ = bounds.getMinZ();

        int maxX = bounds.getMaxX();
        int maxY = bounds.getMaxY();
        int maxZ = bounds.getMaxZ();

        var gen = elementGeneratorFor(boundary, player, collector);
        BiConsumer<IntVector, IntVector> con = (from, to) -> drawSteppedLine(gen, step, from, to);

        // bottom rectangle (y = minY)
        con.accept(new IntVector(minX, minY, minZ), new IntVector(maxX, minY, minZ)); // north edge
        con.accept(new IntVector(maxX, minY, minZ), new IntVector(maxX, minY, maxZ)); // east edge
        con.accept(new IntVector(maxX, minY, maxZ), new IntVector(minX, minY, maxZ)); // south edge
        con.accept(new IntVector(minX, minY, maxZ), new IntVector(minX, minY, minZ)); // west edge

        // top rectangle (y = maxY)
        con.accept(new IntVector(minX, maxY, minZ), new IntVector(maxX, maxY, minZ)); // north edge
        con.accept(new IntVector(maxX, maxY, minZ), new IntVector(maxX, maxY, maxZ)); // east edge
        con.accept(new IntVector(maxX, maxY, maxZ), new IntVector(minX, maxY, maxZ)); // south edge
        con.accept(new IntVector(minX, maxY, maxZ), new IntVector(minX, maxY, minZ)); // west edge

        // vertical edges (connect top & bottom)
        con.accept(new IntVector(minX, minY, minZ), new IntVector(minX, maxY, minZ)); // NW corner
        con.accept(new IntVector(maxX, minY, minZ), new IntVector(maxX, maxY, minZ)); // NE corner
        con.accept(new IntVector(maxX, minY, maxZ), new IntVector(maxX, maxY, maxZ)); // SE corner
        con.accept(new IntVector(minX, minY, maxZ), new IntVector(minX, maxY, maxZ)); // SW corner
    }

    public void drawSteppedLine(BiConsumer<IntVector, IntVector> con, int step, IntVector from, IntVector to) {
        var aabb = new BoundingBox(from, to);
        if (!displayZone.intersects(aabb))
            return;

        if (!displayZone.contains(aabb)) {
            var clamped = displayZone.intersection(aabb);
            from = clamped.getMinInt();
            to = clamped.getMaxInt();
        }

        if (from.equals(to)) {
            con.accept(from, to);
            return;
        }

        Vector start = from.toVector();
        Vector end = to.toVector();
        double dist = start.distance(end);
        Vector dir = end.clone().subtract(start).normalize().multiply(step);

        Vector current = start.clone();
        IntVector prev = new IntVector(current);

        double len = 0;
        while (true) {
            len += step;
            if (len > dist)
                break;

            current.add(dir);

            IntVector next = new IntVector(current);
            if (next.equals(prev))
                continue;
            if (!isWithin(next))
                continue;

            con.accept(prev, next);
            prev = next;
        }

        if (!prev.equals(to))
            con.accept(prev, to);
    }

    public boolean isWithin(IntVector vec)  {
        return displayZone.contains(vec);
    }

    public boolean isWithin(int x, int y, int z)  {
        return displayZone.contains(x, y, z);
    }

    public BiConsumer<IntVector, IntVector> elementGeneratorFor(Boundary boundary, Player player, Consumer<FakeBlockDisplayElement> collector) {
        BlockData data;
        Color color;
        float scale = 0.2F;
        switch (boundary.type()) {
            case ADMIN_CLAIM -> {
                data = Material.ORANGE_STAINED_GLASS.createBlockData();
                color = ChatColor.GOLD.asBungee().getColor();
            }
            case SUBDIVISION -> {
                data = Material.WHITE_STAINED_GLASS.createBlockData();
                color = ChatColor.WHITE.asBungee().getColor();
            }
            case INITIALIZE_ZONE -> {
                data = Material.LIGHT_BLUE_STAINED_GLASS.createBlockData();
                color = ChatColor.AQUA.asBungee().getColor();
                scale = 0.5F;
            }
            case CONFLICT_ZONE -> {
                data = Material.RED_STAINED_GLASS.createBlockData();
                color = ChatColor.RED.asBungee().getColor();
            }
            default -> {
                if (boundary.claim() != null) {
                    if (boundary.claim().isBanned(player.getUniqueId())) {
                        data = Material.RED_STAINED_GLASS.createBlockData();
                        color = ChatColor.RED.asBungee().getColor();
                        break;
                    } else if (boundary.claim().hasAnyExplicitPermission(player.getUniqueId())) {
                        data = Material.YELLOW_STAINED_GLASS.createBlockData();
                        color = ChatColor.YELLOW.asBungee().getColor();
                        break;
                    }
                }
                data = Material.ORANGE_STAINED_GLASS.createBlockData();
                color = new Color(0xFFBB55);
            }
        }

        return elementGenerator(data, color, scale, collector);
    }

    public BiConsumer<IntVector, IntVector> elementGenerator(BlockData blockData, Color color, float scale, Consumer<FakeBlockDisplayElement> collector) {
        return (from, to) -> collector.accept(new FakeBlockDisplayElement(player, from, to, color, blockData, scale));
    }

    @Override
    protected void erase(Boundary boundary) {
        var elements = this.bound2Elements.remove(boundary);
        if (elements != null)
            FakeEntityElement.eraseAllEntities(player, elements);
    }

}
