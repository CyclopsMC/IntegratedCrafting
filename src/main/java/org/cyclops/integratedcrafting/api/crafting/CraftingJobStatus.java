package org.cyclops.integratedcrafting.api.crafting;

/**
 * Status types for a {@link CraftingJob} in an {@link ICraftingInterface}.
 * @author rubensworks
 */
public enum CraftingJobStatus {
    /**
     * The crafting job has been scheduled, but is not processing yet.
     */
    PENDING,
    /**
     * The crafting job is actively processing.
     */
    PROCESSING,
    /**
     * The crafting job is completed.
     */
    FINISHED,
    /**
     * The status for the job is unknown.
     */
    UNKNOWN
}
