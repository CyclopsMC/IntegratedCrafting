package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;

/**
 * An exception for when an infinitely recursive crafting recipe was found.
 * @author rubensworks
 */
public class RecursiveCraftingRecipeException extends Exception {

    private final IPrototypedIngredient prototypedIngredient;

    public <T, M> RecursiveCraftingRecipeException(IPrototypedIngredient prototypedIngredient) {
        super();
        this.prototypedIngredient = prototypedIngredient;
    }

    public IPrototypedIngredient getPrototypedIngredient() {
        return prototypedIngredient;
    }

    @Override
    public String getMessage() {
        return String.format("Infinite recursive recipe detected: %s", prototypedIngredient);
    }
}
