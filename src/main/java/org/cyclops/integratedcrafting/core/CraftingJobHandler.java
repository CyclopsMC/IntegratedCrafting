package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Direction;
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
 * @author rubensworks
 */
public class CraftingJobHandler {

    private final int maxProcessingJobs;
    private final ICraftingResultsSink resultsSink;
    private final Collection<ICraftingProcessOverride> craftingProcessOverrides;

    private final Int2ObjectMap<CraftingJob> allCraftingJobs;
    private final Int2ObjectMap<CraftingJob> processingCraftingJobs;
    private final Int2ObjectMap<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> processingCraftingJobsPendingIngredients;
    private final Int2ObjectMap<CraftingJob> pendingCraftingJobs;
    private final Object2IntMap<IngredientComponent<?, ?>> ingredientObserverCounters;
    private final Map<IngredientComponent<?, ?>, IIngredientComponentStorageObservable.IIndexChangeObserver<?, ?>> ingredientObservers;
    private final List<IngredientComponent<?, ?>> observersPendingCreation;
    private final List<IngredientComponent<?, ?>> observersPendingDeletion;
    private final Int2ObjectMap<CraftingJob> finishedCraftingJobs;
    private final Map<IngredientComponent<?, ?>, Direction> ingredientComponentTargetOverrides;

    public CraftingJobHandler(int maxProcessingJobs, Collection<ICraftingProcessOverride> craftingProcessOverrides,
                              ICraftingResultsSink resultsSink) {
        this.maxProcessingJobs = maxProcessingJobs;
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
    }

    public void writeToNBT(CompoundTag tag) {
        ListTag processingCraftingJobs = new ListTag();
        for (CraftingJob processingCraftingJob : this.processingCraftingJobs.values()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("craftingJob", CraftingJob.serialize(processingCraftingJob));

            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredients = this.processingCraftingJobsPendingIngredients.get(processingCraftingJob.getId());
            ListTag pendingIngredientInstances = new ListTag();
            for (Map.Entry<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredientComponentListEntry : ingredients.entrySet()) {
                CompoundTag ingredientInstance = new CompoundTag();

                IngredientComponent<?, ?> ingredientComponent = ingredientComponentListEntry.getKey();
                ingredientInstance.putString("ingredientComponent", ingredientComponent.getRegistryName().toString());

                ListTag instances = new ListTag();
                IIngredientSerializer serializer = ingredientComponent.getSerializer();
                for (IPrototypedIngredient<?, ?> prototypedIngredient : ingredientComponentListEntry.getValue()) {
                    CompoundTag instance = new CompoundTag();
                    instance.put("prototype", serializer.serializeInstance(prototypedIngredient.getPrototype()));
                    instance.put("condition", serializer.serializeCondition(prototypedIngredient.getCondition()));
                    instances.add(instance);
                }
                ingredientInstance.put("instances", instances);

                pendingIngredientInstances.add(ingredientInstance);
            }
            entryTag.put("pendingIngredientInstances", pendingIngredientInstances);
            processingCraftingJobs.add(entryTag);
        }
        tag.put("processingCraftingJobs", processingCraftingJobs);

        ListTag pendingCraftingJobs = new ListTag();
        for (CraftingJob craftingJob : this.pendingCraftingJobs.values()) {
            pendingCraftingJobs.add(CraftingJob.serialize(craftingJob));
        }
        tag.put("pendingCraftingJobs", pendingCraftingJobs);

        CompoundTag targetOverrides = new CompoundTag();
        for (Map.Entry<IngredientComponent<?, ?>, Direction> entry : this.ingredientComponentTargetOverrides.entrySet()) {
            targetOverrides.putInt(entry.getKey().getName().toString(), entry.getValue().ordinal());
        }
        tag.put("targetOverrides", targetOverrides);
    }

    public void readFromNBT(CompoundTag tag) {
        ListTag processingCraftingJobs = tag.getList("processingCraftingJobs", Tag.TAG_COMPOUND);
        for (Tag entry : processingCraftingJobs) {
            CompoundTag entryTag = (CompoundTag) entry;
            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredientInstances = Maps.newIdentityHashMap();
            ListTag pendingIngredientsList = entryTag.getList("pendingIngredientInstances", Tag.TAG_COMPOUND);
            for (Tag pendingIngredient : pendingIngredientsList) {
                CompoundTag pendingIngredientTag = (CompoundTag) pendingIngredient;
                String componentName = pendingIngredientTag.getString("ingredientComponent");
                IngredientComponent<?, ?> ingredientComponent = IngredientComponent.REGISTRY.getValue(new ResourceLocation(componentName));
                if (ingredientComponent == null) {
                    throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
                }
                IIngredientSerializer serializer = ingredientComponent.getSerializer();

                List<IPrototypedIngredient<?, ?>> pendingIngredients = Lists.newArrayList();
                for (Tag instanceTagUnsafe : pendingIngredientTag.getList("instances", Tag.TAG_COMPOUND)) {
                    CompoundTag instanceTag = (CompoundTag) instanceTagUnsafe;
                    Object instance = serializer.deserializeInstance(instanceTag.get("prototype"));
                    Object condition = serializer.deserializeCondition(instanceTag.get("condition"));
                    pendingIngredients.add(new PrototypedIngredient(ingredientComponent, instance, condition));
                }

                pendingIngredientInstances.put(ingredientComponent, pendingIngredients);
            }

            CraftingJob craftingJob = CraftingJob.deserialize(entryTag.getCompound("craftingJob"));

            this.processingCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.processingCraftingJobsPendingIngredients.put(
                    craftingJob.getId(),
                    pendingIngredientInstances);

        }

        ListTag pendingCraftingJobs = tag.getList("pendingCraftingJobs", Tag.TAG_COMPOUND);
        for (Tag craftingJob : pendingCraftingJobs) {
            CraftingJob craftingJobInstance = CraftingJob.deserialize((CompoundTag) craftingJob);
            this.pendingCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
            this.allCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
        }

        // Add required observers to a list so that they will be created in the next tick
        for (Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> value : this.processingCraftingJobsPendingIngredients.values()) {
            // It's possible that the same component is added multiple times over different jobs,
            // this is because we want to make sure our counters are correct.
            observersPendingCreation.addAll(value.keySet());
        }

        this.ingredientComponentTargetOverrides.clear();
        CompoundTag targetOverrides = tag.getCompound("targetOverrides");
        for (String componentName : targetOverrides.getAllKeys()) {
            IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.getValue(new ResourceLocation(componentName));
            this.ingredientComponentTargetOverrides.put(component, Direction.values()[targetOverrides.getInt(componentName)]);
        }
    }

    public boolean canScheduleCraftingJobs() {
        return this.pendingCraftingJobs.size() < GeneralConfig.maxPendingCraftingJobs;
    }

    public void scheduleCraftingJob(CraftingJob craftingJob) {
        this.pendingCraftingJobs.put(craftingJob.getId(), craftingJob);
        this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
    }

    public Int2ObjectMap<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> getProcessingCraftingJobsPendingIngredients() {
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

    public void markCraftingJobProcessing(CraftingJob craftingJob, Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredients) {
        if (this.pendingCraftingJobs.remove(craftingJob.getId()) != null) {
            setCraftingJobProcessingPendingIngredients(craftingJob, pendingIngredients);
        }
    }

    public void unmarkCraftingJobProcessing(CraftingJob craftingJob) {
        if (this.processingCraftingJobs.remove(craftingJob.getId()) != null) {
            this.processingCraftingJobsPendingIngredients.remove(craftingJob.getId());
            this.pendingCraftingJobs.put(craftingJob.getId(), craftingJob);
        }
    }

    public void setCraftingJobProcessingPendingIngredients(CraftingJob craftingJob,
                                                           Map<IngredientComponent<?, ?>,
                                                                   List<IPrototypedIngredient<?, ?>>> pendingIngredients) {
        if (pendingIngredients.isEmpty()) {
            this.processingCraftingJobs.remove(craftingJob.getId());
            this.processingCraftingJobsPendingIngredients.remove(craftingJob.getId());
            this.allCraftingJobs.remove(craftingJob.getId());
        } else {
            this.processingCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.processingCraftingJobsPendingIngredients.put(craftingJob.getId(), pendingIngredients);
            this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
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
            PendingCraftingJobResultIndexObserver<T, M> observer = new PendingCraftingJobResultIndexObserver<>(ingredientComponent, this);
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
        craftingJob.setAmount(1);
    }

    public void reRegisterObservers(INetwork network) {
        for (Map.Entry<IngredientComponent<?, ?>, IIngredientComponentStorageObservable.IIndexChangeObserver<?, ?>> entry : ingredientObservers.entrySet()) {
            IPositionedAddonsNetworkIngredients ingredientsNetwork = CraftingHelpers
                    .getIngredientsNetworkChecked(network, entry.getKey());
            ingredientsNetwork.addObserver(entry.getValue());
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
                if (finishedCraftingJob.getAmount() == 1) {
                    // If only a single amount for the job was remaining, remove it from the network
                    ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(network);
                    craftingNetwork.onCraftingJobFinished(finishedCraftingJob);
                    allCraftingJobs.remove(finishedCraftingJob.getId());
                } else {
                    // If more than one amount was remaining, decrement it and re-add it to the pending jobs list
                    finishedCraftingJob.setAmount(finishedCraftingJob.getAmount() - 1);
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

        if (processingJobs < this.maxProcessingJobs) {
            // Handle crafting jobs
            CraftingJob startingCraftingJob = null;
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(network);
            CraftingJobDependencyGraph dependencyGraph = craftingNetwork.getCraftingJobDependencyGraph();
            for (CraftingJob pendingCraftingJob : getPendingCraftingJobs()) {
                // Make sure that this crafting job has no incomplete dependency jobs
                if (dependencyGraph.hasDependencies(pendingCraftingJob)) {
                    continue;
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

                // Remove ingredients from network
                IMixedIngredients ingredients = CraftingHelpers.getRecipeInputs(network, startingCraftingJob.getChannel(),
                        startingCraftingJob.getRecipe(), false, 1);

                // This may not be null, error if it is null!
                if (ingredients != null) {
                    // Update state with expected outputs
                    markCraftingJobProcessing(startingCraftingJob,
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
                    }
                } else {
                    IntegratedCrafting.clog(Level.WARN, "Failed to extract ingredients for crafting job " + startingCraftingJob.getId());
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
