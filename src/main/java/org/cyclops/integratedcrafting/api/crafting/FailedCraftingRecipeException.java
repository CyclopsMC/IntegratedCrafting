package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;

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

    private final IRecipeDefinition recipe;
    private final long quantityMissing;
    private final List<UnknownCraftingRecipeException> missingChildRecipes;
    private final IMixedIngredients ingredientsStorage;
    private final List<CraftingJob> partialCraftingJobs;

    public FailedCraftingRecipeException(IRecipeDefinition recipe, long quantityMissing,
                                         List<UnknownCraftingRecipeException> missingChildRecipes,
                                         IMixedIngredients ingredientsStorage, List<CraftingJob> partialCraftingJobs) {
        super();
        this.recipe = recipe;
        this.quantityMissing = quantityMissing;
        this.missingChildRecipes = missingChildRecipes;
        this.ingredientsStorage = ingredientsStorage;
        this.partialCraftingJobs = partialCraftingJobs;
    }

    public IRecipeDefinition getRecipe() {
        return recipe;
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
        return String.format("Could craft the recipe %s (with %s missing, and %s stored, and %s partial), with missing sub-recipes: %s",
                getRecipe(), getQuantityMissing(), getIngredientsStorage(), getPartialCraftingJobs(), getMissingChildRecipes());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FailedCraftingRecipeException)) {
            return false;
        }
        FailedCraftingRecipeException that = (FailedCraftingRecipeException) obj;
        return this.getRecipe().equals(that.getRecipe())
                && this.getQuantityMissing() == that.getQuantityMissing()
                && this.getMissingChildRecipes().equals(that.getMissingChildRecipes())
                && this.getIngredientsStorage().equals(that.getIngredientsStorage())
                && this.getPartialCraftingJobs().equals(that.getPartialCraftingJobs());
    }
}
