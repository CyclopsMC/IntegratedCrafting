package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;

import java.util.List;

/**
 * An exception for when a crafting recipe for the given instance is unavailable.
 *
 * The missing child recipes will only be non-null iff the collectMissingRecipes flag
 * was enabled when calculating crafting jobs.
 *
 * @author rubensworks
 */
public class UnknownCraftingRecipeException extends Exception {

    private final IPrototypedIngredient<?, ?> ingredient;
    private final long quantityMissing;
    private final List<UnknownCraftingRecipeException> missingChildRecipes;
    private final IMixedIngredients ingredientsStorage;
    private final List<CraftingJob> partialCraftingJobs;

    public UnknownCraftingRecipeException(IPrototypedIngredient<?, ?> ingredient, long quantityMissing,
                                          List<UnknownCraftingRecipeException> missingChildRecipes,
                                          IMixedIngredients ingredientsStorage, List<CraftingJob> partialCraftingJobs) {
        super();
        this.ingredient = ingredient;
        this.quantityMissing = quantityMissing;
        this.missingChildRecipes = missingChildRecipes;
        this.ingredientsStorage = ingredientsStorage;
        this.partialCraftingJobs = partialCraftingJobs;
    }

    public IPrototypedIngredient<?, ?> getIngredient() {
        return ingredient;
    }

    public long getQuantityMissing() {
        return quantityMissing;
    }

    public List<UnknownCraftingRecipeException> getMissingChildRecipes() {
        return missingChildRecipes;
    }

    public IMixedIngredients getIngredientsStorage() {
        return ingredientsStorage;
    }

    public List<CraftingJob> getPartialCraftingJobs() {
        return partialCraftingJobs;
    }

    @Override
    public String getMessage() {
        return String.format("Could not find a recipe for %s (with %s missing, and %s stored, and %s partial), with missing sub-recipes: %s", getIngredient(),
                getQuantityMissing(), getIngredientsStorage(), getPartialCraftingJobs(), getMissingChildRecipes());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnknownCraftingRecipeException)) {
            return false;
        }
        UnknownCraftingRecipeException that = (UnknownCraftingRecipeException) obj;
        return this.getIngredient().equals(that.getIngredient())
                && this.getQuantityMissing() == that.getQuantityMissing()
                && this.getMissingChildRecipes().equals(that.getMissingChildRecipes())
                && this.getIngredientsStorage().equals(that.getIngredientsStorage())
                && this.getPartialCraftingJobs().equals(that.getPartialCraftingJobs());
    }
}
