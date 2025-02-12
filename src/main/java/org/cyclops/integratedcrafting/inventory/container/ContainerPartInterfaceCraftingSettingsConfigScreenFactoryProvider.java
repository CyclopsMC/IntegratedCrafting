package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCraftingSettings;

/**
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingSettingsConfigScreenFactoryProvider extends GuiConfigScreenFactoryProvider<ContainerPartInterfaceCraftingSettings> {
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCraftingSettings>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCraftingSettings, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCraftingSettings::new);
    }
}
