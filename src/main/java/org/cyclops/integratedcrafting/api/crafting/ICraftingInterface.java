package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integrateddynamics.api.part.PrioritizedPartPos;

import java.util.*;
import java.util.function.Function;

/**
 * A handler for invoking crafting recipes.
 * @author rubensworks
 */
public interface ICraftingInterface {

    /**
     * @return The collection of recipes that is exposed by this crafting interface.
     */
    public Collection<IRecipeDefinition> getRecipes();

    /**
     * @return If this crafting interface can currently accept crafting jobs.
     */
    public boolean canScheduleCraftingJobs();

    /**
     * Add the given crafting job to the list of crafting jobs.
     * @param craftingJob The crafting job.
     */
    public void scheduleCraftingJob(CraftingJob craftingJob);

    /**
     * Extract the required ingredients from storage and store them in the job.
     * @param craftingJob The crafting job.
     * @param storageGetter The storage getter.
     */
    public void fillCraftingJobBufferFromStorage(CraftingJob craftingJob, Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter);

    /**
     * @return Get the number of scheduled and running crafting jobs in this interface.
     */
    public int getCraftingJobsCount();

    /**
     * @return Get the scheduled and running crafting jobs in this interface.
     */
    public Iterator<CraftingJob> getCraftingJobs();

    /**
     * Get the pending outputs for the given crafting job,
     * where the list indicates the different entries running in parallel.
     * @param craftingJobId A crafting job id.
     * @return A collection of all pending prototype-based ingredients.
     */
    public List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> getPendingCraftingJobOutputs(int craftingJobId);

    /**
     * Get the status for the given crafting job.
     * @param network The network.
     * @param channel The channel.
     * @param craftingJobId A crafting job id.
     * @return The crafting status.
     */
    public CraftingJobStatus getCraftingJobStatus(ICraftingNetwork network, int channel, int craftingJobId);

    /**
     * Cancel the given crafting job.
     *
     * Note: this should not be called directly unless you know what you are doing!
     * Instead, you should call {@link ICraftingNetwork#cancelCraftingJob(int, int)}.
     *
     * @param channel The channel.
     * @param craftingJobId A crafting job id.
     */
    public void cancelCraftingJob(int channel, int craftingJobId);

    /**
     * @param craftingJobId A crafting job id.
     * @return The tick at which the oldest running crafting operation of the given job was started,
     *         or -1 if no operation is running, or if this is unknown.
     */
    public default long getCraftingJobEntryStartTick(int craftingJobId) {
        return -1;
    }

    /**
     * @param recipe A recipe.
     * @return The estimated duration in ticks of a single crafting operation of the given recipe,
     *         based on the operations that were performed by this interface before, or -1 if unknown.
     *         This may fall back to the average duration over all recipes of this interface,
     *         as recipe-specific durations are only remembered for a limited number of recipes,
     *         and are forgotten once they become outdated.
     */
    public default long getEstimatedRecipeDuration(IRecipeDefinition recipe) {
        return -1;
    }

    /**
     * @return The prioritized position of this interface.
     */
    public PrioritizedPartPos getPosition();

    public static Comparator<ICraftingInterface> createComparator() {
        return Comparator.comparing(ICraftingInterface::getPosition);
    }
}
