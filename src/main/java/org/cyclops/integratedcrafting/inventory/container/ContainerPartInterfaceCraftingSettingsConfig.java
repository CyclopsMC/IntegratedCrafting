package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.world.flag.FeatureFlags;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigCommon;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;

/**
 * Config for {@link ContainerPartInterfaceCraftingSettings}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingSettingsConfig extends GuiConfigCommon<ContainerPartInterfaceCraftingSettings, IModBase> {

    public ContainerPartInterfaceCraftingSettingsConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting_settings",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCraftingSettings::new, FeatureFlags.VANILLA_SET));
    }

    @Override
    public GuiConfigScreenFactoryProvider<ContainerPartInterfaceCraftingSettings> getScreenFactoryProvider() {
        return new ContainerPartInterfaceCraftingSettingsConfigScreenFactoryProvider();
    }
}
