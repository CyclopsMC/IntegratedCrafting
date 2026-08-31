package org.cyclops.integratedcrafting;

import net.neoforged.fml.config.ModConfig;
import org.cyclops.cyclopscore.config.ConfigurableProperty;
import org.cyclops.cyclopscore.config.extendedconfig.DummyConfig;
import org.cyclops.cyclopscore.tracking.Analytics;
import org.cyclops.cyclopscore.tracking.Versions;

/**
 * A config with general options for this mod.
 * @author rubensworks
 *
 */
public class GeneralConfig extends DummyConfig {

    @ConfigurableProperty(category = "core", comment = "If an anonymous mod startup analytics request may be sent to our analytics service.")
    public static boolean analytics = true;

    @ConfigurableProperty(category = "core", comment = "If the version checker should be enabled.")
    public static boolean versionChecker = true;

    @ConfigurableProperty(category = "machine", comment = "The minimal update frequency in ticks to use for crafting interfaces.", minimalValue = 1, configLocation = ModConfig.Type.SERVER)
    public static int minCraftingInterfaceUpdateFreq = 5;

    @ConfigurableProperty(category = "machine", comment = "If the crafting interface should validate recipes on insertion.", isCommandable = true, configLocation = ModConfig.Type.SERVER)
    public static boolean validateRecipesCraftingInterface = true;

    @ConfigurableProperty(category = "machine", comment = "The maximum amount of crafting jobs that could be scheduled within one crafting interface without being started", minimalValue = 1, isCommandable = true, configLocation = ModConfig.Type.SERVER)
    public static int maxPendingCraftingJobs = 256;

    @ConfigurableProperty(category = "general", comment = "The base energy usage for the crafting writer.", minimalValue = 0, configLocation = ModConfig.Type.SERVER)
    public static int craftingWriterBaseConsumption = 1;
    @ConfigurableProperty(category = "general", comment = "The base energy usage for the crafting interface per crafting job being processed.", minimalValue = 0, configLocation = ModConfig.Type.SERVER)
    public static int interfaceCraftingBaseConsumption = 5;
    @ConfigurableProperty(category = "general", comment = "The base energy usage for the attuned crafting interface per crafting job being processed.", minimalValue = 0, configLocation = ModConfig.Type.SERVER)
    public static int interfaceCraftingAttunedBaseConsumption = 10;

    @ConfigurableProperty(category = "machine", comment = "The maximum number of recipes that a crafting interface remembers crafting durations for, which are used to estimate the duration of crafting jobs. Set to 0 to disable recipe-specific estimations.", minimalValue = 0, isCommandable = true, configLocation = ModConfig.Type.SERVER)
    public static int craftingInterfaceRecipeDurationEntries = 32;

    @ConfigurableProperty(category = "machine", comment = "The number of ticks after which a measured crafting duration is forgotten, so that estimations follow changes to the network. Set to 0 to never forget them.", minimalValue = 0, isCommandable = true, configLocation = ModConfig.Type.SERVER)
    public static int craftingInterfaceRecipeDurationMaxAge = 24000;

    @ConfigurableProperty(category = "machine", comment = "Enabling this option will log all recipe validation failures in crafting interfaces into the server logs", isCommandable = true, configLocation = ModConfig.Type.SERVER)
    public static boolean logRecipeValidationFailures = true;

    public GeneralConfig() {
        super(IntegratedCrafting._instance, "general");
    }

    @Override
    public void onRegistered() {
        if(analytics) {
            Analytics.registerMod(getMod(), Reference.GA_TRACKING_ID);
        }
        if(versionChecker) {
            Versions.registerMod(getMod(), IntegratedCrafting._instance, Reference.VERSION_URL);
        }
    }

}
