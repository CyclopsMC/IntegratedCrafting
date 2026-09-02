package org.cyclops.integratedcrafting.api.event;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;

/**
 * An event that is emitted on the NeoForge event bus when a crafting job has been completed.
 *
 * This is only emitted for jobs that ran to completion,
 * so not for jobs that were cancelled or that were removed together with their crafting interface.
 *
 * This is emitted for every completed job, including the dependencies of a job.
 * Jobs that were requested directly can be identified via {@link #isRootJob()}.
 *
 * @author rubensworks
 */
public class CraftingJobFinishedEvent extends Event {

    private final ICraftingNetwork craftingNetwork;
    private final CraftingJob craftingJob;
    private final boolean rootJob;

    public CraftingJobFinishedEvent(ICraftingNetwork craftingNetwork, CraftingJob craftingJob) {
        this.craftingNetwork = craftingNetwork;
        this.craftingJob = craftingJob;
        this.rootJob = craftingJob.getDependentCraftingJobs().isEmpty();
    }

    /**
     * @return The crafting network in which the job was running.
     */
    public ICraftingNetwork getCraftingNetwork() {
        return craftingNetwork;
    }

    /**
     * @return The completed crafting job.
     */
    public CraftingJob getCraftingJob() {
        return craftingJob;
    }

    /**
     * @return If the job was requested directly, as opposed to being a dependency of another job.
     *         This is captured when the event is created,
     *         as the job's dependency links are cleared once it is removed from its network.
     */
    public boolean isRootJob() {
        return rootJob;
    }

    public static void post(ICraftingNetwork craftingNetwork, CraftingJob craftingJob) {
        NeoForge.EVENT_BUS.post(new CraftingJobFinishedEvent(craftingNetwork, craftingJob));
    }

}
