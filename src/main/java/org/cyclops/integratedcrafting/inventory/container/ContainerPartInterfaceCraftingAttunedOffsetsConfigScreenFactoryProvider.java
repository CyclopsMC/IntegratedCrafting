package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCraftingAttunedOffsets;

/**
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedOffsetsConfigScreenFactoryProvider extends GuiConfigScreenFactoryProvider<ContainerPartInterfaceCraftingAttunedOffsets> {
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCraftingAttunedOffsets>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCraftingAttunedOffsets, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCraftingAttunedOffsets::new);
    }
}
