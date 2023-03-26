package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfig;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCrafting;

/**
 * Config for {@link ContainerPartInterfaceCrafting}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingConfig extends GuiConfig<ContainerPartInterfaceCrafting> {

    public ContainerPartInterfaceCraftingConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCrafting::new, FeatureFlags.VANILLA_SET));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCrafting>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCrafting, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCrafting::new);
    }

}
