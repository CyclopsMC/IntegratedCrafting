package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCraftingAttunedRecipes;

/**
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedRecipesConfigScreenFactoryProvider extends GuiConfigScreenFactoryProvider<ContainerPartInterfaceCraftingAttunedRecipes> {
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCraftingAttunedRecipes>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCraftingAttunedRecipes, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCraftingAttunedRecipes::new);
    }
}
