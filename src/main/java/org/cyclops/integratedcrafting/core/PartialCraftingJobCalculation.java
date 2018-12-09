package org.cyclops.integratedcrafting.core;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.UnknownCraftingRecipeException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author rubensworks
 */
public class PartialCraftingJobCalculation {

    @Nullable
    private final CraftingJob craftingJob;
    @Nullable
    private final List<UnknownCraftingRecipeException> missingDependencies;
    @Nullable
    private final Map<IngredientComponent<?, ?>, List<?>> ingredientsStorage;
    @Nullable
    private final List<CraftingJob> partialCraftingJobs;

    public PartialCraftingJobCalculation(@Nullable CraftingJob craftingJob,
                                         List<UnknownCraftingRecipeException> missingDependencies,
                                         Map<IngredientComponent<?, ?>, List<?>> ingredientsStorage,
                                         List<CraftingJob> partialCraftingJobs) {
        this.craftingJob = craftingJob;
        this.missingDependencies = missingDependencies;
        this.ingredientsStorage = ingredientsStorage;
        this.partialCraftingJobs = partialCraftingJobs;
    }

    @Nullable
    public CraftingJob getCraftingJob() {
        return craftingJob;
    }

    @Nullable
    public List<UnknownCraftingRecipeException> getMissingDependencies() {
        return missingDependencies;
    }

    @Nullable
    public Map<IngredientComponent<?, ?>, List<?>> getIngredientsStorage() {
        return ingredientsStorage;
    }

    @Nullable
    public List<CraftingJob> getPartialCraftingJobs() {
        return partialCraftingJobs;
    }
}
