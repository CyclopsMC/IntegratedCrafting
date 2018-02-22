package org.cyclops.integratedcrafting.core;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentIndex;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Collections;
import java.util.Set;

/**
 * A recipe component index for {@link IngredientComponent#ENERGY}.
 * @author rubensworks
 */
public class IngredientComponentIndexEnergy implements IIngredientComponentIndex<Integer, Void> {

    private final Set<PrioritizedRecipe> recipes;

    public IngredientComponentIndexEnergy() {
        this.recipes = PrioritizedRecipe.newOutputSortedSet();
    }

    @Override
    public void addRecipe(PrioritizedRecipe prioritizedRecipe) {
        if (prioritizedRecipe.getRecipe().getOutput().getComponents().contains(IngredientComponent.ENERGY)) {
            this.recipes.add(prioritizedRecipe);
        }
    }

    @Override
    public void removeRecipe(PrioritizedRecipe prioritizedRecipe) {
        if (prioritizedRecipe.getRecipe().getOutput().getComponents().contains(IngredientComponent.ENERGY)) {
            this.recipes.remove(prioritizedRecipe);
        }
    }

    @Override
    public Set<PrioritizedRecipe> getRecipes(Integer output, Void matchCondition, int limit) {
        return Collections.unmodifiableSet(recipes);
    }
}
