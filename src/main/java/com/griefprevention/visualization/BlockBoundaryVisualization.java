package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public abstract class BlockBoundaryVisualization extends BoundaryVisualization
{

    protected final int step;
    protected final BoundingBox displayZoneArea;
    protected final Collection<BlockElement> elements = new ArrayList<>();

    /**
     * Construct a new {@code BlockBoundaryVisualization} with a step size of {@code 10} and a display radius of
     * {@code 75}.
     *
     * @param player the {@link Player} to visualize for
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     */
    protected BlockBoundaryVisualization(@NotNull Player player, @NotNull IntVector visualizeFrom, int height)
    {
        this(player, visualizeFrom, height, 10, 75);
    }

    /**
     * Construct a new {@code BlockBoundaryVisualization}.
     *
     * @param player the {@link Player} to visualize for
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     * @param step the distance between individual side elements
     * @param displayZoneRadius the radius in which elements are visible from the visualization location
     */
    protected BlockBoundaryVisualization(
            @NotNull Player player,
            @NotNull IntVector visualizeFrom,
            int height,
            int step,
            int displayZoneRadius)
    {
        super(player, visualizeFrom, height);
        this.step = step;
        this.displayZoneArea = new BoundingBox(
                visualizeFrom.add(-displayZoneRadius, -displayZoneRadius, -displayZoneRadius),
                visualizeFrom.add(displayZoneRadius, displayZoneRadius, displayZoneRadius));
    }

    @Override
    protected void apply() {
        super.apply();
        elements.forEach(BlockElement::draw);
    }

    @Override
    protected void draw(@NotNull Boundary boundary)
    {
        BoundingBox area = boundary.bounds();

        // Trim to area - allows for simplified display containment check later.
        BoundingBox displayZone = displayZoneArea.intersection(area);

        // If area is not inside display zone, there is nothing to display.
        if (displayZone == null)
            return;

        boolean is3d = area.getMaxY() < Claim._2D_HEIGHT;
        Consumer<@NotNull IntVector> addCornerElem = addCornerElements(boundary);
        Consumer<@NotNull IntVector> addSideElem = addSideElements(boundary);
        Consumer<IntVector> corner = (pos) -> addDisplayed(displayZone, pos, addCornerElem);
        Consumer<IntVector> side = (pos) -> addDisplayed(displayZone, pos, addSideElem);

        // we render a cube for 3d boundaries, otherwise we render a square on the "floor" for 2d boundaries
        if (is3d) {
            // Add corners first to override any other elements created by very small claims.
            corner.accept(new IntVector(area.getMinX(), area.getMaxY(), area.getMaxZ()));
            corner.accept(new IntVector(area.getMaxX(), area.getMaxY(), area.getMaxZ()));
            corner.accept(new IntVector(area.getMinX(), area.getMaxY(), area.getMinZ()));
            corner.accept(new IntVector(area.getMaxX(), area.getMaxY(), area.getMinZ()));

            corner.accept(new IntVector(area.getMinX(), area.getMinY(), area.getMaxZ()));
            corner.accept(new IntVector(area.getMaxX(), area.getMinY(), area.getMaxZ()));
            corner.accept(new IntVector(area.getMinX(), area.getMinY(), area.getMinZ()));
            corner.accept(new IntVector(area.getMaxX(), area.getMinY(), area.getMinZ()));

            // North and south boundaries
            for (int x = Math.max(area.getMinX() + step, displayZone.getMinX()); x < area.getMaxX() - step / 2 && x < displayZone.getMaxX(); x += step) {
                side.accept(new IntVector(x, area.getMaxY(), area.getMaxZ()));
                side.accept(new IntVector(x, area.getMaxY(), area.getMinZ()));

                side.accept(new IntVector(x, area.getMinY(), area.getMaxZ()));
                side.accept(new IntVector(x, area.getMinY(), area.getMinZ()));
            }

            // East and west boundaries
            for (int z = Math.max(area.getMinZ() + step, displayZone.getMinZ()); z < area.getMaxZ() - step / 2 && z < displayZone.getMaxZ(); z += step) {
                side.accept(new IntVector(area.getMinX(), area.getMaxY(), z));
                side.accept(new IntVector(area.getMaxX(), area.getMaxY(), z));

                side.accept(new IntVector(area.getMinX(), area.getMinY(), z));
                side.accept(new IntVector(area.getMaxX(), area.getMinY(), z));
            }

            // First and last step are always directly adjacent to corners
            if (area.getLength() > 2) {
                side.accept(new IntVector(area.getMinX() + 1, area.getMaxY(), area.getMaxZ()));
                side.accept(new IntVector(area.getMinX() + 1, area.getMaxY(), area.getMinZ()));
                side.accept(new IntVector(area.getMaxX() - 1, area.getMaxY(), area.getMaxZ()));
                side.accept(new IntVector(area.getMaxX() - 1, area.getMaxY(), area.getMinZ()));

                side.accept(new IntVector(area.getMinX() + 1, area.getMinY(), area.getMaxZ()));
                side.accept(new IntVector(area.getMinX() + 1, area.getMinY(), area.getMinZ()));
                side.accept(new IntVector(area.getMaxX() - 1, area.getMinY(), area.getMaxZ()));
                side.accept(new IntVector(area.getMaxX() - 1, area.getMinY(), area.getMinZ()));
            }

            if (area.getWidth() > 2) {
                side.accept(new IntVector(area.getMinX(), area.getMaxY(), area.getMinZ() + 1));
                side.accept(new IntVector(area.getMaxX(), area.getMaxY(), area.getMinZ() + 1));
                side.accept(new IntVector(area.getMinX(), area.getMaxY(), area.getMaxZ() - 1));
                side.accept(new IntVector(area.getMaxX(), area.getMaxY(), area.getMaxZ() - 1));

                side.accept(new IntVector(area.getMinX(), area.getMinY(), area.getMinZ() + 1));
                side.accept(new IntVector(area.getMaxX(), area.getMinY(), area.getMinZ() + 1));
                side.accept(new IntVector(area.getMinX(), area.getMinY(), area.getMaxZ() - 1));
                side.accept(new IntVector(area.getMaxX(), area.getMinY(), area.getMaxZ() - 1));
            }

            // extra logic for the vertical direction
            for (int y = Math.max(area.getMinY() + step, displayZone.getMinY()); y < area.getMaxY() - step / 2 && y < displayZone.getMaxY(); y += step) {
                side.accept(new IntVector(area.getMinX(), y, area.getMaxZ()));
                side.accept(new IntVector(area.getMaxX(), y, area.getMinZ()));

                side.accept(new IntVector(area.getMinX(), y, area.getMinZ()));
                side.accept(new IntVector(area.getMaxX(), y, area.getMaxZ()));
            }
            if (area.getHeight() > 2) {
                side.accept(new IntVector(area.getMinX(), area.getMaxY() - 1, area.getMinZ()));
                side.accept(new IntVector(area.getMaxX(), area.getMaxY() - 1, area.getMinZ()));
                side.accept(new IntVector(area.getMinX(), area.getMaxY() - 1, area.getMaxZ()));
                side.accept(new IntVector(area.getMaxX(), area.getMaxY() - 1, area.getMaxZ()));

                side.accept(new IntVector(area.getMinX(), area.getMinY() + 1, area.getMinZ()));
                side.accept(new IntVector(area.getMaxX(), area.getMinY() + 1, area.getMinZ()));
                side.accept(new IntVector(area.getMinX(), area.getMinY() + 1, area.getMaxZ()));
                side.accept(new IntVector(area.getMaxX(), area.getMinY() + 1, area.getMaxZ()));
            }
        } else { // if this boundary is 2d
            // resolve height for each corner so each corner is a consistent height
            int minMax = findFloor(area.getMinX(), height, area.getMaxZ());
            int maxMin = findFloor(area.getMaxX(), height, area.getMinZ());
            int maxMax = findFloor(area.getMaxX(), height, area.getMaxZ());
            int minMin = findFloor(area.getMinX(), height, area.getMinZ());

            // Add corners first to override any other elements created by very small boundaries.
            corner.accept(new IntVector(area.getMinX(), minMax, area.getMaxZ()));
            corner.accept(new IntVector(area.getMaxX(), maxMax, area.getMaxZ()));
            corner.accept(new IntVector(area.getMinX(), minMin, area.getMinZ()));
            corner.accept(new IntVector(area.getMaxX(), maxMin, area.getMinZ()));

            // North and south boundaries
            for (int x = Math.max(area.getMinX() + step, displayZone.getMinX()); x < area.getMaxX() - step / 2 && x < displayZone.getMaxX(); x += step) {
                side.accept(new IntVector(x, findFloor(x, height, area.getMaxZ()), area.getMaxZ()));
                side.accept(new IntVector(x, findFloor(x, height, area.getMinZ()), area.getMinZ()));
            }

            // East and west boundaries
            for (int z = Math.max(area.getMinZ() + step, displayZone.getMinZ()); z < area.getMaxZ() - step / 2 && z < displayZone.getMaxZ(); z += step) {
                side.accept(new IntVector(area.getMinX(), findFloor(area.getMinX(), height, z), z));
                side.accept(new IntVector(area.getMaxX(), findFloor(area.getMaxX(), height, z), z));
            }

            // First and last step are always directly adjacent to corners
            if (area.getLength() > 2) {
                side.accept(new IntVector(area.getMinX() + 1, minMax, area.getMaxZ()));
                side.accept(new IntVector(area.getMinX() + 1, minMin, area.getMinZ()));
                side.accept(new IntVector(area.getMaxX() - 1, maxMax, area.getMaxZ()));
                side.accept(new IntVector(area.getMaxX() - 1, maxMin, area.getMinZ()));
            }

            if (area.getWidth() > 2) {
                side.accept(new IntVector(area.getMinX(), minMin, area.getMinZ() + 1));
                side.accept(new IntVector(area.getMaxX(), maxMin, area.getMinZ() + 1));
                side.accept(new IntVector(area.getMinX(), minMax, area.getMaxZ() - 1));
                side.accept(new IntVector(area.getMaxX(), maxMax, area.getMaxZ() - 1));
            }
        }
    }

    /**
     * Create a {@link Consumer} that adds a corner element for the given {@link IntVector}.
     *
     * @param boundary the {@code Boundary}
     * @return the corner element consumer
     */
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary) {
        return intVector -> {};
    }

    /**
     * Create a {@link Consumer} that adds a side element for the given {@link IntVector}.
     *
     * @param boundary the {@code Boundary}
     * @return the side element consumer
     */
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary) {
        return intVector -> {};
    }

    protected boolean isAccessible(@NotNull BoundingBox displayZone, @NotNull IntVector coordinate)
    {
        return displayZone.contains2d(coordinate) && coordinate.isChunkLoaded(world);
    }

    /**
     * Add a display element if accessible.
     *
     * @param displayZone the zone in which elements may be displayed
     * @param coordinate the coordinate being displayed
     * @param addElement the function for obtaining the element displayed
     */
    protected void addDisplayed(
            @NotNull BoundingBox displayZone,
            @NotNull IntVector coordinate,
            @NotNull Consumer<@NotNull IntVector> addElement)
    {
        if (isAccessible(displayZone, coordinate)) {
            addElement.accept(coordinate);
        }
    }

    @Override
    public void revert()
    {
        // If the player cannot visualize the blocks, they should already be effectively reverted.
        if (!canVisualize())
        {
            return;
        }

        // Elements do not track the boundary they're attached to - all elements are reverted individually instead.
        this.elements.forEach(BlockElement::erase);
    }

    @Override
    protected void erase(@NotNull Boundary boundary)
    {
        this.elements.forEach(BlockElement::erase);
    }

}
