package org.cyclops.integratedcrafting.api.recipe;

import org.cyclops.integratedcrafting.api.crafting.CraftingJob;

/**
 * A modifiable crafting job index.
 * @author rubensworks
 */
public interface ICraftingJobIndexModifiable extends ICraftingJobIndex {

    /**
     * Add the given crafting job to this index.
     * @param craftingJob A crafting job.
     */
    public void addCraftingJob(CraftingJob craftingJob);

    /**
     * Remove the given crafting job from this index.
     * @param craftingJob A crafting job.
     */
    public void removeCraftingJob(CraftingJob craftingJob);

}
