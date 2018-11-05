package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientSerializer;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
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

    private final Map<CraftingJob, Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> processingCraftingJobsPendingIngredients;
    private final List<CraftingJob> pendingCraftingJobs;
    private final Object2IntMap<IngredientComponent<?, ?>> ingredientObserverCounters;
    private final Map<IngredientComponent<?, ?>, IIngredientComponentStorageObservable.IIndexChangeObserver<?, ?>> ingredientObservers;
    private final List<IngredientComponent<?, ?>> observersPendingCreation;
    private final List<IngredientComponent<?, ?>> observersPendingDeletion;
    private final List<CraftingJob> finishedCraftingJobs;

    public CraftingJobHandler(int maxProcessingJobs, Collection<ICraftingProcessOverride> craftingProcessOverrides,
                              ICraftingResultsSink resultsSink) {
        this.maxProcessingJobs = maxProcessingJobs;
        this.resultsSink = resultsSink;
        this.craftingProcessOverrides = craftingProcessOverrides;

        this.pendingCraftingJobs = Lists.newArrayList();
        this.processingCraftingJobsPendingIngredients = Maps.newIdentityHashMap();
        this.ingredientObserverCounters = new Object2IntOpenHashMap<>();
        this.ingredientObservers = Maps.newIdentityHashMap();
        this.observersPendingCreation = Lists.newArrayList();
        this.observersPendingDeletion = Lists.newArrayList();
        this.finishedCraftingJobs = Lists.newArrayList();
    }

    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList processingCraftingJobs = new NBTTagList();
        for (Map.Entry<CraftingJob, Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> entry : this.processingCraftingJobsPendingIngredients.entrySet()) {
            NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setTag("craftingJob", CraftingJob.serialize(entry.getKey()));
            NBTTagList pendingIngredientInstances = new NBTTagList();
            for (Map.Entry<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> ingredientComponentListEntry : entry.getValue().entrySet()) {
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
        for (CraftingJob craftingJob : this.pendingCraftingJobs) {
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

            this.processingCraftingJobsPendingIngredients.put(
                    CraftingJob.deserialize(entryTag.getCompoundTag("craftingJob")),
                    pendingIngredientInstances);

        }

        NBTTagList pendingCraftingJobs = tag.getTagList("pendingCraftingJobs", Constants.NBT.TAG_COMPOUND);
        for (NBTBase craftingJob : pendingCraftingJobs) {
            this.pendingCraftingJobs.add(CraftingJob.deserialize((NBTTagCompound) craftingJob));
        }

        // Add required observers to a list so that they will be created in the next tick
        for (Map.Entry<CraftingJob, Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> entry : this.processingCraftingJobsPendingIngredients.entrySet()) {
            // It's possible that the same component is added multiple times over different jobs,
            // this is because we want to make sure our counters are correct.
            observersPendingCreation.addAll(entry.getValue().keySet());
        }
    }

    public void scheduleCraftingJob(CraftingJob craftingJob) {
        this.pendingCraftingJobs.add(craftingJob);
    }

    public Map<CraftingJob, Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> getProcessingCraftingJobsPendingIngredients() {
        return processingCraftingJobsPendingIngredients;
    }

    public Collection<CraftingJob> getProcessingCraftingJobs() {
        return processingCraftingJobsPendingIngredients.keySet();
    }

    public Collection<CraftingJob> getPendingCraftingJobs() {
        return pendingCraftingJobs;
    }

    public void markCraftingJobProcessing(CraftingJob craftingJob, Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredients) {
        if (this.pendingCraftingJobs.remove(craftingJob)) {
            setCraftingJobProcessingPendingIngredients(craftingJob, pendingIngredients);
        }
    }

    public void setCraftingJobProcessingPendingIngredients(CraftingJob craftingJob,
                                                           Map<IngredientComponent<?, ?>,
                                                                   List<IPrototypedIngredient<?, ?>>> pendingIngredients) {
        if (pendingIngredients.isEmpty()) {
            this.processingCraftingJobsPendingIngredients.remove(craftingJob);
        } else {
            this.processingCraftingJobsPendingIngredients.put(craftingJob, pendingIngredients);
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
        this.finishedCraftingJobs.add(craftingJob);
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
            for (CraftingJob finishedCraftingJob : finishedCraftingJobs) {
                ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetwork(network);
                craftingNetwork.onCraftingJobFinished(finishedCraftingJob);
            }
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
                IMixedIngredients ingredients = CraftingHelpers.getRecipeInputs(network, channel,
                        pendingCraftingJob.getRecipe().getRecipe(), true);
                if (ingredients != null && insertCrafting(targetPos, ingredients, true)) {
                    startingCraftingJob = pendingCraftingJob;
                    break;
                }
            }

            // Start the crafting job
            if (startingCraftingJob != null) {
                // Remove ingredients from network
                IMixedIngredients ingredients = CraftingHelpers.getRecipeInputs(network, channel,
                        startingCraftingJob.getRecipe().getRecipe(), false);

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

}
