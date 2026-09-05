package org.cyclops.integratedcrafting;

import org.cyclops.cyclopscore.config.ConfigurablePropertyCommon;
import org.cyclops.cyclopscore.config.ModConfigLocation;
import org.cyclops.cyclopscore.config.extendedconfig.DummyConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

/**
 * A config with general options for this mod.
 * @author rubensworks
 *
 */
public class GeneralConfig extends DummyConfigCommon<IModBase> {

    @ConfigurablePropertyCommon(category = "machine", comment = "The minimal update frequency in ticks to use for crafting interfaces.", minimalValue = 1, configLocation = ModConfigLocation.SERVER)
    public static int minCraftingInterfaceUpdateFreq = 1;

    @ConfigurablePropertyCommon(category = "machine", comment = "If the crafting interface should validate recipes on insertion.", isCommandable = true, configLocation = ModConfigLocation.SERVER)
    public static boolean validateRecipesCraftingInterface = true;

    @ConfigurablePropertyCommon(category = "machine", comment = "The maximum amount of crafting jobs that could be scheduled within one crafting interface without being started", minimalValue = 1, isCommandable = true, configLocation = ModConfigLocation.SERVER)
    public static int maxPendingCraftingJobs = 256;

    @ConfigurablePropertyCommon(category = "general", comment = "The base energy usage for the crafting writer.", minimalValue = 0, configLocation = ModConfigLocation.SERVER)
    public static int craftingWriterBaseConsumption = 1;
    @ConfigurablePropertyCommon(category = "general", comment = "The base energy usage for the crafting interface per crafting job being processed.", minimalValue = 0, configLocation = ModConfigLocation.SERVER)
    public static int interfaceCraftingBaseConsumption = 5;
    @ConfigurablePropertyCommon(category = "general", comment = "The base energy usage for the attuned crafting interface per crafting job being processed.", minimalValue = 0, configLocation = ModConfigLocation.SERVER)
    public static int interfaceCraftingAttunedBaseConsumption = 10;

    @ConfigurablePropertyCommon(category = "machine", comment = "The maximum number of recipes that a crafting interface remembers crafting durations for, which are used to estimate the duration of crafting jobs. Set to 0 to disable recipe-specific estimations.", minimalValue = 0, isCommandable = true, configLocation = ModConfigLocation.SERVER)
    public static int craftingInterfaceRecipeDurationEntries = 32;

    @ConfigurablePropertyCommon(category = "machine", comment = "The number of ticks after which a measured crafting duration is forgotten, so that estimations follow changes to the network. Set to 0 to never forget them.", minimalValue = 0, isCommandable = true, configLocation = ModConfigLocation.SERVER)
    public static int craftingInterfaceRecipeDurationMaxAge = 144000;

    @ConfigurablePropertyCommon(category = "machine", comment = "Enabling this option will log all recipe validation failures in crafting interfaces into the server logs", isCommandable = true, configLocation = ModConfigLocation.SERVER)
    public static boolean logRecipeValidationFailures = true;

    public GeneralConfig() {
        super(IntegratedCrafting._instance, "general");
    }

}
