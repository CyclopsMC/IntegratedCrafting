package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Collection;
import java.util.Iterator;

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

}
