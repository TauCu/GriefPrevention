package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author <a href="https://github.com/TauCu">TauCubed</a>
 */
public abstract class EntityBlockBoundaryVisualization<T extends FakeEntityElement> extends BlockBoundaryVisualization {

    protected HashMap<IntVector, T> entityElements = new HashMap<>(32);

    public EntityBlockBoundaryVisualization(@NotNull Player player, @NotNull IntVector visualizeFrom, int height) {
        this(player, visualizeFrom, height, 10, 128);
    }

    public EntityBlockBoundaryVisualization(Player player, IntVector visualizeFrom, int height, int step, int displayZoneRadius) {
        super(player, visualizeFrom, height, step, displayZoneRadius);
    }

    @Override
    protected void apply() {
        super.apply();
        // Apply all visualization elements.
        for (T element : entityElements.values())
            element.draw();
    }

    public T elementByEID(int entityId) {
        for (T element : entityElements.values()) {
            if (element.entityId() == entityId)
                return element;
        }
        return null;
    }

    public T elementByLocation(Location where) {
        if (getWorld() != where.getWorld())
            return null;
        return entityElements.get(new IntVector(where));
    }

    public World getWorld() {
        return this.world;
    }

    public Collection<T> getElements() {
        return Collections.unmodifiableCollection(entityElements.values());
    }

    @Override
    protected void erase(@NotNull Boundary boundary) {
        revert();
    }

}
