package org.cyclops.integratedcrafting.api.recipe;

import java.util.Set;

/**
 * An ingredient index for a certain recipe component.
 * @param <T> The instance type.
 * @param <M> The matching condition parameter, may be Void. Instances MUST properly implement the equals method.
 * @author rubensworks
 */
public interface IIngredientComponentIndex<T, M> {

    /**
     * Add the given recipe to this index.
     * @param prioritizedRecipe A recipe.
     */
    public void addRecipe(PrioritizedRecipe prioritizedRecipe);

    /**
     * Remove the given recipe from this index.
     * @param prioritizedRecipe A recipe.
     */
    public void removeRecipe(PrioritizedRecipe prioritizedRecipe);

    /**
     * Find recipes with the given output.
     * @param output An output ingredient instance.
     * @param matchCondition A condition under which the matching should be done.
     * @param limit An upper limit for the number of limits that should be returned.
     *              This is not strict, the implementor may choose to ignore this parameter.
     *              This is only a suggestive parameter and can be used by the implementor for optimizations.
     * @return The recipes that have the given output.
     */
    public Set<PrioritizedRecipe> getRecipes(T output, M matchCondition, int limit);

    /**
     * Factory for {@link IIngredientComponentIndex}.
     * @param <T> The instance type.
     */
    public static interface IFactory<T, M> {

        /**
         * @return A new recipe component index.
         */
        public IIngredientComponentIndex<T, M> newIndex();

    }

}
