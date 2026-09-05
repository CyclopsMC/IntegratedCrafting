package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.*;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollectionMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.*;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
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
    private final Map<IngredientComponent<?, ?>, PendingCraftingJobResultIndexObserver<?, ?>> ingredientObservers;
    private final List<IngredientComponent<?, ?>> observersPendingCreation;
    private final List<IngredientComponent<?, ?>> observersPendingDeletion;
    private final Int2ObjectMap<CraftingJob> finishedCraftingJobs;
    private final Map<IngredientComponent<?, ?>, Direction> ingredientComponentTargetOverrides;
    private final Int2IntMap nonBlockingJobsRunningAmount;
    private final Int2ObjectMap<LongList> processingCraftingJobsStartTicks;
    private RecipeDurationStatistics recipeDurationStatistics;

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
        this.processingCraftingJobsStartTicks = new Int2ObjectOpenHashMap<>();
    }

    public void serialize(ValueOutput valueOutput) {
        valueOutput.putBoolean("blockingJobsMode", this.blockingJobsMode);

        ValueOutput.ValueOutputList processingCraftingJobs = valueOutput.childrenList("processingCraftingJobs");
        for (CraftingJob processingCraftingJob : this.processingCraftingJobs.values()) {
            ValueOutput entriesTag = processingCraftingJobs.addChild();
            CraftingJob.serialize(entriesTag.child("craftingJob"), processingCraftingJob);

            List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> ingredientsEntries = this.processingCraftingJobsPendingIngredients.get(processingCraftingJob.getId());
            ValueOutput.ValueOutputList pendingEntries = entriesTag.childrenList("pendingIngredientInstanceEntries");
            for (Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredients : ingredientsEntries) {
                ValueOutput pendingEntryTag = pendingEntries.addChild();
                ValueOutput.ValueOutputList pendingIngredientInstances = pendingEntryTag.childrenList("v");
                for (Map.Entry<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredientComponentListEntry : ingredients.entrySet()) {
                    ValueOutput ingredientInstance = pendingIngredientInstances.addChild();

                    IngredientComponent<?, ?> ingredientComponent = ingredientComponentListEntry.getKey();
                    ingredientInstance.putString("ingredientComponent", IngredientComponent.REGISTRY.getKey(ingredientComponent).toString());

                    ValueOutput.ValueOutputList instances = ingredientInstance.childrenList("instances");
                    IIngredientSerializer serializer = ingredientComponent.getSerializer();
                    for (IPrototypedIngredient<?, ?> prototypedIngredient : ingredientComponentListEntry.getValue()) {
                        ValueOutput instance = instances.addChild();
                        serializer.serializeInstance(instance.child("prototype"), prototypedIngredient.getPrototype());
                        instance.store("condition", ExtraCodecs.NBT, serializer.serializeCondition(prototypedIngredient.getCondition()));
                    }
                }
            }

            LongList startTicks = this.processingCraftingJobsStartTicks.get(processingCraftingJob.getId());
            if (startTicks != null) {
                entriesTag.store("pendingIngredientInstanceEntryStartTicks", Codec.LONG.listOf(), startTicks);
            }
        }

        ValueOutput.ValueOutputList pendingCraftingJobs = valueOutput.childrenList("pendingCraftingJobs");
        for (CraftingJob craftingJob : this.pendingCraftingJobs.values()) {
            CraftingJob.serialize(pendingCraftingJobs.addChild(), craftingJob);
        }

        ValueOutput.ValueOutputList finishedCraftingJobs = valueOutput.childrenList("finishedCraftingJobs");
        for (CraftingJob craftingJob : this.finishedCraftingJobs.values()) {
            CraftingJob.serialize(finishedCraftingJobs.addChild(), craftingJob);
        }

        ValueOutput.ValueOutputList targetOverrides = valueOutput.childrenList("targetOverrides");
        for (Map.Entry<IngredientComponent<?, ?>, Direction> entry : this.ingredientComponentTargetOverrides.entrySet()) {
            ValueOutput entryTag = targetOverrides.addChild();
            entryTag.putString("key", entry.getKey().getName().toString());
            entryTag.putInt("value", entry.getValue().ordinal());
        }

        ValueOutput.ValueOutputList nonBlockingJobsRunningAmount = valueOutput.childrenList("nonBlockingJobsRunningAmount");
        for (Int2IntMap.Entry entry : this.nonBlockingJobsRunningAmount.int2IntEntrySet()) {
            ValueOutput entryTag = nonBlockingJobsRunningAmount.addChild();
            entryTag.putInt("key", entry.getIntKey());
            entryTag.putInt("value", entry.getIntValue());
        }

        getRecipeDurationStatistics().serialize(valueOutput.child("recipeDurationStatistics"));
    }

    public void deserialize(ValueInput valueInput) {
        ValueInput.ValueInputList processingCraftingJobs = valueInput.childrenList("processingCraftingJobs").orElseThrow();
        for (ValueInput entryTag : processingCraftingJobs) {
            List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> pendingIngredientInstanceEntries = Lists.newArrayList();
            ValueInput.ValueInputList ingredientsEntries = entryTag.childrenList("pendingIngredientInstanceEntries").orElseThrow();
            for (ValueInput ingredientEntry : ingredientsEntries) {
                ValueInput.ValueInputList pendingIngredientsList = ingredientEntry.childrenList("v").orElseThrow();
                Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredientInstances = Maps.newIdentityHashMap();
                for (ValueInput pendingIngredientTag : pendingIngredientsList) {
                    String componentName = pendingIngredientTag.getString("ingredientComponent").orElseThrow();
                    IngredientComponent<?, ?> ingredientComponent = IngredientComponent.REGISTRY.getValue(Identifier.parse(componentName));
                    if (ingredientComponent == null) {
                        throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
                    }
                    IIngredientSerializer serializer = ingredientComponent.getSerializer();

                    List<IPrototypedIngredient<?, ?>> pendingIngredients = Lists.newArrayList();
                    for (ValueInput instanceTag : pendingIngredientTag.childrenList("instances").orElseThrow()) {
                        Object instance = serializer.deserializeInstance(instanceTag.child("prototype").orElseThrow());
                        Object condition = serializer.deserializeCondition(instanceTag.read("condition", ExtraCodecs.NBT).orElseThrow());
                        pendingIngredients.add(new PrototypedIngredient(ingredientComponent, instance, condition));
                    }

                    pendingIngredientInstances.put(ingredientComponent, pendingIngredients);
                }

                pendingIngredientInstanceEntries.add(pendingIngredientInstances);
            }

            CraftingJob craftingJob = CraftingJob.deserialize(entryTag.child("craftingJob").orElseThrow());

            this.processingCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.processingCraftingJobsPendingIngredients.put(
                    craftingJob.getId(),
                    pendingIngredientInstanceEntries);

            entryTag.read("pendingIngredientInstanceEntryStartTicks", Codec.LONG.listOf())
                    .ifPresent(startTicks -> this.processingCraftingJobsStartTicks.put(
                            craftingJob.getId(),
                            new LongArrayList(startTicks)));

        }

        for (ValueInput craftingJob : valueInput.childrenList("pendingCraftingJobs").orElseThrow()) {
            CraftingJob craftingJobInstance = CraftingJob.deserialize(craftingJob);
            this.pendingCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
            this.allCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
        }

        for (ValueInput craftingJob : valueInput.childrenList("finishedCraftingJobs").orElseThrow()) {
            CraftingJob craftingJobInstance = CraftingJob.deserialize(craftingJob);
            this.finishedCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
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
        for (ValueInput targetOverride : valueInput.childrenList("targetOverrides").orElseThrow()) {
            IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.getValue(Identifier.parse(targetOverride.getString("key").orElseThrow()));
            this.ingredientComponentTargetOverrides.put(component, Direction.values()[targetOverride.getInt("value").orElseThrow()]);
        }

        this.nonBlockingJobsRunningAmount.clear();
        for (ValueInput job : valueInput.childrenList("nonBlockingJobsRunningAmount").orElseThrow()) {
            int craftingJobId = job.getInt("key").orElseThrow();
            int amount = job.getInt("value").orElseThrow();
            this.nonBlockingJobsRunningAmount.put(craftingJobId, amount);
        }

        getRecipeDurationStatistics().deserialize(valueInput.childOrEmpty("recipeDurationStatistics"));
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

    public void fillCraftingJobBufferFromStorage(CraftingJob craftingJob, Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter) {
        if (!craftingJob.getIngredientsStorageBuffer().isEmpty()) {
            throw new IllegalStateException("Re-filling a non-empty crafting job buffer is illegal");
        }
        // Determine the ingredients to extract. We can not reuse the ingredientsStorage value from the crafting job, as this may have been modified due to job splitting.
        // If this job has dependencies, skip reusable ingredients so that they remain available for other jobs.
        // They will be lazily extracted in the update loop once the dependencies have finished.
        boolean skipReusableIngredients = !craftingJob.getDependencyCraftingJobs().isEmpty();
        Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> inputResult = CraftingHelpers.getRecipeInputs(storageGetter, craftingJob.getRecipe(), false, Maps.newIdentityHashMap(), Maps.newIdentityHashMap(), true, craftingJob.getAmount(), skipReusableIngredients);
        IMixedIngredients buffer = new MixedIngredients(inputResult.getLeft());
        craftingJob.setIngredientsStorageBuffer(CraftingHelpers.compressMixedIngredients(buffer));
        craftingJob.setLastMissingIngredients(inputResult.getRight());
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

    /**
     * @param craftingJobId A crafting job id.
     * @return The tick at which the oldest running crafting operation of the given job was started,
     *         or -1 if no operation is running.
     */
    public long getCraftingJobEntryStartTick(int craftingJobId) {
        LongList startTicks = this.processingCraftingJobsStartTicks.get(craftingJobId);
        return startTicks == null || startTicks.isEmpty() ? -1 : startTicks.getLong(0);
    }

    /**
     * Only the time between starting a crafting operation and its outputs coming back in is measured.
     * Recipes that produce their outputs within the tick they are started in, such as regular crafting
     * recipes, therefore measure as taking no time at all, which would estimate whole crafting jobs away.
     *
     * In blocking mode, a single operation is started per update, so an operation occupies this handler
     * for a full update interval, however quickly the recipe itself is done.
     * In non-blocking mode, as many operations are started as the target accepts, so there is no such
     * lower bound, and the given interval is ignored.
     *
     * @param recipe A recipe.
     * @param updateInterval The number of ticks between two updates of this handler.
     * @return The estimated duration in ticks of a single crafting operation of the given recipe,
     *         based on the operations that were performed by this handler before, or -1 if unknown.
     *         This falls back to the average duration over all recipes
     *         when the given recipe itself was not crafted recently.
     */
    public long getEstimatedRecipeDuration(IRecipeDefinition recipe, long updateInterval) {
        long recipeDuration = getRecipeDurationStatistics().getEstimatedDuration(recipe, getCurrentTick());
        if (recipeDuration < 0) {
            return -1;
        }
        return isBlockingJobsMode() ? Math.max(recipeDuration, updateInterval) : recipeDuration;
    }

    /**
     * @return The current game tick.
     */
    protected long getCurrentTick() {
        return CraftingHelpers.getCurrentTick();
    }

    /**
     * Take the duration of a finished crafting operation into account for future estimations.
     * @param recipe The recipe that was crafted.
     * @param durationTicks The number of ticks the crafting operation took.
     */
    protected void reportRecipeDuration(IRecipeDefinition recipe, long durationTicks) {
        getRecipeDurationStatistics().reportDuration(recipe, durationTicks, getCurrentTick());
    }

    /**
     * @return The duration statistics of this handler, which are created lazily,
     *         as their configuration is only available once the mod is fully loaded.
     */
    public RecipeDurationStatistics getRecipeDurationStatistics() {
        if (this.recipeDurationStatistics == null) {
            this.recipeDurationStatistics = createRecipeDurationStatistics();
        }
        return this.recipeDurationStatistics;
    }

    protected RecipeDurationStatistics createRecipeDurationStatistics() {
        return new RecipeDurationStatistics(GeneralConfig.craftingInterfaceRecipeDurationEntries,
                GeneralConfig.craftingInterfaceRecipeDurationMaxAge);
    }

    public void unmarkCraftingJobProcessing(CraftingJob craftingJob) {
        if (this.processingCraftingJobs.remove(craftingJob.getId()) != null) {
            this.processingCraftingJobsPendingIngredients.remove(craftingJob.getId());
            this.processingCraftingJobsStartTicks.remove(craftingJob.getId());
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
            this.processingCraftingJobsStartTicks.remove(craftingJob.getId());

        } else {
            this.processingCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.allCraftingJobs.put(craftingJob.getId(), craftingJob);

            List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> pendingIngredientsEntries = this.processingCraftingJobsPendingIngredients.get(craftingJob.getId());
            if (pendingIngredientsEntries == null) {
                pendingIngredientsEntries = Lists.newArrayList();
                this.processingCraftingJobsPendingIngredients.put(craftingJob.getId(), pendingIngredientsEntries);
            }
            pendingIngredientsEntries.add(pendingIngredients);

            // Remember when this crafting operation started, so that its duration can be measured once it finishes
            LongList startTicks = this.processingCraftingJobsStartTicks.get(craftingJob.getId());
            if (startTicks == null) {
                startTicks = new LongArrayList();
                this.processingCraftingJobsStartTicks.put(craftingJob.getId(), startTicks);
            }
            startTicks.add(getCurrentTick());
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
            ingredientsNetwork.registerInsertPreConsumer(observer);
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
            PendingCraftingJobResultIndexObserver<T, M> observer =
                    (PendingCraftingJobResultIndexObserver<T, M>) ingredientObservers
                            .remove(ingredientComponent);
            ingredientsNetwork.unregisterInsertPreConsumer(observer);
        }
    }

    public void onCraftingJobFinished(CraftingJob craftingJob) {
        this.processingCraftingJobs.remove(craftingJob.getId());
        this.processingCraftingJobsStartTicks.remove(craftingJob.getId());
        this.pendingCraftingJobs.remove(craftingJob.getId());
        this.finishedCraftingJobs.put(craftingJob.getId(), craftingJob);
        this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
    }

    // This does the same as above, just based on crafting job id
    public void markCraftingJobFinished(int craftingJobId) {
        this.processingCraftingJobsPendingIngredients.remove(craftingJobId);
        this.processingCraftingJobsStartTicks.remove(craftingJobId);
        this.processingCraftingJobs.remove(craftingJobId);
        this.pendingCraftingJobs.remove(craftingJobId);

        // Needed so that we remove the job in the next tick
        CraftingJob craftingJob = this.allCraftingJobs.get(craftingJobId);
        this.finishedCraftingJobs.put(craftingJobId, craftingJob);
        craftingJob.setAmount(0);
    }

    public void onCraftingJobEntryFinished(ICraftingNetwork craftingNetwork, int craftingJobId) {
        CraftingJob craftingJob = this.allCraftingJobs.get(craftingJobId);
        craftingJob.setAmount(craftingJob.getAmount() - 1);

        // Measure how long this crafting operation took, so that future jobs for this recipe can be estimated.
        // Operations don't necessarily finish in the order in which they were started,
        // but as they all apply to the same recipe, the oldest one can safely be used.
        LongList startTicks = this.processingCraftingJobsStartTicks.get(craftingJobId);
        if (startTicks != null && !startTicks.isEmpty()) {
            reportRecipeDuration(craftingJob.getRecipe(), getCurrentTick() - startTicks.removeLong(0));
            if (startTicks.isEmpty()) {
                this.processingCraftingJobsStartTicks.remove(craftingJobId);
            }
        }

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
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(network);
            for (CraftingJob finishedCraftingJob : finishedCraftingJobs.values()) {
                if (finishedCraftingJob.getAmount() == 0) {
                    // If the job is fully finished, remove it from the network
                    craftingNetwork.onCraftingJobFinished(finishedCraftingJob);
                    allCraftingJobs.remove(finishedCraftingJob.getId());
                    nonBlockingJobsRunningAmount.remove(finishedCraftingJob.getId());

                    if (!finishedCraftingJob.getIngredientsStorageBuffer().isEmpty()) {
                        CraftingHelpers.insertIngredientsGuaranteed(finishedCraftingJob.getIngredientsStorageBuffer(), CraftingHelpers.getNetworkStorageGetter(network, channel, false), this.resultsSink);
                    }
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
                CraftingJob craftingJob = this.allCraftingJobs.get(craftingJobId); // Could be null, but not sure why: CyclopsMC/IntegratedCrafting#161
                if (runningAmount > 0 && craftingJob != null && runningAmount < craftingJob.getAmount()) {
                    insertLoopNonBlocking(network, channel, targetPos, craftingJob);
                }
            }
        }

        // Only look for a job to start if this handler has room for one, and has something to start.
        // Skipping this block for an idle handler avoids a crafting network lookup for every idle tick.
        if (processingJobs < this.maxProcessingJobs && !this.pendingCraftingJobs.isEmpty()) {
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
                IRecipeDefinition recipe = pendingCraftingJob.getRecipe();
                Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> inputs = CraftingHelpers.getRecipeInputs(
                        CraftingHelpers.getCraftingJobBufferStorageGetter(pendingCraftingJob),
                        recipe, true, Maps.newIdentityHashMap(), Maps.newIdentityHashMap(), true, 1);
                if (inputs.getRight().isEmpty()) { // If we have no missing ingredients
                    if (insertCrafting(targetPos, new MixedIngredients(inputs.getLeft()), recipe, pendingCraftingJob, network, channel, true)) {
                        startingCraftingJob = pendingCraftingJob;
                        startingCraftingJob.setInvalidInputs(false);
                        break;
                    } else {
                        pendingCraftingJob.setInvalidInputs(true);
                    }
                } else {
                    // For the missing ingredients that are reusable,
                    // trigger a crafting job for them if no job is running yet.
                    // This special case is needed because reusable ingredients are usually durability-based,
                    // and may be consumed _during_ a bulk crafting job.
                    for (IngredientComponent<?, ?> component : inputs.getRight().keySet()) {
                        MissingIngredients<?, ?> missingIngredients = inputs.getRight().get(component);
                        for (MissingIngredients.Element<?, ?> element : missingIngredients.getElements()) {
                            if (element.isInputReusable()) {
                                IIngredientComponentStorage storage = CraftingHelpers.getNetworkStorage(network, channel, component, true);
                                for (MissingIngredients.PrototypedWithRequested alternative : element.getAlternatives()) {
                                    // First check if we can extract it from storage.
                                    Object extractedFromStorage = storage.extract(alternative.getRequestedPrototype().getPrototype(), alternative.getRequestedPrototype().getCondition(), false);
                                    if (!((IIngredientMatcher) component.getMatcher()).isEmpty(extractedFromStorage)) {
                                        pendingCraftingJob.addToIngredientsStorageBuffer((IngredientComponent<? super Object, ? extends Object>) component, extractedFromStorage);
                                        break;
                                    }

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
            }

            // Start the crafting job
            if (startingCraftingJob != null) {
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

    protected boolean insertCrafting(PartPos target, IMixedIngredients ingredients, IRecipeDefinition recipe, CraftingJob craftingJob, INetwork network, int channel, boolean simulate) {
        Function<IngredientComponent<?, ?>, PartPos> targetGetter = getTargetGetter(target);
        // First check our crafting overrides
        for (ICraftingProcessOverride craftingProcessOverride : this.craftingProcessOverrides) {
            if (craftingProcessOverride.isApplicable(target)) {
                try {
                    return craftingProcessOverride.craft(targetGetter, ingredients, recipe, this.resultsSink, craftingJob, simulate);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }

        // Fallback to default crafting insertion
        return CraftingHelpers.insertCrafting(targetGetter, ingredients, network, channel, simulate);
    }

    protected void insertLoopNonBlocking(INetwork network, int channel, PartPos targetPos, CraftingJob craftingJob) {
        // If in non-blocking mode, try to push as much as possible into the target
        while (nonBlockingJobsRunningAmount.get(craftingJob.getId()) < craftingJob.getAmount()) {
            IRecipeDefinition recipe = craftingJob.getRecipe();
            IMixedIngredients ingredientsSimulated = CraftingHelpers.getRecipeInputsFromCraftingJobBuffer(craftingJob,
                    recipe, true, 1);
            if (ingredientsSimulated == null || !insertCrafting(targetPos, ingredientsSimulated, recipe, craftingJob, network, channel, true)) {
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
        IRecipeDefinition recipe = startingCraftingJob.getRecipe();
        IMixedIngredients ingredients = CraftingHelpers.getRecipeInputsFromCraftingJobBuffer(startingCraftingJob,
                recipe, false, 1);

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
            if (!insertCrafting(targetPos, ingredients, recipe, startingCraftingJob, network, channel, false)) {
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

    /**
     * This method is called right before a crafting interface's result buffer is flushed to the network.
     * This will first try to pass the instance along to crafting jobs that have pending ingredients.
     * The remaining instance that could not be inserted into any of those crafting jobs is returned.
     * @param instanceWrapper The instance that would be inserted into the network.
     * @param channel The channel.
     * @return The remaining instance that was not consumed by observers.
     * @param <T> The ingredient type.
     * @param <M> The match condition.
     */
    public <T, M> IngredientInstanceWrapper<T, M> beforeFlushIngredientToNetwork(IngredientInstanceWrapper<T, M> instanceWrapper, int channel) {
        PendingCraftingJobResultIndexObserver<T, M> observer = (PendingCraftingJobResultIndexObserver<T, M>) ingredientObservers.get(instanceWrapper.getComponent());
        if (observer != null) {
            IIngredientCollectionMutable<T, M> instances = new IngredientCollectionPrototypeMap<>(instanceWrapper.getComponent());
            instances.add(instanceWrapper.getInstance());
            return observer.addIngredient(instanceWrapper, channel, false);
        }
        return instanceWrapper;
    }
}
