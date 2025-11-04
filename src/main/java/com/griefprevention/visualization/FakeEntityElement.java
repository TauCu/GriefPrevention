package com.griefprevention.visualization;

import com.griefprevention.util.IntVector;
import me.ryanhamshire.GriefPrevention.util.ProtocolUtil;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class FakeEntityElement extends BlockElement {

    protected int entityId = -1;
    protected UUID entityUid = null;
    protected boolean drawn = false;

    public FakeEntityElement(Player player, IntVector coordinate) {
        super(player, coordinate);
    }

    @Override
    public void draw() {
        if (drawn())
            return;
        this.drawn = true;
        onDraw();
    }

    protected abstract void onDraw();

    @Override
    public void erase() {
        if (drawn()) {
            eraseAllEntities(player, List.of(this));
            entityId = -1;
            entityUid = null;
            drawn = false;
        }
    }

    protected void onErase() {}

    public boolean drawn() {
        return drawn;
    }

    public int entityId() {
        return entityId;
    }

    public UUID entityUID() {
        return entityUid;
    }

    public static void eraseAllEntities(Player whom, Collection<? extends FakeEntityElement> elements) {
        if (!elements.isEmpty()) {
            List<Integer> ids = elements.stream().map(FakeEntityElement::entityId).toList();
            for (FakeEntityElement element : elements) {
                element.onErase();
            }

            ProtocolUtil.destroyEntitiesFor(whom, ids);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 213299393;
    }

}
