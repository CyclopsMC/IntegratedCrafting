package org.cyclops.integratedcrafting.core.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentIndexTypeRegistry;
import org.cyclops.integratedcrafting.core.IngredientComponentRecipeIndexEnergy;
import org.cyclops.integratedcrafting.core.IngredientComponentRecipeIndexFluidStack;
import org.cyclops.integratedcrafting.core.IngredientComponentRecipeIndexItemStack;

/**
 * @author rubensworks
 */
public class IngredientComponentIndexTypes {

    public static final IIngredientComponentIndexTypeRegistry REGISTRY = IntegratedCrafting._instance.getRegistryManager().getRegistry(IIngredientComponentIndexTypeRegistry.class);

    public static void load() {}

    public static final IIngredientComponentRecipeIndex.IFactory<ItemStack, Integer> FACTORY_ITEMSTACK = REGISTRY.register(
            IngredientComponent.ITEMSTACK, IngredientComponentRecipeIndexItemStack::new);
    public static final IIngredientComponentRecipeIndex.IFactory<FluidStack, Integer> FACTORY_FLUIDSTACK = REGISTRY.register(
            IngredientComponent.FLUIDSTACK, IngredientComponentRecipeIndexFluidStack::new);
    public static final IIngredientComponentRecipeIndex.IFactory<Integer, Void> FACTORY_ENERGY = REGISTRY.register(
            IngredientComponent.ENERGY, IngredientComponentRecipeIndexEnergy::new);
}
