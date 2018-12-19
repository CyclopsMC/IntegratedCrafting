package org.cyclops.integratedcrafting.api.crafting;

/**
 * Status types for a {@link CraftingJob} in an {@link ICraftingInterface}.
 * @author rubensworks
 */
public enum CraftingJobStatus {
    /**
     * The crafting job has been scheduled,
     * but is not processing yet because a dependency is still being processed.
     */
    PENDING_DEPENDENCIES,
    /**
     * The crafting job has been scheduled,
     * but is not processing yet because input ingredients are missing.
     */
    PENDING_INGREDIENTS,
    /**
     * The crafting job is actively processing,
     * and output ingredients are being awaited.
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
