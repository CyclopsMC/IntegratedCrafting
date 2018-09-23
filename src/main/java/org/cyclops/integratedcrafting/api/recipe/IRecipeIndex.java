package org.cyclops.integratedcrafting.api.recipe;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;

import java.util.Iterator;
import java.util.Set;

/**
 * Indexes recipes by output.
 * @author rubensworks
 */
public interface IRecipeIndex {

    /**
     * @return All recipes that are available.
     */
    public Set<PrioritizedRecipe> getRecipes();

    /**
     * Find recipes with the given output.
     * @param outputType The recipe component type.
     * @param output An output ingredient instance.
     * @param matchCondition A condition under which the matching should be done.
     * @return An iterator of the recipes that have the given output.
     */
    public <T, M> Iterator<PrioritizedRecipe> getRecipes(IngredientComponent<T, M> outputType, T output, M matchCondition);

}
