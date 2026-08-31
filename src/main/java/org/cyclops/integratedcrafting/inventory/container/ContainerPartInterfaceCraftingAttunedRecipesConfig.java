package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfig;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCraftingAttunedRecipes;

/**
 * Config for {@link ContainerPartInterfaceCraftingAttunedRecipes}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedRecipesConfig extends GuiConfig<ContainerPartInterfaceCraftingAttunedRecipes> {

    public ContainerPartInterfaceCraftingAttunedRecipesConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting_attuned_recipes",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCraftingAttunedRecipes::new, FeatureFlags.VANILLA_SET));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCraftingAttunedRecipes>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCraftingAttunedRecipes, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCraftingAttunedRecipes::new);
    }

}
