package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.world.flag.FeatureFlags;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigCommon;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;

/**
 * Config for {@link ContainerPartInterfaceCraftingAttunedRecipes}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedRecipesConfig extends GuiConfigCommon<ContainerPartInterfaceCraftingAttunedRecipes, IModBase> {

    public ContainerPartInterfaceCraftingAttunedRecipesConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting_attuned_recipes",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCraftingAttunedRecipes::new, FeatureFlags.VANILLA_SET));
    }

    @Override
    public GuiConfigScreenFactoryProvider<ContainerPartInterfaceCraftingAttunedRecipes> getScreenFactoryProvider() {
        return new ContainerPartInterfaceCraftingAttunedRecipesConfigScreenFactoryProvider();
    }
}
