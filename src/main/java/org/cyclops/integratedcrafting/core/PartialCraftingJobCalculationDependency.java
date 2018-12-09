package org.cyclops.integratedcrafting.core;

import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.UnknownCraftingRecipeException;

import java.util.Collection;
import java.util.List;

/**
 * @author rubensworks
 */
public class PartialCraftingJobCalculationDependency {

    private final List<UnknownCraftingRecipeException> unknownCrafingRecipes;
    private final Collection<CraftingJob> partialCraftingJobs;

    public PartialCraftingJobCalculationDependency(List<UnknownCraftingRecipeException> unknownCrafingRecipes,
                                                   Collection<CraftingJob> partialCraftingJobs) {
        this.unknownCrafingRecipes = unknownCrafingRecipes;
        this.partialCraftingJobs = partialCraftingJobs;
    }

    public List<UnknownCraftingRecipeException> getUnknownCrafingRecipes() {
        return unknownCrafingRecipes;
    }

    public Collection<CraftingJob> getPartialCraftingJobs() {
        return partialCraftingJobs;
    }

    public boolean isValid() {
        return getUnknownCrafingRecipes().isEmpty();
    }
}
