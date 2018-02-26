package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Maps;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.cyclops.commoncapabilities.api.capability.fluidhandler.FluidMatch;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Map;
import java.util.Set;

/**
 * A recipe component index for {@link IngredientComponent#FLUIDSTACK}.
 * @author rubensworks
 */
public class IngredientComponentRecipeIndexFluidStack implements IIngredientComponentRecipeIndex<FluidStack, Integer> {

    private final Map<Fluid, Set<PrioritizedRecipe>> prototypedFluidRecipes;
    private final Set<PrioritizedRecipe> plainFluidRecipes;

    public IngredientComponentRecipeIndexFluidStack() {
        this.prototypedFluidRecipes = Maps.newIdentityHashMap();
        this.plainFluidRecipes = PrioritizedRecipe.newOutputSortedSet();
    }

    @Override
    public void addRecipe(PrioritizedRecipe prioritizedRecipe) {
        for (FluidStack ingredient : prioritizedRecipe.getRecipe().getOutput().getInstances(IngredientComponent.FLUIDSTACK)) {
            Fluid fluid = ingredient.getFluid();
            Set<PrioritizedRecipe> recipes = this.prototypedFluidRecipes.computeIfAbsent(fluid, i -> PrioritizedRecipe.newOutputSortedSet());
            recipes.add(prioritizedRecipe);
        }
    }

    @Override
    public void removeRecipe(PrioritizedRecipe prioritizedRecipe) {
        for (FluidStack ingredient : prioritizedRecipe.getRecipe().getOutput().getInstances(IngredientComponent.FLUIDSTACK)) {
            Fluid fluid = ingredient.getFluid();
            Set<PrioritizedRecipe> recipes = this.prototypedFluidRecipes.get(fluid);
            if (recipes != null) {
                recipes.remove(prioritizedRecipe);
            }
        }
    }

    @Override
    public Set<PrioritizedRecipe> getRecipes(FluidStack output, Integer matchCondition, int limit) {
        Set<PrioritizedRecipe> recipes = PrioritizedRecipe.newOutputSortedSet();

        // Check the prototyped recipes
        for (PrioritizedRecipe prioritizedRecipe : this.prototypedFluidRecipes.get(output.getFluid())) {
            for (FluidStack ingredient : prioritizedRecipe.getRecipe().getOutput().getInstances(IngredientComponent.FLUIDSTACK)) {
                if (FluidMatch.areFluidStacksEqual(ingredient, output, matchCondition)) {
                    recipes.add(prioritizedRecipe);
                    if (recipes.size() >= limit) {
                        return recipes;
                    }
                    break;
                }
            }
        }

        return recipes;
    }
}
