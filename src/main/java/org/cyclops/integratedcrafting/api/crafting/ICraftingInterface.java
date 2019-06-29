package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integrateddynamics.api.part.PrioritizedPartPos;

import java.util.Collection;
import java.util.Comparator;
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
     * @return Get the number of scheduled and running crafting jobs in this interface.
     */
    public int getCraftingJobsCount();

    /**
     * @return Get the scheduled and running crafting jobs in this interface.
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
     * @return The prioritized position of this interface.
     */
    public PrioritizedPartPos getPosition();

    public static Comparator<ICraftingInterface> createComparator() {
        return Comparator.comparing(ICraftingInterface::getPosition);
    }

}
