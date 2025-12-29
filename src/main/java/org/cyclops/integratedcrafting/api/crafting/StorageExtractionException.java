package org.cyclops.integratedcrafting.api.crafting;

/**
 * An exception for when the storage ingredients for a crafting job could not be extracted.
 *
 * @author rubensworks
 */
public class StorageExtractionException extends Exception {

    private final CraftingJob craftingJob;

    public StorageExtractionException(CraftingJob craftingJob) {
        this.craftingJob = craftingJob;
    }

    public CraftingJob getCraftingJob() {
        return craftingJob;
    }
}
