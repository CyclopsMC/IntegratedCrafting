package org.cyclops.integratedcrafting.core;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.cyclops.commoncapabilities.api.ingredient.*;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollection;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.cyclopscore.ingredient.storage.IngredientComponentStorageCollectionWrapper;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integrateddynamics.api.ingredient.IIngredientComponentStorageObservable;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.INetworkIngredientsChannel;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;

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
public class PendingCraftingJobResultIndexObserver<T, M>
        implements IIngredientComponentStorageObservable.IIndexChangeObserver<T, M> {

    private final IngredientComponent<T, M> ingredientComponent;
    private final CraftingJobHandler handler;
    private final ICraftingNetwork craftingNetwork;
    private final IPositionedAddonsNetworkIngredients<T, M> ingredientsNetwork;
    private final INetwork network;

    public PendingCraftingJobResultIndexObserver(IngredientComponent<T, M> ingredientComponent, CraftingJobHandler handler, ICraftingNetwork craftingNetwork, IPositionedAddonsNetworkIngredients<T, M> ingredientsNetwork, INetwork network) {
        this.ingredientComponent = ingredientComponent;
        this.handler = handler;
        this.craftingNetwork = craftingNetwork;
        this.ingredientsNetwork = ingredientsNetwork;
        this.network = network;
    }

    @Override
    public void onChange(IIngredientComponentStorageObservable.StorageChangeEvent<T, M> event) { // If changes are made here, also change in method below!!! (only partially abstracted...)
        // This adds the given instance to the waiting crafting jobs if they have been detected in storage.
        // This only acts as a fallback to instances being detected once they are flushed from the crafting interface,
        // which will cause the method below to be invoked.
        // This will first attempt to complete running jobs that are awaiting results.
        // Then, it will try to give the instance to the dependent jobs based on their missing ingredients.
        if (event.getChangeType() == IIngredientComponentStorageObservable.Change.ADDITION
                // If we're still initializing the network, skip addition events.
                // Otherwise, we could incorrectly mark running crafting jobs as finished.
                && !event.isInitialChange()) {
            IIngredientCollection<T, M> addedIngredients = event.getInstances();
            IIngredientComponentStorage<T, M> ingredientsHayStack = null; // A mutable copy of addedIngredients (lazily created)
            IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();

            Int2ObjectMap<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>> processingJobs = handler.getProcessingCraftingJobsPendingIngredients();
            ObjectIterator<Int2ObjectMap.Entry<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>>> jobsEntryIt = processingJobs.int2ObjectEntrySet().iterator();
            while (jobsEntryIt.hasNext()) {
                Int2ObjectMap.Entry<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>> jobsEntry = jobsEntryIt.next();
                int craftingJobId = jobsEntry.getIntKey();
                // Only check jobs that have a matching channel with the event
                CraftingJob craftingJob = handler.getAllCraftingJobs().get(jobsEntry.getIntKey());
                if (craftingJob != null
                        && (craftingJob.getChannel() == IPositionedAddonsNetwork.WILDCARD_CHANNEL || craftingJob.getChannel() == event.getChannel())) {
                    Iterator<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> jobEntryIt = jobsEntry.getValue().iterator();
                    while (jobEntryIt.hasNext()) {
                        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> jobEntry = jobEntryIt.next();
                        List<IPrototypedIngredient<?, ?>> pendingIngredientsUnsafe = jobEntry.get(ingredientComponent);
                        if (pendingIngredientsUnsafe != null) {
                            // Remove pending ingredients that were added in the event
                            List<IPrototypedIngredient<T, M>> pendingIngredients = (List<IPrototypedIngredient<T, M>>) (Object) pendingIngredientsUnsafe;

                            // Iterate over all pending ingredients for this ingredient component
                            ListIterator<IPrototypedIngredient<T, M>> it = pendingIngredients.listIterator();
                            while (it.hasNext()) {
                                IPrototypedIngredient<T, M> prototypedIngredient = it.next();
                                final long initialQuantity = matcher.getQuantity(prototypedIngredient.getPrototype());
                                long remainingQuantity = initialQuantity;

                                // Lazily create ingredientsHayStack only when needed,
                                // because we need to copy all ingredients from addedIngredients,
                                // which can get expensive
                                // We need to make a copy because multiple crafting jobs can have the same pending instances,
                                // so each instance may only be consumed by a single crafting job.
                                if (ingredientsHayStack == null) {
                                    if (addedIngredients.contains(prototypedIngredient.getPrototype(),
                                            prototypedIngredient.getCondition())) {
                                        IngredientCollectionPrototypeMap<T, M> prototypeMap = new IngredientCollectionPrototypeMap<>(ingredientComponent);
                                        ingredientsHayStack = new IngredientComponentStorageCollectionWrapper<>(prototypeMap);
                                        prototypeMap.addAll(addedIngredients);
                                    } else {
                                        continue;
                                    }
                                }

                                // Iteratively extract the pending ingredient from the hay stack.
                                T extracted;
                                do {
                                    extracted = ingredientsHayStack.extract(prototypedIngredient.getPrototype(),
                                            prototypedIngredient.getCondition(), false);
                                    long extractedQuantity = matcher.getQuantity(extracted);

                                    if (matcher.isEmpty(extracted)) {
                                        // Quickly break when no matches are available anymore
                                        break;
                                    } else {
                                        long extractedQuantityToAssign = extractedQuantity;
                                        // Move this ingredient from storage to dependent crafting jobs.
                                        // We only consider jobs that have this instance as missing ingredient.
                                        IntIterator dependentJobs = craftingJob.getDependentCraftingJobs().intIterator();
                                        while (dependentJobs.hasNext()) {
                                            CraftingJob dependentJob = craftingNetwork.getCraftingJob(craftingJob.getChannel(), dependentJobs.nextInt());
                                            if (dependentJob != null) {
                                                long missingQuantity = dependentJob.getMissingIngredientQuantity(ingredientComponent, extracted);
                                                if (missingQuantity > 0) {
                                                    INetworkIngredientsChannel<T, M> storage = this.ingredientsNetwork.getChannelInternal(craftingJob.getChannel());
                                                    T toExtract = matcher.withQuantity(extracted, Math.min(missingQuantity, extractedQuantityToAssign));
                                                    T extractedFromStorage = storage.extract(toExtract, matcher.getExactMatchCondition(), false);
                                                    if (!matcher.matchesExactly(toExtract, extractedFromStorage)) {
                                                        IntegratedCrafting.clog("Unable to extract ingredient from storage for pending crafting job: " + toExtract);
                                                        storage.insert(extractedFromStorage, false);
                                                    } else {
                                                        dependentJob.addToIngredientsStorageBuffer(ingredientComponent, extractedFromStorage);
                                                    }
                                                    extractedQuantityToAssign -= matcher.getQuantity(extractedFromStorage);
                                                    if (extractedQuantityToAssign == 0) {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    remainingQuantity -= extractedQuantity;
                                } while (!matcher.isEmpty(extracted) && remainingQuantity > 0);

                                // Update the list if the prototype has changed.
                                if (remainingQuantity <= 0) {
                                    it.remove();
                                } else if (initialQuantity != remainingQuantity) {
                                    it.set(new PrototypedIngredient<>(ingredientComponent,
                                            matcher.withQuantity(prototypedIngredient.getPrototype(), remainingQuantity),
                                            prototypedIngredient.getCondition()));
                                }
                            }

                            onPendingIngredientsEmpty(jobsEntryIt, jobsEntry, jobEntryIt, jobEntry, pendingIngredients, craftingJobId);
                        }
                    }
                }
            }
        }
    }

    /**
     * This adds the given instance to the waiting crafting jobs directly, without having to go through the network storage and its observers.
     * Like the method above, this will first attempt to complete running jobs that are awaiting results.
     * Then, it will try to give the instance to the dependent jobs based on their missing ingredients.
     * @param instanceWrapper The instance to add.
     * @param channel The channel.
     * @return The remaining instance that could not be given to any jobs that had missing ingredients.
     */
    public IngredientInstanceWrapper<T, M> addIngredient(IngredientInstanceWrapper<T, M> instanceWrapper, int channel) {
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
                                            dependentJob.addToIngredientsStorageBuffer(ingredientComponent, toExtract);
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
                            if (remainingQuantity <= 0) {
                                it.remove();
                            } else if (initialQuantity != remainingQuantity) {
                                it.set(new PrototypedIngredient<>(ingredientComponent,
                                        matcher.withQuantity(prototypedIngredient.getPrototype(), remainingQuantity),
                                        prototypedIngredient.getCondition()));
                            }
                        }

                        onPendingIngredientsEmpty(jobsEntryIt, jobsEntry, jobEntryIt, jobEntry, pendingIngredients, craftingJobId);
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


}
