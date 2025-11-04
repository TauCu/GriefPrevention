package com.griefprevention.visualization.impl;

import com.griefprevention.util.IntVector;
import com.griefprevention.visualization.Boundary;
import com.griefprevention.visualization.EntityBlockBoundaryVisualization;
import me.ryanhamshire.GriefPrevention.util.ScoreboardColors;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FakeShulkerBulletVisualization extends EntityBlockBoundaryVisualization<FakeShulkerBulletElement> {

    public FakeShulkerBulletVisualization(@NotNull Player player, @NotNull IntVector visualizeFrom, int height) {
        super(player, visualizeFrom, height);
    }

    public FakeShulkerBulletVisualization(Player player, IntVector visualizeFrom, int height, int step, int displayZoneRadius) {
        super(player, visualizeFrom, height, step, displayZoneRadius);
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addCornerElements(@NotNull Boundary boundary) {
        return switch (boundary.type()) {
            case ADMIN_CLAIM ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.GOLD));
            case SUBDIVISION ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.WHITE));
            case INITIALIZE_ZONE ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.AQUA));
            case CONFLICT_ZONE ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.RED));
            default ->
                    addBulletElement(ScoreboardColors.getTeamFor(ChatColor.YELLOW));
        };
    }

    @Override
    protected @NotNull Consumer<@NotNull IntVector> addSideElements(@NotNull Boundary boundary) {
        return addCornerElements(boundary);
    }

    protected @NotNull Consumer<@NotNull IntVector> addBulletElement(@NotNull Team teamColor) {
        return vector -> {
            // don't draw over existing elements in the same position
            entityElements.putIfAbsent(vector, new FakeShulkerBulletElement(player, vector, teamColor));
        };
    }

    @Override
    public void revert() {
        FakeShulkerBulletElement.eraseAllBullets(player, entityElements.values());
    }

    @Override
    public boolean isValidFloor(int originalY, int x, int y, int z) {
        return FakeBlockDisplayVisualization.isFloor(world, originalY, x, y, z);
    }

    @Override
    public boolean isValidFloor(Block block) {
        throw new UnsupportedOperationException("not implemented. use isValidFloor(org.bukkit.World, int, int, int, int)");
    }

}
