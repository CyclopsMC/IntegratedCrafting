package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;

/**
 * A sink for crafting results from a {@link ICraftingProcessOverride}.
 * @author rubensworks
 */
public interface ICraftingResultsSink {

    /**
     * Add the given instance.
     * @param ingredientComponent An ingredient component type.
     * @param instance An instance.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter, may be Void.
     */
    public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance);

}
