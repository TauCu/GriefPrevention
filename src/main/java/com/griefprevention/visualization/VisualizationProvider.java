package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A provider for {@link BoundaryVisualization BoundaryVisualizations}.
 */
public interface VisualizationProvider
{

    /**
     * Construct a new {@link BoundaryVisualization} with the given parameters.
     *
     * @param player the {@link Player} to visualize for
     * @param visualizeFrom the {@link IntVector} representing the world coordinate being visualized from
     * @param height the height of the visualization
     * @return the resulting visualization
     */
    @Contract(pure = true, value = "_, _, _ -> new")
    @NotNull BoundaryVisualization create(@NotNull Player player, @NotNull IntVector visualizeFrom, int height);

}
