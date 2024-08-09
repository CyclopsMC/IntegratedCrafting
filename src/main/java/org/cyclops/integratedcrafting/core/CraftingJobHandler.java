package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientSerializer;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobStatus;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integrateddynamics.api.ingredient.IIngredientComponentStorageObservable;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A CraftingJobHandler maintains a list of processing and pending crafting job.
 *
 * Each time that {@link #update(INetwork, int, PartPos)} is called,
 * the handler will observe the target position for changes in the processing job.
 * Also, it will try initiating pending jobs into the target if none was running.
 *
 * If blockingJobsMode is true, then a multi-amount job will only be crafted one-by-one.
 * If false, then as much as possible of that job will be crafted at once.
 *
 * @author rubensworks
 */
public class CraftingJobHandler {

    private final int maxProcessingJobs;
    private boolean blockingJobsMode;
    private final ICraftingResultsSink resultsSink;
    private final Collection<ICraftingProcessOverride> craftingProcessOverrides;

    private final Int2ObjectMap<CraftingJob> allCraftingJobs;
    private final Int2ObjectMap<CraftingJob> processingCraftingJobs;
    private final Int2ObjectMap<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>> processingCraftingJobsPendingIngredients;
    private final Int2ObjectMap<CraftingJob> pendingCraftingJobs;
    private final Object2IntMap<IngredientComponent<?, ?>> ingredientObserverCounters;
    private final Map<IngredientComponent<?, ?>, IIngredientComponentStorageObservable.IIndexChangeObserver<?, ?>> ingredientObservers;
    private final List<IngredientComponent<?, ?>> observersPendingCreation;
    private final List<IngredientComponent<?, ?>> observersPendingDeletion;
    private final Int2ObjectMap<CraftingJob> finishedCraftingJobs;
    private final Map<IngredientComponent<?, ?>, Direction> ingredientComponentTargetOverrides;
    private final Int2IntMap nonBlockingJobsRunningAmount;

    public CraftingJobHandler(int maxProcessingJobs, boolean blockingJobsMode,
                              Collection<ICraftingProcessOverride> craftingProcessOverrides,
                              ICraftingResultsSink resultsSink) {
        this.maxProcessingJobs = maxProcessingJobs;
        this.blockingJobsMode = blockingJobsMode;
        this.resultsSink = resultsSink;
        this.craftingProcessOverrides = craftingProcessOverrides;

        this.allCraftingJobs = new Int2ObjectOpenHashMap<>();
        this.processingCraftingJobs = new Int2ObjectOpenHashMap<>();
        this.pendingCraftingJobs = new Int2ObjectOpenHashMap<>();
        this.processingCraftingJobsPendingIngredients = new Int2ObjectOpenHashMap<>();
        this.ingredientObserverCounters = new Object2IntOpenHashMap<>();
        this.ingredientObservers = Maps.newIdentityHashMap();
        this.observersPendingCreation = Lists.newArrayList();
        this.observersPendingDeletion = Lists.newArrayList();
        this.finishedCraftingJobs = new Int2ObjectOpenHashMap<>();
        this.ingredientComponentTargetOverrides = Maps.newIdentityHashMap();
        this.nonBlockingJobsRunningAmount = new Int2IntOpenHashMap();
    }

    public void writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag tag) {
        tag.putBoolean("blockingJobsMode", this.blockingJobsMode);

        ListTag processingCraftingJobs = new ListTag();
        for (CraftingJob processingCraftingJob : this.processingCraftingJobs.values()) {
            CompoundTag entriesTag = new CompoundTag();
            entriesTag.put("craftingJob", CraftingJob.serialize(lookupProvider, processingCraftingJob));

            List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> ingredientsEntries = this.processingCraftingJobsPendingIngredients.get(processingCraftingJob.getId());
            ListTag pendingEntries = new ListTag();
            for (Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredients : ingredientsEntries) {
                ListTag pendingIngredientInstances = new ListTag();
                for (Map.Entry<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredientComponentListEntry : ingredients.entrySet()) {
                    CompoundTag ingredientInstance = new CompoundTag();

                    IngredientComponent<?, ?> ingredientComponent = ingredientComponentListEntry.getKey();
                    ingredientInstance.putString("ingredientComponent", IngredientComponent.REGISTRY.getKey(ingredientComponent).toString());

                    ListTag instances = new ListTag();
                    IIngredientSerializer serializer = ingredientComponent.getSerializer();
                    for (IPrototypedIngredient<?, ?> prototypedIngredient : ingredientComponentListEntry.getValue()) {
                        CompoundTag instance = new CompoundTag();
                        instance.put("prototype", serializer.serializeInstance(lookupProvider, prototypedIngredient.getPrototype()));
                        instance.put("condition", serializer.serializeCondition(prototypedIngredient.getCondition()));
                        instances.add(instance);
                    }
                    ingredientInstance.put("instances", instances);

                    pendingIngredientInstances.add(ingredientInstance);
                }
                pendingEntries.add(pendingIngredientInstances);
            }
            entriesTag.put("pendingIngredientInstanceEntries", pendingEntries);
            processingCraftingJobs.add(entriesTag);
        }
        tag.put("processingCraftingJobs", processingCraftingJobs);

        ListTag pendingCraftingJobs = new ListTag();
        for (CraftingJob craftingJob : this.pendingCraftingJobs.values()) {
            pendingCraftingJobs.add(CraftingJob.serialize(lookupProvider, craftingJob));
        }
        tag.put("pendingCraftingJobs", pendingCraftingJobs);

        CompoundTag targetOverrides = new CompoundTag();
        for (Map.Entry<IngredientComponent<?, ?>, Direction> entry : this.ingredientComponentTargetOverrides.entrySet()) {
            targetOverrides.putInt(entry.getKey().getName().toString(), entry.getValue().ordinal());
        }
        tag.put("targetOverrides", targetOverrides);

        CompoundTag nonBlockingJobsRunningAmount = new CompoundTag();
        for (Int2IntMap.Entry entry : this.nonBlockingJobsRunningAmount.int2IntEntrySet()) {
            nonBlockingJobsRunningAmount.putInt(String.valueOf(entry.getIntKey()), entry.getIntValue());
        }
        tag.put("nonBlockingJobsRunningAmount", nonBlockingJobsRunningAmount);
    }

    public void readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag tag) {
        if (tag.contains("blockingJobsMode")) {
            this.blockingJobsMode = tag.getBoolean("blockingJobsMode");
        }

        ListTag processingCraftingJobs = tag.getList("processingCraftingJobs", Tag.TAG_COMPOUND);
        for (Tag entry : processingCraftingJobs) {
            CompoundTag entryTag = (CompoundTag) entry;

            List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> pendingIngredientInstanceEntries = Lists.newArrayList();
            if (entryTag.contains("pendingIngredientInstances")) {
                // TODO: for backwards-compatibility, remove this in the next major update
                Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredientInstances = Maps.newIdentityHashMap();
                ListTag pendingIngredientsList = entryTag.getList("pendingIngredientInstances", Tag.TAG_COMPOUND);
                for (Tag pendingIngredient : pendingIngredientsList) {
                    CompoundTag pendingIngredientTag = (CompoundTag) pendingIngredient;
                    String componentName = pendingIngredientTag.getString("ingredientComponent");
                    IngredientComponent<?, ?> ingredientComponent = IngredientComponent.REGISTRY.get(ResourceLocation.parse(componentName));
                    if (ingredientComponent == null) {
                        throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
                    }
                    IIngredientSerializer serializer = ingredientComponent.getSerializer();

                    List<IPrototypedIngredient<?, ?>> pendingIngredients = Lists.newArrayList();
                    for (Tag instanceTagUnsafe : pendingIngredientTag.getList("instances", Tag.TAG_COMPOUND)) {
                        CompoundTag instanceTag = (CompoundTag) instanceTagUnsafe;
                        Object instance = serializer.deserializeInstance(lookupProvider, instanceTag.get("prototype"));
                        Object condition = serializer.deserializeCondition(instanceTag.get("condition"));
                        pendingIngredients.add(new PrototypedIngredient(ingredientComponent, instance, condition));
                    }

                    pendingIngredientInstances.put(ingredientComponent, pendingIngredients);
                }
                pendingIngredientInstanceEntries.add(pendingIngredientInstances);
            } else {
                ListTag ingredientsEntries = entryTag.getList("pendingIngredientInstanceEntries", Tag.TAG_LIST);
                for (Tag ingredientEntry : ingredientsEntries) {
                    ListTag pendingIngredientsList = (ListTag) ingredientEntry;

                    Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredientInstances = Maps.newIdentityHashMap();
                    for (Tag pendingIngredient : pendingIngredientsList) {
                        CompoundTag pendingIngredientTag = (CompoundTag) pendingIngredient;
                        String componentName = pendingIngredientTag.getString("ingredientComponent");
                        IngredientComponent<?, ?> ingredientComponent = IngredientComponent.REGISTRY.get(ResourceLocation.parse(componentName));
                        if (ingredientComponent == null) {
                            throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
                        }
                        IIngredientSerializer serializer = ingredientComponent.getSerializer();

                        List<IPrototypedIngredient<?, ?>> pendingIngredients = Lists.newArrayList();
                        for (Tag instanceTagUnsafe : pendingIngredientTag.getList("instances", Tag.TAG_COMPOUND)) {
                            CompoundTag instanceTag = (CompoundTag) instanceTagUnsafe;
                            Object instance = serializer.deserializeInstance(lookupProvider, instanceTag.get("prototype"));
                            Object condition = serializer.deserializeCondition(instanceTag.get("condition"));
                            pendingIngredients.add(new PrototypedIngredient(ingredientComponent, instance, condition));
                        }

                        pendingIngredientInstances.put(ingredientComponent, pendingIngredients);
                    }

                    pendingIngredientInstanceEntries.add(pendingIngredientInstances);
                }
            }

            CraftingJob craftingJob = CraftingJob.deserialize(lookupProvider, entryTag.getCompound("craftingJob"));

            this.processingCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.processingCraftingJobsPendingIngredients.put(
                    craftingJob.getId(),
                    pendingIngredientInstanceEntries);

        }

        ListTag pendingCraftingJobs = tag.getList("pendingCraftingJobs", Tag.TAG_COMPOUND);
        for (Tag craftingJob : pendingCraftingJobs) {
            CraftingJob craftingJobInstance = CraftingJob.deserialize(lookupProvider, (CompoundTag) craftingJob);
            this.pendingCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
            this.allCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
        }

        // Add required observers to a list so that they will be created in the next tick
        for (List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> valueEntries : this.processingCraftingJobsPendingIngredients.values()) {
            for (Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> value : valueEntries) {
                // It's possible that the same component is added multiple times over different jobs,
                // this is because we want to make sure our counters are correct.
                observersPendingCreation.addAll(value.keySet());
            }
        }

        this.ingredientComponentTargetOverrides.clear();
        CompoundTag targetOverrides = tag.getCompound("targetOverrides");
        for (String componentName : targetOverrides.getAllKeys()) {
            IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.get(ResourceLocation.parse(componentName));
            this.ingredientComponentTargetOverrides.put(component, Direction.values()[targetOverrides.getInt(componentName)]);
        }

        this.nonBlockingJobsRunningAmount.clear();
        CompoundTag nonBlockingJobsRunningAmount = tag.getCompound("nonBlockingJobsRunningAmount");
        for (String key : nonBlockingJobsRunningAmount.getAllKeys()) {
            int craftingJobId = Integer.parseInt(key);
            int amount = nonBlockingJobsRunningAmount.getInt(key);
            this.nonBlockingJobsRunningAmount.put(craftingJobId, amount);
        }
    }

    public boolean setBlockingJobsMode(boolean blockingJobsMode) {
        if (this.blockingJobsMode != blockingJobsMode) {
            this.blockingJobsMode = blockingJobsMode;
            return true;
        }
        return false;
    }

    public boolean isBlockingJobsMode() {
        return blockingJobsMode;
    }

    public boolean canScheduleCraftingJobs() {
        return this.pendingCraftingJobs.size() < GeneralConfig.maxPendingCraftingJobs;
    }

    public void scheduleCraftingJob(CraftingJob craftingJob) {
        this.pendingCraftingJobs.put(craftingJob.getId(), craftingJob);
        this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
        if (!this.isBlockingJobsMode()) {
            this.nonBlockingJobsRunningAmount.put(craftingJob.getId(), 0);
        }
    }

    public Int2ObjectMap<List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>>> getProcessingCraftingJobsPendingIngredients() {
        return processingCraftingJobsPendingIngredients;
    }

    public Int2ObjectMap<CraftingJob> getProcessingCraftingJobsRaw() {
        return processingCraftingJobs;
    }

    public Collection<CraftingJob> getProcessingCraftingJobs() {
        return getProcessingCraftingJobsRaw().values();
    }

    public Collection<CraftingJob> getPendingCraftingJobs() {
        return pendingCraftingJobs.values();
    }

    public void unmarkCraftingJobProcessing(CraftingJob craftingJob) {
        if (this.processingCraftingJobs.remove(craftingJob.getId()) != null) {
            this.processingCraftingJobsPendingIngredients.remove(craftingJob.getId());
            this.pendingCraftingJobs.put(craftingJob.getId(), craftingJob);
        }
    }

    public void addCraftingJobProcessingPendingIngredientsEntry(CraftingJob craftingJob,
                                                                Map<IngredientComponent<?, ?>,
                                                                   List<IPrototypedIngredient<?, ?>>> pendingIngredients) {
        if (pendingIngredients.isEmpty()) {
            this.processingCraftingJobs.remove(craftingJob.getId());
            this.allCraftingJobs.remove(craftingJob.getId());
            this.nonBlockingJobsRunningAmount.remove(craftingJob.getId());
            this.processingCraftingJobsPendingIngredients.remove(craftingJob.getId());

        } else {
            this.processingCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.allCraftingJobs.put(craftingJob.getId(), craftingJob);

            List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> pendingIngredientsEntries = this.processingCraftingJobsPendingIngredients.get(craftingJob.getId());
            if (pendingIngredientsEntries == null) {
                pendingIngredientsEntries = Lists.newArrayList();
                this.processingCraftingJobsPendingIngredients.put(craftingJob.getId(), pendingIngredientsEntries);
            }
            pendingIngredientsEntries.add(pendingIngredients);
        }
    }

    public List<IngredientComponent<?, ?>> getObserversPendingDeletion() {
        return observersPendingDeletion;
    }

    protected <T, M> void registerIngredientObserver(IngredientComponent<T, M> ingredientComponent, INetwork network) {
        int count = ingredientObserverCounters.getInt(ingredientComponent);
        if (count == 0) {
            IPositionedAddonsNetworkIngredients<T, M> ingredientsNetwork = CraftingHelpers
                    .getIngredientsNetworkChecked(network, ingredientComponent);
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(network);
            PendingCraftingJobResultIndexObserver<T, M> observer = new PendingCraftingJobResultIndexObserver<>(ingredientComponent, this, craftingNetwork);
            ingredientsNetwork.addObserver(observer);
            ingredientsNetwork.scheduleObservation();
            ingredientObservers.put(ingredientComponent, observer);
        }
        ingredientObserverCounters.put(ingredientComponent, count + 1);
    }

    protected <T, M> void unregisterIngredientObserver(IngredientComponent<T, M> ingredientComponent, INetwork network) {
        int count = ingredientObserverCounters.getInt(ingredientComponent);
        count--;
        ingredientObserverCounters.put(ingredientComponent, count);
        if (count == 0) {
            IPositionedAddonsNetworkIngredients<T, M> ingredientsNetwork = CraftingHelpers
                    .getIngredientsNetworkChecked(network, ingredientComponent);
            IIngredientComponentStorageObservable.IIndexChangeObserver<T, M> observer =
                    (IIngredientComponentStorageObservable.IIndexChangeObserver<T, M>) ingredientObservers
                            .remove(ingredientComponent);
            ingredientsNetwork.removeObserver(observer);
        }
    }

    public void onCraftingJobFinished(CraftingJob craftingJob) {
        this.processingCraftingJobs.remove(craftingJob.getId());
        this.pendingCraftingJobs.remove(craftingJob.getId());
        this.finishedCraftingJobs.put(craftingJob.getId(), craftingJob);
        this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
    }

    // This does the same as above, just based on crafting job id
    public void markCraftingJobFinished(int craftingJobId) {
        this.processingCraftingJobsPendingIngredients.remove(craftingJobId);
        this.processingCraftingJobs.remove(craftingJobId);
        this.pendingCraftingJobs.remove(craftingJobId);

        // Needed so that we remove the job in the next tick
        CraftingJob craftingJob = this.allCraftingJobs.get(craftingJobId);
        this.finishedCraftingJobs.put(craftingJobId, craftingJob);
        craftingJob.setAmount(0);
    }

    public void reRegisterObservers(INetwork network) {
        for (Map.Entry<IngredientComponent<?, ?>, IIngredientComponentStorageObservable.IIndexChangeObserver<?, ?>> entry : ingredientObservers.entrySet()) {
            IPositionedAddonsNetworkIngredients ingredientsNetwork = CraftingHelpers
                    .getIngredientsNetworkChecked(network, entry.getKey());
            ingredientsNetwork.addObserver(entry.getValue());
        }
    }

    public void onCraftingJobEntryFinished(ICraftingNetwork craftingNetwork, int craftingJobId) {
        CraftingJob craftingJob = this.allCraftingJobs.get(craftingJobId);
        craftingJob.setAmount(craftingJob.getAmount() - 1);

        if (this.nonBlockingJobsRunningAmount.containsKey(craftingJobId)) {
            this.nonBlockingJobsRunningAmount.put(craftingJobId, this.nonBlockingJobsRunningAmount.get(craftingJobId) - 1);
        }

        // We mark each dependent job that it may attempt to be started,
        // because its (partially) finished dependency may have produced ingredients to already start part of this job.
        for (CraftingJob dependent : craftingNetwork.getCraftingJobDependencyGraph().getDependents(craftingJob)) {
            dependent.setIgnoreDependencyCheck(true);
        }
    }

    public void update(INetwork network, int channel, PartPos targetPos) {
        // Create creation-pending observers
        if (observersPendingCreation.size() > 0) {
            for (IngredientComponent<?, ?> ingredientComponent : observersPendingCreation) {
                registerIngredientObserver(ingredientComponent, network);
            }
            observersPendingCreation.clear();
        }

        // Remove removal-pending observers
        if (observersPendingDeletion.size() > 0) {
            for (IngredientComponent<?, ?> ingredientComponent : observersPendingDeletion) {
                unregisterIngredientObserver(ingredientComponent, network);
            }
            observersPendingDeletion.clear();
        }

        // Notify the network of finalized crafting jobs
        if (finishedCraftingJobs.size() > 0) {
            for (CraftingJob finishedCraftingJob : finishedCraftingJobs.values()) {
                if (finishedCraftingJob.getAmount() == 0) {
                    // If the job is fully finished, remove it from the network
                    ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(network);
                    craftingNetwork.onCraftingJobFinished(finishedCraftingJob);
                    allCraftingJobs.remove(finishedCraftingJob.getId());
                    nonBlockingJobsRunningAmount.remove(finishedCraftingJob.getId());
                } else {
                    // Re-add it to the pending jobs list if entries are remaining
                    pendingCraftingJobs.put(finishedCraftingJob.getId(), finishedCraftingJob);
                }
            }
            finishedCraftingJobs.clear();
        }

        // The actual output observation of processing jobs is done via the ingredient observers
        int processingJobs = getProcessingCraftingJobs().size();

        // Enable the observers for the next tick
        if (processingJobs > 0) {
            for (IngredientComponent<?, ?> ingredientComponent : ingredientObservers.keySet()) {
                IPositionedAddonsNetworkIngredients<?, ?> ingredientsNetwork = CraftingHelpers.getIngredientsNetworkChecked(network, ingredientComponent);
                ingredientsNetwork.scheduleObservation();
            }
        }

        // Process the jobs that are in non-blocking mode and still require amounts to be processed by re-trying insertion
        if (!this.nonBlockingJobsRunningAmount.isEmpty()) {
            for (Int2IntMap.Entry entry : this.nonBlockingJobsRunningAmount.int2IntEntrySet()) {
                int craftingJobId = entry.getIntKey();
                int runningAmount = entry.getIntValue();
                CraftingJob craftingJob = this.allCraftingJobs.get(craftingJobId);
                if (runningAmount > 0 && runningAmount < craftingJob.getAmount()) {
                    insertLoopNonBlocking(network, channel, targetPos, craftingJob);
                }
            }
        }

        if (processingJobs < this.maxProcessingJobs) {
            // Handle crafting jobs
            CraftingJob startingCraftingJob = null;
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(network);
            CraftingJobDependencyGraph dependencyGraph = craftingNetwork.getCraftingJobDependencyGraph();
            for (CraftingJob pendingCraftingJob : getPendingCraftingJobs()) {
                // Make sure that this crafting job has no incomplete dependency jobs
                // This check can be overridden if the ignoreDependencyCheck flag is set
                // (which is done once a dependent finishes a job entry).
                // This override only applies for a single tick.
                if (dependencyGraph.hasDependencies(pendingCraftingJob) && !pendingCraftingJob.isIgnoreDependencyCheck()) {
                    continue;
                }
                if (pendingCraftingJob.isIgnoreDependencyCheck()) {
                    pendingCraftingJob.setIgnoreDependencyCheck(false);
                }

                // Check if pendingCraftingJob can start and set as startingCraftingJob
                // This requires checking the available ingredients AND if the crafting handler can accept it.
                Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> inputs = CraftingHelpers.getRecipeInputs(
                        CraftingHelpers.getNetworkStorageGetter(network, pendingCraftingJob.getChannel(), false),
                        pendingCraftingJob.getRecipe(), true, Maps.newIdentityHashMap(), Maps.newIdentityHashMap(), true, 1);
                if (inputs.getRight().isEmpty()) { // If we have no missing ingredients
                    if (insertCrafting(targetPos, new MixedIngredients(inputs.getLeft()), network, channel, true)) {
                        startingCraftingJob = pendingCraftingJob;
                        startingCraftingJob.setInvalidInputs(false);
                        break;
                    } else {
                        pendingCraftingJob.setInvalidInputs(true);
                    }
                } else {
                    // Register listeners for pending ingredients
                    if (pendingCraftingJob.getLastMissingIngredients().isEmpty()) {
                        for (IngredientComponent<?, ?> component : inputs.getRight().keySet()) {
                            registerIngredientObserver(component, network);

                            // For the missing ingredients that are reusable,
                            // trigger a crafting job for them if no job is running yet.
                            // This special case is needed because reusable ingredients are usually durability-based,
                            // and may be consumed _during_ a bulk crafting job.
                            MissingIngredients<?, ?> missingIngredients = inputs.getRight().get(component);
                            for (MissingIngredients.Element<?, ?> element : missingIngredients.getElements()) {
                                if (element.isInputReusable()) {
                                    for (MissingIngredients.PrototypedWithRequested alternative : element.getAlternatives()) {
                                        // Try to start crafting jobs for each alternative until one of them succeeds.
                                        if (CraftingHelpers.isCrafting(craftingNetwork, channel,
                                                alternative.getRequestedPrototype().getComponent(), alternative.getRequestedPrototype().getPrototype(), alternative.getRequestedPrototype().getCondition())) {
                                            // Break loop if we have found an existing job for our dependency
                                            // This may occur if a crafting job was triggered in a parallelized job
                                            break;
                                        }
                                        CraftingJob craftingJob = CraftingHelpers.calculateAndScheduleCraftingJob(network, channel,
                                                alternative.getRequestedPrototype().getComponent(), alternative.getRequestedPrototype().getPrototype(), alternative.getRequestedPrototype().getCondition(), true, true,
                                                CraftingHelpers.getGlobalCraftingJobIdentifier(), null);
                                        if (craftingJob != null) {
                                            pendingCraftingJob.addDependency(craftingJob);
                                            // Break loop once we have found a valid job
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    pendingCraftingJob.setLastMissingIngredients(inputs.getRight());
                }
            }

            // Start the crafting job
            if (startingCraftingJob != null) {
                // If the job previously had missing in ingredients, unregister the observers that were previously created for it.
                if (!startingCraftingJob.getLastMissingIngredients().isEmpty()) {
                    for (IngredientComponent<?, ?> component : startingCraftingJob.getLastMissingIngredients().keySet()) {
                        unregisterIngredientObserver(component, network);
                    }
                    startingCraftingJob.setLastMissingIngredients(Maps.newIdentityHashMap());
                }

                // Check if the job was started while blocking mode was enabled in this handler
                boolean blockingMode = !nonBlockingJobsRunningAmount.containsKey(startingCraftingJob.getId()) || startingCraftingJob.getAmount() == 1;

                // Start the actual crafting
                boolean couldCraft = consumeAndInsertCrafting(blockingMode, network, channel, targetPos, startingCraftingJob);

                // Keep inserting as much as possible if non-blocking
                if (couldCraft && !blockingMode) {
                    nonBlockingJobsRunningAmount.put(startingCraftingJob.getId(), 1);
                    insertLoopNonBlocking(network, channel, targetPos, startingCraftingJob);
                }
            }
        }
    }

    protected boolean insertCrafting(PartPos target, IMixedIngredients ingredients, INetwork network, int channel, boolean simulate) {
        Function<IngredientComponent<?, ?>, PartPos> targetGetter = getTargetGetter(target);
        // First check our crafting overrides
        for (ICraftingProcessOverride craftingProcessOverride : this.craftingProcessOverrides) {
            if (craftingProcessOverride.isApplicable(target)) {
                return craftingProcessOverride.craft(targetGetter, ingredients, this.resultsSink, simulate);
            }
        }

        // Fallback to default crafting insertion
        return CraftingHelpers.insertCrafting(targetGetter, ingredients, network, channel, simulate);
    }

    protected void insertLoopNonBlocking(INetwork network, int channel, PartPos targetPos, CraftingJob craftingJob) {
        // If in non-blocking mode, try to push as much as possible into the target
        while (nonBlockingJobsRunningAmount.get(craftingJob.getId()) < craftingJob.getAmount()) {
            IMixedIngredients ingredientsSimulated = CraftingHelpers.getRecipeInputs(network, craftingJob.getChannel(),
                    craftingJob.getRecipe(), true, 1);
            if (ingredientsSimulated == null ||!insertCrafting(targetPos, ingredientsSimulated, network, channel, true)) {
                break;
            }
            if (!consumeAndInsertCrafting(true, network, channel, targetPos, craftingJob)) {
                break;
            }
            nonBlockingJobsRunningAmount.put(craftingJob.getId(), nonBlockingJobsRunningAmount.get(craftingJob.getId()) + 1);
        }
    }

    protected boolean consumeAndInsertCrafting(boolean blockingMode, INetwork network, int channel, PartPos targetPos, CraftingJob startingCraftingJob) {
        // Remove ingredients from network
        IMixedIngredients ingredients = CraftingHelpers.getRecipeInputs(network, startingCraftingJob.getChannel(),
                startingCraftingJob.getRecipe(), false, 1);

        // This may not be null, error if it is null!
        if (ingredients != null) {
            this.pendingCraftingJobs.remove(startingCraftingJob.getId());

            // Update state with expected outputs
            addCraftingJobProcessingPendingIngredientsEntry(startingCraftingJob,
                    CraftingHelpers.getRecipeOutputs(startingCraftingJob.getRecipe()));

            // Register listeners for pending ingredients
            for (IngredientComponent<?, ?> component : startingCraftingJob.getRecipe().getOutput().getComponents()) {
                registerIngredientObserver(component, network);
            }

            // Push the ingredients to the crafting interface
            if (!insertCrafting(targetPos, ingredients, network, channel, false)) {
                // Unregister listeners again for pending ingredients
                for (IngredientComponent<?, ?> component : startingCraftingJob.getRecipe().getOutput().getComponents()) {
                    unregisterIngredientObserver(component, network);
                }

                // If we reach this point, the target does not accept the recipe inputs,
                // even though they were acceptable in simulation mode.
                // The failed ingredients were already re-inserted into the network at this point,
                // so we mark the job as failed, and add it again to the queue.
                startingCraftingJob.setInvalidInputs(true);
                unmarkCraftingJobProcessing(startingCraftingJob);
                return false;
            } else {
                return true;
            }
        } else {
            IntegratedCrafting.clog(Level.WARN, "Failed to extract ingredients for crafting job " + startingCraftingJob.getId());
            return false;
        }
    }

    public CraftingJobStatus getCraftingJobStatus(ICraftingNetwork network, int channel, int craftingJobId) {
        if (pendingCraftingJobs.containsKey(craftingJobId)) {
            CraftingJob craftingJob = allCraftingJobs.get(craftingJobId);
            if (craftingJob != null && craftingJob.isInvalidInputs()) {
                return CraftingJobStatus.INVALID_INPUTS;
            }

            CraftingJobDependencyGraph dependencyGraph = network.getCraftingJobDependencyGraph();
            if (dependencyGraph.hasDependencies(craftingJobId)) {
                return CraftingJobStatus.PENDING_DEPENDENCIES;
            } else {
                if (!craftingJob.getLastMissingIngredients().isEmpty()) {
                    return CraftingJobStatus.PENDING_INGREDIENTS;
                } else {
                    return CraftingJobStatus.PENDING_INTERFACE;
                }
            }
        } else if (processingCraftingJobs.containsKey(craftingJobId)) {
            return CraftingJobStatus.PROCESSING;
        } else if (finishedCraftingJobs.containsKey(craftingJobId)) {
            return CraftingJobStatus.FINISHED;
        }
        return CraftingJobStatus.UNKNOWN;
    }

    public Int2ObjectMap<CraftingJob> getAllCraftingJobs() {
        return allCraftingJobs;
    }

    public void setIngredientComponentTarget(IngredientComponent<?, ?> ingredientComponent, @Nullable Direction side) {
        if (side == null) {
            this.ingredientComponentTargetOverrides.remove(ingredientComponent);
        } else {
            this.ingredientComponentTargetOverrides.put(ingredientComponent, side);
        }
    }

    @Nullable
    public Direction getIngredientComponentTarget(IngredientComponent<?, ?> ingredientComponent) {
        return this.ingredientComponentTargetOverrides.get(ingredientComponent);
    }

    public Function<IngredientComponent<?, ?>, PartPos> getTargetGetter(PartPos defaultPosition) {
        return ingredientComponent -> {
            Direction sideOverride = this.ingredientComponentTargetOverrides.get(ingredientComponent);
            if (sideOverride == null) {
                return defaultPosition;
            } else {
                return PartPos.of(defaultPosition.getPos(), sideOverride);
            }
        };
    }
}
