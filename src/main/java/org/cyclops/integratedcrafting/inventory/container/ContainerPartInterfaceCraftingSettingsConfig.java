package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfig;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCraftingSettings;

/**
 * Config for {@link ContainerPartInterfaceCraftingSettings}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingSettingsConfig extends GuiConfig<ContainerPartInterfaceCraftingSettings> {

    public ContainerPartInterfaceCraftingSettingsConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting_settings",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCraftingSettings::new));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCraftingSettings>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCraftingSettings, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCraftingSettings::new);
    }

}
