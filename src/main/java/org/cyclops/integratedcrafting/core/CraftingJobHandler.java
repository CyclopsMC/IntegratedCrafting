package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientSerializer;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    }

    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList processingCraftingJobs = new NBTTagList();
        for (CraftingJob processingCraftingJob : this.processingCraftingJobs.values()) {
            NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setTag("craftingJob", CraftingJob.serialize(processingCraftingJob));

            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredients = this.processingCraftingJobsPendingIngredients.get(processingCraftingJob.getId());
            NBTTagList pendingIngredientInstances = new NBTTagList();
            for (Map.Entry<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredientComponentListEntry : ingredients.entrySet()) {
                NBTTagCompound ingredientInstance = new NBTTagCompound();

                IngredientComponent<?, ?> ingredientComponent = ingredientComponentListEntry.getKey();
                ingredientInstance.setString("ingredientComponent", ingredientComponent.getRegistryName().toString());

                NBTTagList instances = new NBTTagList();
                IIngredientSerializer serializer = ingredientComponent.getSerializer();
                for (IPrototypedIngredient<?, ?> prototypedIngredient : ingredientComponentListEntry.getValue()) {
                    NBTTagCompound instance = new NBTTagCompound();
                    instance.setTag("prototype", serializer.serializeInstance(prototypedIngredient.getPrototype()));
                    instance.setTag("condition", serializer.serializeCondition(prototypedIngredient.getCondition()));
                    instances.appendTag(instance);
                }
                ingredientInstance.setTag("instances", instances);

                pendingIngredientInstances.appendTag(ingredientInstance);
            }
            entryTag.setTag("pendingIngredientInstances", pendingIngredientInstances);
            processingCraftingJobs.appendTag(entryTag);
        }
        tag.setTag("processingCraftingJobs", processingCraftingJobs);

        NBTTagList pendingCraftingJobs = new NBTTagList();
        for (CraftingJob craftingJob : this.pendingCraftingJobs.values()) {
            pendingCraftingJobs.appendTag(CraftingJob.serialize(craftingJob));
        }
        tag.setTag("pendingCraftingJobs", pendingCraftingJobs);
    }

    public void readFromNBT(NBTTagCompound tag) {
        NBTTagList processingCraftingJobs = tag.getTagList("processingCraftingJobs", Constants.NBT.TAG_COMPOUND);
        for (NBTBase entry : processingCraftingJobs) {
            NBTTagCompound entryTag = (NBTTagCompound) entry;
            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredientInstances = Maps.newIdentityHashMap();
            NBTTagList pendingIngredientsList = entryTag.getTagList("pendingIngredientInstances", Constants.NBT.TAG_COMPOUND);
            for (NBTBase pendingIngredient : pendingIngredientsList) {
                NBTTagCompound pendingIngredientTag = (NBTTagCompound) pendingIngredient;
                String componentName = pendingIngredientTag.getString("ingredientComponent");
                IngredientComponent<?, ?> ingredientComponent = IngredientComponent.REGISTRY.getValue(new ResourceLocation(componentName));
                if (ingredientComponent == null) {
                    throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
                }
                IIngredientSerializer serializer = ingredientComponent.getSerializer();

                List<IPrototypedIngredient<?, ?>> pendingIngredients = Lists.newArrayList();
                for (NBTBase instanceTagUnsafe : pendingIngredientTag.getTagList("instances", Constants.NBT.TAG_COMPOUND)) {
                    NBTTagCompound instanceTag = (NBTTagCompound) instanceTagUnsafe;
                    Object instance = serializer.deserializeInstance(instanceTag.getTag("prototype"));
                    Object condition = serializer.deserializeCondition(instanceTag.getTag("condition"));
                    pendingIngredients.add(new PrototypedIngredient(ingredientComponent, instance, condition));
                }

                pendingIngredientInstances.put(ingredientComponent, pendingIngredients);
            }

            CraftingJob craftingJob = CraftingJob.deserialize(entryTag.getCompoundTag("craftingJob"));

            this.processingCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
            this.processingCraftingJobsPendingIngredients.put(
                    craftingJob.getId(),
                    pendingIngredientInstances);

        }

        NBTTagList pendingCraftingJobs = tag.getTagList("pendingCraftingJobs", Constants.NBT.TAG_COMPOUND);
        for (NBTBase craftingJob : pendingCraftingJobs) {
            CraftingJob craftingJobInstance = CraftingJob.deserialize((NBTTagCompound) craftingJob);
            this.pendingCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
            this.allCraftingJobs.put(craftingJobInstance.getId(), craftingJobInstance);
        }

        // Add required observers to a list so that they will be created in the next tick
        for (Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> value : this.processingCraftingJobsPendingIngredients.values()) {
            // It's possible that the same component is added multiple times over different jobs,
            // this is because we want to make sure our counters are correct.
            observersPendingCreation.addAll(value.keySet());
        }
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
                    .getIngredientsNetwork(network, ingredientComponent);
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
                    .getIngredientsNetwork(network, ingredientComponent);
            IIngredientComponentStorageObservable.IIndexChangeObserver<T, M> observer =
                    (IIngredientComponentStorageObservable.IIndexChangeObserver<T, M>) ingredientObservers
                            .remove(ingredientComponent);
            ingredientsNetwork.removeObserver(observer);
        }
    }

    public void onCraftingJobFinished(CraftingJob craftingJob) {
        this.finishedCraftingJobs.put(craftingJob.getId(), craftingJob);
        this.allCraftingJobs.put(craftingJob.getId(), craftingJob);
    }

    public void reRegisterObservers(INetwork network) {
        for (Map.Entry<IngredientComponent<?, ?>, IIngredientComponentStorageObservable.IIndexChangeObserver<?, ?>> entry : ingredientObservers.entrySet()) {
            IPositionedAddonsNetworkIngredients ingredientsNetwork = CraftingHelpers
                    .getIngredientsNetwork(network, entry.getKey());
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
                    ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetwork(network);
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
                IPositionedAddonsNetworkIngredients<?, ?> ingredientsNetwork = CraftingHelpers.getIngredientsNetwork(network, ingredientComponent);
                ingredientsNetwork.scheduleObservation();
            }
        }

        if (processingJobs < this.maxProcessingJobs) {
            // Handle crafting jobs
            CraftingJob startingCraftingJob = null;
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetwork(network);
            CraftingJobDependencyGraph dependencyGraph = craftingNetwork.getCraftingJobDependencyGraph();
            for (CraftingJob pendingCraftingJob : getPendingCraftingJobs()) {
                // Make sure that this crafting job has no incomplete dependency jobs
                if (dependencyGraph.hasDependencies(pendingCraftingJob)) {
                    continue;
                }

                // Check if pendingCraftingJob can start and set as startingCraftingJob
                // This requires checking the available ingredients AND if the crafting handler can accept it.
                Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> inputs = CraftingHelpers.getRecipeInputs(
                        CraftingHelpers.getNetworkStorageGetter(network, channel),
                        pendingCraftingJob.getRecipe().getRecipe(), true, Maps.newIdentityHashMap(), true, 1);
                if (inputs.getLeft() != null && !inputs.getLeft().isEmpty()
                        && insertCrafting(targetPos, new MixedIngredients(inputs.getLeft()), true)) {
                    startingCraftingJob = pendingCraftingJob;
                    pendingCraftingJob.setLastMissingIngredients(Maps.newIdentityHashMap());
                    break;
                } else {
                    pendingCraftingJob.setLastMissingIngredients(inputs.getRight());
                }
            }

            // Start the crafting job
            if (startingCraftingJob != null) {
                // Remove ingredients from network
                IMixedIngredients ingredients = CraftingHelpers.getRecipeInputs(network, channel,
                        startingCraftingJob.getRecipe().getRecipe(), false, 1);

                // Update state with expected outputs
                markCraftingJobProcessing(startingCraftingJob,
                        CraftingHelpers.getRecipeOutputs(startingCraftingJob.getRecipe().getRecipe()));

                // Push the ingredients to the crafting interface
                insertCrafting(targetPos, ingredients, false);

                // Register listeners for pending ingredients
                for (IngredientComponent<?, ?> component : ingredients.getComponents()) {
                    registerIngredientObserver(component, network);
                }

            }
        }
    }

    protected boolean insertCrafting(PartPos target, IMixedIngredients ingredients, boolean simulate) {
        // First check our crafting overrides
        for (ICraftingProcessOverride craftingProcessOverride : this.craftingProcessOverrides) {
            if (craftingProcessOverride.isApplicable(target)) {
                return craftingProcessOverride.craft(target, ingredients, this.resultsSink, simulate);
            }
        }

        // Fallback to default crafting insertion
        return CraftingHelpers.insertCrafting(target, ingredients, simulate);
    }

    public CraftingJobStatus getCraftingJobStatus(ICraftingNetwork network, int channel, int craftingJobId) {
        if (pendingCraftingJobs.containsKey(craftingJobId)) {
            CraftingJobDependencyGraph dependencyGraph = network.getCraftingJobDependencyGraph();
            if (dependencyGraph.hasDependencies(craftingJobId)) {
                return CraftingJobStatus.PENDING_DEPENDENCIES;
            } else {
                return CraftingJobStatus.PENDING_INGREDIENTS;
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
}
