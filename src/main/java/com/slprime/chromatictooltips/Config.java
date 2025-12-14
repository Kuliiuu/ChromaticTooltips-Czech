package com.slprime.chromatictooltips;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean hotkeysEnricherEnabled = true;
    public static boolean hotkeysHelpTextEnabled = true;
    public static boolean stackSizeEnricherEnabled = true;
    public static boolean playerInventoryStackSizeEnabled = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        hotkeysEnricherEnabled = configuration.getBoolean(
            "enabled",
            "enricher.hotkeys",
            hotkeysEnricherEnabled,
            "Enable or disable the hotkeys tooltip enricher.");
        hotkeysHelpTextEnabled = configuration.getBoolean(
            "helpText",
            "enricher.hotkeys",
            hotkeysHelpTextEnabled,
            "Show help text for hotkeys in inventory tooltips.");

        stackSizeEnricherEnabled = configuration.getBoolean(
            "enabled",
            "enricher.stackSize",
            stackSizeEnricherEnabled,
            "Enable or disable the stack size tooltip enricher.");
        playerInventoryStackSizeEnabled = configuration.getBoolean(
            "playerInventoryStackSize",
            "enricher.stackSize",
            playerInventoryStackSizeEnabled,
            "Include the player's inventory when calculating stack sizes.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
