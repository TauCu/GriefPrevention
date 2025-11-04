package me.ryanhamshire.GriefPrevention.registry;

import com.griefprevention.visualization.VisualizationProvider;
import com.griefprevention.visualization.VisualizationProviders;
import com.griefprevention.visualization.impl.*;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

public class GPRegistries {

    public static final GPRegistry<GPRegistry<?>> REGISTRIES = new GPRegistry<>("greifprevention:registries");

    public static final DefaultedGPRegistry<VisualizationProvider> VISUALIZATION_PROVIDERS = new DefaultedGPRegistry<>("griefprevention:visualization_providers", (player, visualizeFrom, height) -> {
        if (GriefPrevention.instance.support_protocollib_enabled) {
            return new FakeBlockDisplayVisualization(player, visualizeFrom, height);
        } else {
            return new FakeBlockVisualization(player, visualizeFrom, height);
        }
    });

    // boostrap
    static {
        // add registries
        REGISTRIES.register(VISUALIZATION_PROVIDERS.getName(), VISUALIZATION_PROVIDERS);

        // init registries
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_BLOCK_DISPLAY.getKey(), FakeBlockDisplayVisualization::new);
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_BLOCK_DISPLAY_LINE.getKey(), FakeBlockDisplayLineVisualization::new);
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_SHULKER_BULLET.getKey(), FakeShulkerBulletVisualization::new);
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_BLOCK.getKey(), FakeBlockVisualization::new);
        VISUALIZATION_PROVIDERS.register(VisualizationProviders.FAKE_BLOCK_ANTI_CHEAT_COMPAT.getKey(), AntiCheatCompatVisualization::new);
    }

}
