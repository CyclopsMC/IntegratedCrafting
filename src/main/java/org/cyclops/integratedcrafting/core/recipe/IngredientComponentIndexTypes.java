package org.cyclops.integratedcrafting.core.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentIndex;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentIndexTypeRegistry;
import org.cyclops.integratedcrafting.core.IngredientComponentIndexEnergy;
import org.cyclops.integratedcrafting.core.IngredientComponentIndexFluidStack;
import org.cyclops.integratedcrafting.core.IngredientComponentIndexItemStack;

/**
 * @author rubensworks
 */
public class IngredientComponentIndexTypes {

    public static final IIngredientComponentIndexTypeRegistry REGISTRY = IntegratedCrafting._instance.getRegistryManager().getRegistry(IIngredientComponentIndexTypeRegistry.class);

    public static void load() {}

    public static final IIngredientComponentIndex.IFactory<ItemStack, Integer> FACTORY_ITEMSTACK = REGISTRY.register(
            IngredientComponent.ITEMSTACK, IngredientComponentIndexItemStack::new);
    public static final IIngredientComponentIndex.IFactory<FluidStack, Integer> FACTORY_FLUIDSTACK = REGISTRY.register(
            IngredientComponent.FLUIDSTACK, IngredientComponentIndexFluidStack::new);
    public static final IIngredientComponentIndex.IFactory<Integer, Void> FACTORY_ENERGY = REGISTRY.register(
            IngredientComponent.ENERGY, IngredientComponentIndexEnergy::new);
}
