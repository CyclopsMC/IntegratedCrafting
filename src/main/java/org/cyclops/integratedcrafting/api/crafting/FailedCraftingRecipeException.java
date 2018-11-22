package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.List;

/**
 * An exception for when a crafting recipe could not be crafted due to failing sub-dependencies.
 *
 * The missing child recipes will only be non-null iff the collectMissingRecipes flag
 * was enabled when calculating crafting jobs.
 *
 * @author rubensworks
 */
public class FailedCraftingRecipeException extends Exception {

    private final PrioritizedRecipe recipe;
    private final long quantityMissing;
    private final List<UnknownCraftingRecipeException> missingChildRecipes;

    public FailedCraftingRecipeException(PrioritizedRecipe recipe, long quantityMissing,
                                         List<UnknownCraftingRecipeException> missingChildRecipes) {
        super();
        this.recipe = recipe;
        this.quantityMissing = quantityMissing;
        this.missingChildRecipes = missingChildRecipes;
    }

    public PrioritizedRecipe getRecipe() {
        return recipe;
    }

    public long getQuantityMissing() {
        return quantityMissing;
    }

    public List<UnknownCraftingRecipeException> getMissingChildRecipes() {
        return missingChildRecipes;
    }

    @Override
    public String getMessage() {
        return String.format("Could craft the recipe %s (with %s missing), with missing sub-recipes: %s",
                getRecipe().getRecipe(), getQuantityMissing(), getMissingChildRecipes());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FailedCraftingRecipeException)) {
            return false;
        }
        FailedCraftingRecipeException that = (FailedCraftingRecipeException) obj;
        return this.getRecipe().equals(that.getRecipe())
                && this.getQuantityMissing() == that.getQuantityMissing()
                && this.getMissingChildRecipes().equals(that.getMissingChildRecipes());
    }
}
