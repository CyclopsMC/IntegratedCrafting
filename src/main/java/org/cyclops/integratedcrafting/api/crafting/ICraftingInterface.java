package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A handler for invoking crafting recipes.
 * @author rubensworks
 */
public interface ICraftingInterface {

    /**
     * @return The collection of recipes that is exposed by this crafting interface.
     */
    public Collection<PrioritizedRecipe> getRecipes();

    /**
     * Add the given crafting job to the list of crafting jobs.
     * @param craftingJob The crafting job.
     */
    public void scheduleCraftingJob(CraftingJob craftingJob);

    /**
     * @return Get all present crafting jobs.
     */
    public Iterator<CraftingJob> getCraftingJobs();

    /**
     * Get the pending outputs for the given crafting job.
     * @param craftingJobId A crafting job id.
     * @return A collection of all pending prototype-based ingredients.
     */
    public Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> getPendingCraftingJobOutputs(int craftingJobId);

    /**
     * Get the status for the given crafting job.
     * @param craftingJobId A crafting job id.
     * @return The crafting status.
     */
    public CraftingJobStatus getCraftingJobStatus(int craftingJobId);

}
