package org.cyclops.integratedcrafting.api.recipe;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.init.IRegistry;

import javax.annotation.Nullable;

/**
 * Registry for {@link IIngredientComponentRecipeIndex.IFactory}.
 * @author rubensworks
 */
public interface IIngredientComponentIndexTypeRegistry extends IRegistry {

    /**
     * Register a new recipe component index factory for the given recipe component type.
     * @param recipeComponent A recipe component type.
     * @param indexFactory A recipe component index factory.
     * @param <T> The recipe component instance type.
     * @return The registered factory.
     */
    public <T, M> IIngredientComponentRecipeIndex.IFactory<T, M> register(IngredientComponent<T, M> recipeComponent, IIngredientComponentRecipeIndex.IFactory<T, M> indexFactory);

    /**
     * Get the factory for the given recipe component type.
     * @param recipeComponent A recipe component type.
     * @param <T> The recipe component instance type.
     * @return The registered factory for the given type or null if none was registered before.
     */
    @Nullable
    public <T, M> IIngredientComponentRecipeIndex.IFactory<T, M> getFactory(IngredientComponent<T, M> recipeComponent);

}
