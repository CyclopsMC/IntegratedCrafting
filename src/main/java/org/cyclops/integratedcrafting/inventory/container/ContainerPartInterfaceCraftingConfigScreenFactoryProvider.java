package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCrafting;

/**
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingConfigScreenFactoryProvider extends GuiConfigScreenFactoryProvider<ContainerPartInterfaceCrafting> {
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCrafting>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCrafting, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCrafting::new);
    }
}
