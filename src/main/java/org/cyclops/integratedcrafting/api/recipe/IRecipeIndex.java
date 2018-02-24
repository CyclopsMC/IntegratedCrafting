package org.cyclops.integratedcrafting.api.recipe;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;

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
     * @param limit An upper limit for the number of limits that should be returned.
     *              This is not strict, the implementor may choose to ignore this parameter.
     *              This is only a suggestive parameter and can be used by the implementor for optimizations.
     * @return The recipes that have the given output.
     */
    public <T, M> Set<PrioritizedRecipe> getRecipes(IngredientComponent<T, M> outputType, T output, M matchCondition, int limit);

}
