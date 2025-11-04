package com.griefprevention.visualization;

public enum VisualizationProviders {

    FAKE_BLOCK_DISPLAY("griefprevention:fake_block_display"),
    FAKE_BLOCK_DISPLAY_LINE("griefprevention:fake_block_display_line"),
    FAKE_SHULKER_BULLET("griefprevention:fake_shulker_bullet"),
    FAKE_BLOCK("griefprevention:fake_block"),
    FAKE_BLOCK_ANTI_CHEAT_COMPAT("griefprevention:fake_block_anti_cheat_compat");

    private final String key;

    VisualizationProviders(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
