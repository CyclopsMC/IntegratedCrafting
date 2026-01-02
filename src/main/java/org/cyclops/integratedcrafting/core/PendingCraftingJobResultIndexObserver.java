package org.cyclops.integratedcrafting.core;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.cyclops.commoncapabilities.api.ingredient.*;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.core.network.IIngredientChannelInsertPreConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * An ingredient index observer that tracks crafting job outputs for a certain ingredient component type.
 *
 * It will observe changes and (partially) resolve awaiting crafting job outputs when applicable.
 *
 * @author rubensworks
 */
public class PendingCraftingJobResultIndexObserver<T, M> implements IIngredientChannelInsertPreConsumer<T> {

    private final IngredientComponent<T, M> ingredientComponent;
    private final CraftingJobHandler handler;
    private final ICraftingNetwork craftingNetwork;

    public PendingCraftingJobResultIndexObserver(IngredientComponent<T, M> ingredientComponent, CraftingJobHandler handler, ICraftingNetwork craftingNetwork) {
        this.ingredientComponent = ingredientComponent;
        this.handler = handler;
        this.craftingNetwork = craftingNetwork;
    }

    /**
     * This adds the given instance to the waiting crafting jobs directly, without having to go through the network storage and its observers.
     * Like the method above, this will first attempt to complete running jobs that are awaiting results.
     * Then, it will try to give the instance to the dependent jobs based on their missing ingredients.
     * @param instanceWrapper The instance to add.
     * @param channel The channel.
     * @param simulate Simulate mode.
     * @return The remaining instance that could not be given to any jobs that had missing ingredients.
     */
    public IngredientInstanceWrapper<T, M> addIngredient(IngredientInstanceWrapper<T, M> instanceWrapper, int channel, boolean simulate) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        long instanceAmount = matcher.getQuantity(instanceWrapper.getInstance());

        Int2ObjectMap<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>> processingJobs = handler.getProcessingCraftingJobsPendingIngredients();
        ObjectIterator<Int2ObjectMap.Entry<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>>> jobsEntryIt = processingJobs.int2ObjectEntrySet().iterator();
        while (jobsEntryIt.hasNext() && instanceAmount > 0) {
            Int2ObjectMap.Entry<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>> jobsEntry = jobsEntryIt.next();
            int craftingJobId = jobsEntry.getIntKey();
            // Only check jobs that have a matching channel with the event
            CraftingJob craftingJob = handler.getAllCraftingJobs().get(jobsEntry.getIntKey());
            if (craftingJob != null
                    && (craftingJob.getChannel() == IPositionedAddonsNetwork.WILDCARD_CHANNEL || craftingJob.getChannel() == channel)) {
                Iterator<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> jobEntryIt = jobsEntry.getValue().iterator();
                while (jobEntryIt.hasNext() && instanceAmount > 0) {
                    Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> jobEntry = jobEntryIt.next();
                    List<IPrototypedIngredient<?, ?>> pendingIngredientsUnsafe = jobEntry.get(ingredientComponent);
                    if (pendingIngredientsUnsafe != null) {
                        // Remove pending ingredients based on the given instance.
                        List<IPrototypedIngredient<T, M>> pendingIngredients = (List<IPrototypedIngredient<T, M>>) (Object) pendingIngredientsUnsafe;

                        // Iterate over all pending ingredients for this ingredient component
                        ListIterator<IPrototypedIngredient<T, M>> it = pendingIngredients.listIterator();
                        while (it.hasNext() && instanceAmount > 0) {
                            IPrototypedIngredient<T, M> prototypedIngredient = it.next();
                            final long initialQuantity = matcher.getQuantity(prototypedIngredient.getPrototype());
                            long remainingQuantity = initialQuantity;

                            // Check if the instance matches the job's expected outputs.
                            if (matcher.matches(instanceWrapper.getInstance(), prototypedIngredient.getPrototype(),
                                    prototypedIngredient.getCondition())) {
                                long extractedQuantityToAssign = Math.min(remainingQuantity, instanceAmount);
                                remainingQuantity -= extractedQuantityToAssign;
                                T extracted = matcher.withQuantity(instanceWrapper.getInstance(), extractedQuantityToAssign);

                                // Move this ingredient from storage to dependent crafting jobs.
                                // We only consider jobs that have this instance as missing ingredient.
                                IntIterator dependentJobs = craftingJob.getDependentCraftingJobs().intIterator();
                                while (dependentJobs.hasNext()) {
                                    CraftingJob dependentJob = craftingNetwork.getCraftingJob(craftingJob.getChannel(), dependentJobs.nextInt());
                                    if (dependentJob != null) {
                                        long missingQuantity = dependentJob.getMissingIngredientQuantity(ingredientComponent, extracted);
                                        if (missingQuantity > 0) {
                                            long toExtractQuantity = Math.min(missingQuantity, extractedQuantityToAssign);
                                            T toExtract = matcher.withQuantity(extracted, toExtractQuantity);
                                            if (!simulate) {
                                                dependentJob.addToIngredientsStorageBuffer(ingredientComponent, toExtract);
                                            }
                                            instanceAmount -= toExtractQuantity;
                                            extractedQuantityToAssign -= toExtractQuantity;
                                            if (extractedQuantityToAssign == 0) {
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            // Update the list if the prototype has changed.
                            if (!simulate) {
                                if (remainingQuantity <= 0) {
                                    it.remove();
                                } else if (initialQuantity != remainingQuantity) {
                                    it.set(new PrototypedIngredient<>(ingredientComponent,
                                            matcher.withQuantity(prototypedIngredient.getPrototype(), remainingQuantity),
                                            prototypedIngredient.getCondition()));
                                }
                            }
                        }

                        if (!simulate) {
                            onPendingIngredientsEmpty(jobsEntryIt, jobsEntry, jobEntryIt, jobEntry, pendingIngredients, craftingJobId);
                        }
                    }
                }
            }
        }

        return new IngredientInstanceWrapper<>(instanceWrapper.getComponent(), matcher.withQuantity(instanceWrapper.getInstance(), instanceAmount));
    }

    // If no prototypes for this component type for this crafting job for this job entry are pending.
    // If changes are made here, also change in method above!!! (only partially abstracted...)
    protected void onPendingIngredientsEmpty(ObjectIterator<Int2ObjectMap.Entry<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>>> jobsEntryIt,
                                             Int2ObjectMap.Entry<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>> jobsEntry,
                                             Iterator<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> jobEntryIt,
                                             Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> jobEntry,
                                             List<IPrototypedIngredient<T, M>> pendingIngredients,
                                             int craftingJobId) {
        if (pendingIngredients.isEmpty()) {
            // Remove observer (in next tick) when all pending ingredients are resolved
            handler.getObserversPendingDeletion().add(ingredientComponent);

            // Remove crafting job if needed.
            // No ingredients of this ingredient component type are pending
            jobEntry.remove(ingredientComponent);
            if (jobEntry.isEmpty()) {
                // No ingredients are pending for this non-blocking-mode-entry are pending
                handler.onCraftingJobEntryFinished(this.craftingNetwork, craftingJobId);
                jobEntryIt.remove();
            }
            if (jobsEntry.getValue().isEmpty()) {
                // No more entries for this crafting job are pending
                handler.onCraftingJobFinished(handler.getAllCraftingJobs().get(craftingJobId));
                handler.getProcessingCraftingJobsRaw().remove(craftingJobId);
                jobsEntryIt.remove();
            }
        }
    }

    @Override
    public T insert(int channel, @NotNull T ingredient, boolean simulate) {
        IngredientInstanceWrapper<T, M> instanceWrapper = addIngredient(new IngredientInstanceWrapper<>(ingredientComponent, ingredient), channel, simulate);
        return instanceWrapper.getInstance();
    }
}
