package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;

/**
 * An exception for when a crafting recipe for the given instance is unavailable.
 * @author rubensworks
 */
public class UnknownCraftingRecipeException extends Exception {

    private final Object instance;
    private final Object matchCondition;

    public <T, M> UnknownCraftingRecipeException(IngredientComponent<T, M> ingredientComponent, T instance, M matchCondition) {
        super();
        this.instance = instance;
        this.matchCondition = matchCondition;
    }

    public Object getInstance() {
        return instance;
    }

    public Object getMatchCondition() {
        return matchCondition;
    }

    @Override
    public String getMessage() {
        return String.format("Could not find a recipe for %s under match condition %s", instance, matchCondition);
    }
}
