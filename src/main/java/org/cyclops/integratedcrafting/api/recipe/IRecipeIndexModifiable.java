package org.cyclops.integratedcrafting.api.recipe;

/**
 * A modifiable recipe index.
 * @author rubensworks
 */
public interface IRecipeIndexModifiable extends IRecipeIndex {

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

}
