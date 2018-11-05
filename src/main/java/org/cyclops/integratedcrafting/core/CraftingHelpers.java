package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IngredientComponentStorageEmpty;
import org.cyclops.cyclopscore.helper.TileHelpers;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.RecursiveCraftingRecipeException;
import org.cyclops.integratedcrafting.api.crafting.UnknownCraftingRecipeException;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkConfig;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helpers related to handling crafting jobs.
 * @author rubensworks
 */
public class CraftingHelpers {

    /**
     * Get the crafting network in the given network.
     * @param network A network.
     * @return The crafting network or null.
     */
    @Nullable
    public static ICraftingNetwork getCraftingNetwork(@Nullable INetwork network) {
        if (network != null) {
            return network.getCapability(CraftingNetworkConfig.CAPABILITY);
        }
        return null;
    }

    /**
     * Get the storage network of the given type in the given network.
     * @param network A network.
     * @param ingredientComponent The ingredient component type of the network.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The storage network or null.
     */
    @Nullable
    public static <T, M> IPositionedAddonsNetworkIngredients<T, M> getIngredientsNetwork(INetwork network,
                                                                                         IngredientComponent<T, M> ingredientComponent) {
        return ingredientComponent
                .getCapability(Capabilities.POSITIONED_ADDONS_NETWORK_INGREDIENTS_HANDLER)
                .getStorage(network);
    }

    /**
     * Get the storage of the given ingredient component type from the network.
     * @param network The network.
     * @param channel A network channel.
     * @param ingredientComponent The ingredient component type of the network.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The storage.
     */
    public static <T, M> IIngredientComponentStorage<T, M> getNetworkStorage(INetwork network, int channel,
                                                                             IngredientComponent<T, M> ingredientComponent,
                                                                             boolean scheduleObservation) {
        IPositionedAddonsNetworkIngredients<T, M> ingredientsNetwork = getIngredientsNetwork(network, ingredientComponent);
        if (ingredientsNetwork != null) {
            if (scheduleObservation) {
                ingredientsNetwork.scheduleObservation();
            }
            return ingredientsNetwork.getChannel(channel);
        }
        return new IngredientComponentStorageEmpty<>(ingredientComponent);
    }

    /**
     * Calculate the required crafting jobs and their dependencies for the given instance in the given network.
     * @param network The target network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to craft.
     * @param matchCondition The match condition of the instance.
     * @param craftMissing If the missing required ingredients should also be crafted.
     * @param identifierGenerator identifierGenerator An ID generator for crafting jobs.
     * @param craftingJobsGraph The target graph where all dependencies will be stored.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The crafting job for the given instance.
     * @throws UnknownCraftingRecipeException If the recipe for a (sub)ingredient is unavailable.
     * @throws RecursiveCraftingRecipeException If an infinite recursive recipe was detected.
     */
    public static <T, M> CraftingJob calculateCraftingJobs(INetwork network, int channel,
                                                           IngredientComponent<T, M> ingredientComponent,
                                                           T instance, M matchCondition, boolean craftMissing,
                                                           IIdentifierGenerator identifierGenerator,
                                                           CraftingJobDependencyGraph craftingJobsGraph)
            throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        ICraftingNetwork craftingNetwork = getCraftingNetwork(network);
        IRecipeIndex recipeIndex = craftingNetwork.getRecipeIndex(channel);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = getNetworkStorageGetter(network, channel);
        return calculateCraftingJobs(recipeIndex, channel, storageGetter, ingredientComponent, instance, matchCondition,
                craftMissing, Maps.newIdentityHashMap(), identifierGenerator, craftingJobsGraph, Sets.newHashSet());
    }

    /**
     * @return An identifier generator for crafting jobs.
     */
    public static IIdentifierGenerator getGlobalCraftingJobIdentifier() {
        return () -> IntegratedCrafting.globalCounters.getNext("craftingJob");
    }

    /**
     * Calculate a crafting job for the given instance.
     *
     * This method is merely an easily-stubbable implementation for the method above,
     * to simplify unit testing.
     *
     * @param recipeIndex The recipe index.
     * @param channel The target channel that will be stored in created crafting jobs.
     * @param storageGetter A callback function to get a storage for the given ingredient component.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to craft.
     * @param matchCondition The match condition of the instance.
     * @param craftMissing If the missing required ingredients should also be crafted.
     * @param simulatedExtractionMemory This map remembers all extracted instances in simulation mode.
     *                                  This is to make sure that instances can not be extracted multiple times
     *                                  when simulating.
     * @param identifierGenerator An ID generator for crafting jobs.
     * @param craftingJobsGraph The target graph where all dependencies will be stored.
     * @param parentDependencies A set of parent recipe dependencies that are pending.
     *                           This is used to check for infinite recursion in recipes.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The crafting job for the given instance.
     * @throws UnknownCraftingRecipeException If the recipe for a (sub)ingredient is unavailable.
     * @throws RecursiveCraftingRecipeException If an infinite recursive recipe was detected.
     */
    protected static <T, M> CraftingJob calculateCraftingJobs(IRecipeIndex recipeIndex, int channel,
                                                              Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
                                                              IngredientComponent<T, M> ingredientComponent,
                                                              T instance, M matchCondition, boolean craftMissing,
                                                              Map<IngredientComponent<?, ?>,
                                                                      IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemory,
                                                              IIdentifierGenerator identifierGenerator,
                                                              CraftingJobDependencyGraph craftingJobsGraph,
                                                              Set<IPrototypedIngredient> parentDependencies)
            throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        // This matching condition makes it so that the recipe output does not have to match with the requested input by quantity.
        M quantifierlessCondition = matcher.withoutCondition(matchCondition,
                ingredientComponent.getPrimaryQuantifier().getMatchCondition());

        // Loop over all available recipes, and return the first valid one.
        Iterator<PrioritizedRecipe> recipes = recipeIndex.getRecipes(ingredientComponent, instance, quantifierlessCondition);
        while (recipes.hasNext()) {
            PrioritizedRecipe recipe = recipes.next();

            // Check if all requirements are met for this recipe, if so return directly (don't schedule yet)
            Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>>> simulation =
                    getRecipeInputs(storageGetter, recipe.getRecipe(), true, simulatedExtractionMemory, true);
            Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> missingIngredients = simulation.getRight();
            boolean validDependencies = true;

            if (!craftMissing && !missingIngredients.isEmpty()) {
                continue;
            }

            // For all missing ingredients, recursively call this method for all missing items, and add as dependencies
            List<CraftingJob> dependencies = Lists.newArrayListWithCapacity(missingIngredients.size());
            // We must be able to find crafting jobs for all dependencies
            for (IngredientComponent<?, ?> dependencyComponent : missingIngredients.keySet()) {
                for (List<IPrototypedIngredient<?, ?>> prototypedAlternatives : missingIngredients.get(dependencyComponent)) {
                    CraftingJob dependency = null;
                    for (IPrototypedIngredient<?, ?> prototypedAlternative : prototypedAlternatives) {
                        // Find at least one prototype that we can craft
                        try {
                            Set<IPrototypedIngredient> childDependencies = Sets.newHashSet(parentDependencies);
                            if (!childDependencies.add(prototypedAlternative)) {
                                throw new RecursiveCraftingRecipeException(prototypedAlternative);
                            }
                            dependency = calculateCraftingJobs(recipeIndex, channel, storageGetter,
                                    (IngredientComponent) dependencyComponent, prototypedAlternative.getPrototype(),
                                    prototypedAlternative.getCondition(), true, simulatedExtractionMemory,
                                    identifierGenerator, craftingJobsGraph, childDependencies);
                            break;
                        } catch (UnknownCraftingRecipeException e) {
                            // Ignore error, and check the next prototype
                        }
                    }

                    // If no valid crafting recipe was found for the current sub-instance, re-throw its error
                    if (dependency == null) {
                        validDependencies = false;
                        break;
                    }

                    // When we reach this point, a valid sub-recipe was found, so add it to our dependencies
                    dependencies.add(dependency);
                }
                // Don't check the other components once we have an invalid dependency.
                if (!validDependencies) {
                    break;
                }
            }

            // If at least one of our dependencies does not have a valid recipe or is not available,
            // go check the next recipe.
            if (!validDependencies) {
                continue;
            }

            // Calculate the quantity for the given instance that the recipe outputs
            long recipeOutputQuantity = recipe.getRecipe().getOutput().getInstances(ingredientComponent)
                    .stream()
                    .filter(i -> matcher.matches(i, instance, quantifierlessCondition))
                    .mapToLong(matcher::getQuantity)
                    .sum();
            // With this number, calculate the amount of required recipe jobs.
            int amount = (int) Math.ceil(((float) matcher.getQuantity(instance)) / (float) recipeOutputQuantity);

            CraftingJob craftingJob = new CraftingJob(identifierGenerator.getNext(), channel, recipe, amount);
            for (CraftingJob dependency : dependencies) {
                craftingJob.addDependency(dependency);
                craftingJobsGraph.addDependency(craftingJob, dependency);
            }
            return craftingJob;
        }

        // No valid recipes were available, so we error
        throw new UnknownCraftingRecipeException(ingredientComponent, instance, matchCondition);
    }

    /**
     * Schedule all crafting jobs in the given dependency graph in the given network.
     * @param craftingNetwork The target crafting network.
     * @param craftingJobDependencyGraph The crafting job dependency graph.
     */
    public static void scheduleCraftingJobs(ICraftingNetwork craftingNetwork,
                                            CraftingJobDependencyGraph craftingJobDependencyGraph) {
        for (CraftingJob craftingJob : craftingJobDependencyGraph.getCraftingJobs()) {
            craftingNetwork.scheduleCraftingJob(craftingJob);
        }
    }

    /**
     * Schedule the given crafting job  in the given network.
     * @param craftingNetwork The target crafting network.
     * @param craftingJob The crafting job to schedule.
     * @return The scheduled crafting job.
     */
    public static CraftingJob scheduleCraftingJob(ICraftingNetwork craftingNetwork,
                                                  CraftingJob craftingJob) {
        craftingNetwork.scheduleCraftingJob(craftingJob);
        return craftingJob;
    }

    /**
     * Schedule a crafting job for the given instance in the given network.
     * @param network The target network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to craft.
     * @param matchCondition The match condition of the instance.
     * @param craftMissing If the missing required ingredients should also be crafted.
     * @param identifierGenerator An ID generator for crafting jobs.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The scheduled crafting job, or null if no recipe was found.
     */
    @Nullable
    public static <T, M> CraftingJob calculateAndScheduleCraftingJob(INetwork network, int channel,
                                                                     IngredientComponent<T, M> ingredientComponent,
                                                                     T instance, M matchCondition,
                                                                     boolean craftMissing,
                                                                     IIdentifierGenerator identifierGenerator) {
        try {
            CraftingJobDependencyGraph dependencyGraph = new CraftingJobDependencyGraph();
            CraftingJob craftingJob = calculateCraftingJobs(network, channel, ingredientComponent, instance,
                    matchCondition, craftMissing, identifierGenerator, dependencyGraph);

            ICraftingNetwork craftingNetwork = getCraftingNetwork(network);
            craftingNetwork.getCraftingJobDependencyGraph().importDependencies(dependencyGraph);

            scheduleCraftingJobs(craftingNetwork, dependencyGraph);

            return craftingJob;
        } catch (UnknownCraftingRecipeException | RecursiveCraftingRecipeException e) {
            return null;
        }
    }

    /**
     * Check if the given network contains the given instance in any of its storages.
     * @param network The target network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to check.
     * @param matchCondition The match condition of the instance.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return If the instance is present in the network.
     */
    public static <T, M> boolean hasStorageInstance(INetwork network, int channel,
                                                    IngredientComponent<T, M> ingredientComponent,
                                                    T instance, M matchCondition) {
        return !ingredientComponent.getMatcher().isEmpty(
                getNetworkStorage(network, channel, ingredientComponent, true)
                        .extract(instance, matchCondition, true));
    }

    /**
     * Check if there is a scheduled crafting job for the given instance.
     * @param craftingNetwork The target crafting network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to check.
     * @param matchCondition The match condition of the instance.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return If the instance has a crafting job.
     */
    public static <T, M> boolean isCrafting(ICraftingNetwork craftingNetwork, int channel,
                                            IngredientComponent<T, M> ingredientComponent,
                                            T instance, M matchCondition) {
        Iterator<CraftingJob> craftingJobs = craftingNetwork.getCraftingJobs(channel, ingredientComponent,
                instance, matchCondition);
        return craftingJobs.hasNext();
    }

    /**
     * Get all required recipe input ingredients from the network for the given ingredient component.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * Note: Make sure that you first call in simulation-mode
     * to see if the ingredients are available.
     * If you immediately call this non-simulated,
     * then there might be a chance that ingredients are lost
     * from the network.
     *
     * @param storage The target storage.
     * @param ingredientComponent The ingredient component to get the ingredients for.
     * @param recipe The recipe to get the inputs from.
     * @param simulate If true, then the ingredients will effectively be removed from the network, not when false.
     * @return A list of slot-based ingredients, or null if no valid inputs could be found.
     */
    @Nullable
    public static <T, M> List<T> getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage,
                                                           IngredientComponent<T, M> ingredientComponent,
                                                           IRecipeDefinition recipe, boolean simulate) {
        return getIngredientRecipeInputs(storage, ingredientComponent, recipe, simulate,
                simulate ? new IngredientCollectionPrototypeMap<>(ingredientComponent) : null, false).getLeft();
    }

    /**
     * Get all required recipe input ingredients from the network for the given ingredient component,
     * and optionally, explicitly calculate the missing ingredients.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * Note: Make sure that you first call in simulation-mode
     * to see if the ingredients are available.
     * If you immediately call this non-simulated,
     * then there might be a chance that ingredients are lost
     * from the network.
     *
     * @param storage The target storage.
     * @param ingredientComponent The ingredient component to get the ingredients for.
     * @param recipe The recipe to get the inputs from.
     * @param simulate If true, then the ingredients will effectively be removed from the network, not when false.
     * @param simulatedExtractionMemory This map remembers all extracted instances in simulation mode.
     *                                  This is to make sure that instances can not be extracted multiple times
     *                                  when simulating.
     * @param collectMissingIngredients If missing ingredients should be collected.
     *                                  If false, then the first returned list may be null
     *                                  if no valid matches can be found,
     *                                  and the second returned list is always null,
     * @return A pair with two lists:
     *           1. A list of available slot-based ingredients.
     *           2. A list with missing ingredients (non-slot-based).
     *              The first list contains a list of ingredients,
     *              whereas the deeper second list contains different prototype-based alternatives
     *              for the ingredient at this position.
     */
    public static <T, M> Pair<List<T>, List<List<IPrototypedIngredient<T, M>>>>
    getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage, IngredientComponent<T, M> ingredientComponent,
                              IRecipeDefinition recipe, boolean simulate,
                              IngredientCollectionPrototypeMap<T, M> simulatedExtractionMemory,
                              boolean collectMissingIngredients) {
        // Quickly return if the storage is empty
        if (storage.getMaxQuantity() == 0) {
            if (collectMissingIngredients) {
                return Pair.of(
                        Lists.newArrayList(Collections.nCopies(recipe.getInputs(ingredientComponent).size(),
                                ingredientComponent.getMatcher().getEmptyInstance())),
                        recipe.getInputs(ingredientComponent));
            } else {
                return Pair.of(null, null);
            }
        }

        // Iterate over all input slots
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        List<List<IPrototypedIngredient<T, M>>> inputAlternativePrototypes = recipe.getInputs(ingredientComponent);
        List<T> inputInstances = Lists.newArrayList();
        List<List<IPrototypedIngredient<T, M>>> missingInstances =
                collectMissingIngredients ? Lists.newArrayList() : null;
        for (List<IPrototypedIngredient<T, M>> inputPrototypes : inputAlternativePrototypes) {
            T inputInstance = null;
            boolean hasInputInstance = false;

            // Iterate over all alternatives for this input slot, and take the first matching ingredient.
            for (IPrototypedIngredient<T, M> inputPrototype : inputPrototypes) {
                // If the prototype is empty, we can skip network extraction
                if (matcher.isEmpty(inputPrototype.getPrototype())) {
                    inputInstance = inputPrototype.getPrototype();
                    hasInputInstance = true;
                    break;
                }

                long memoryQuantity;
                if (simulate && (memoryQuantity = simulatedExtractionMemory
                        .getQuantity(inputPrototype.getPrototype())) > 0) {
                    T newInstance = matcher.withQuantity(inputPrototype.getPrototype(),
                            memoryQuantity + matcher.getQuantity(inputPrototype.getPrototype()));
                    M matchCondition = matcher.withCondition(inputPrototype.getCondition(),
                            ingredientComponent.getPrimaryQuantifier().getMatchCondition());
                    T extracted = storage.extract(newInstance, matchCondition, true);
                    if (!matcher.isEmpty(extracted)) {
                        inputInstance = inputPrototype.getPrototype();
                        hasInputInstance = true;
                        simulatedExtractionMemory.add(inputPrototype.getPrototype());
                        break;
                    }
                } else {
                    T extracted = storage.extract(inputPrototype.getPrototype(), inputPrototype.getCondition(), simulate);
                    if (!matcher.isEmpty(extracted)) {
                        inputInstance = extracted;
                        hasInputInstance = true;
                        if (simulate) {
                            simulatedExtractionMemory.add(extracted);
                        }
                        break;
                    }
                }

                // Remove the simulated quantity from simulatedExtractionMemory if this prototype was not found
                if (simulate) {
                    simulatedExtractionMemory.remove(inputPrototype.getPrototype());
                }
            }

            // If none of the alternatives were found, fail immediately
            if (!hasInputInstance) {
                if (!simulate) {
                    // But first, re-insert all already-extracted instances
                    for (T instance : inputInstances) {
                        T remaining = storage.insert(instance, false);
                        if (!matcher.isEmpty(remaining)) {
                            throw new IllegalStateException("Extraction for a crafting recipe failed" +
                                    "due to inconsistent insertion behaviour by destination in simulation " +
                                    "and non-simulation: " + storage + ". Lost: " + remaining);
                        }
                    }
                }

                if (!collectMissingIngredients) {
                    // This input failed, return immediately
                    return Pair.of(null, null);
                } else {
                    missingInstances.add(inputPrototypes);
                }
            }

            // Otherwise, append it to the list and carry on.
            inputInstances.add(inputInstance);
        }

        return Pair.of(inputInstances, missingInstances);
    }

    /**
     * Get all required recipe input ingredients from the network.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * Note: Make sure that you first call in simulation-mode
     * to see if the ingredients are available.
     * If you immediately call this non-simulated,
     * then there might be a chance that ingredients are lost
     * from the network.
     *
     * @param network The target network.
     * @param channel The target channel.
     * @param recipe The recipe to get the inputs from.
     * @param simulate If true, then the ingredients will effectively be removed from the network, not when false.
     * @return The found ingredients or null.
     */
    @Nullable
    public static IMixedIngredients getRecipeInputs(INetwork network, int channel,
                                                    IRecipeDefinition recipe, boolean simulate) {
        Map<IngredientComponent<?, ?>, List<?>> inputs = getRecipeInputs(getNetworkStorageGetter(network, channel),
                recipe, simulate, Maps.newIdentityHashMap(), false).getLeft();
        return inputs == null ? null : new MixedIngredients(inputs);
    }

    /**
     * Create a callback function for getting a storage for an ingredient component from the given network channel.
     * @param network The target network.
     * @param channel The target channel.
     * @return A callback function for getting a storage for an ingredient component.
     */
    public static Function<IngredientComponent<?, ?>, IIngredientComponentStorage> getNetworkStorageGetter(INetwork network, int channel) {
        return ingredientComponent -> getNetworkStorage(network, channel, ingredientComponent, false);
    }

    /**
     * Get all required recipe input ingredients based on a given storage callback.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * Note: Make sure that you first call in simulation-mode
     * to see if the ingredients are available.
     * If you immediately call this non-simulated,
     * then there might be a chance that ingredients are lost
     * from the network.
     *
     * @param storageGetter A callback function to get a storage for the given ingredient component.
     * @param recipe The recipe to get the inputs from.
     * @param simulate If true, then the ingredients will effectively be removed from the network, not when false.
     * @param simulatedExtractionMemories This map remembers all extracted instances in simulation mode.
     *                                    This is to make sure that instances can not be extracted multiple times
     *                                    when simulating.
     * @param collectMissingIngredients If missing ingredients should be collected.
     *                                  If false, then the first returned mixed ingredients may be null
     *                                  if no valid matches can be found,
     *                                  and the second returned list is always null,
     * @return A pair with two objects:
     *           1. The found ingredients or null.
     *           2. A mapping from ingredient component to a list with missing ingredients (non-slot-based).
     *                The first list contains a list of ingredients,
     *                whereas the deeper second list contains different prototype-based alternatives
     *                for the ingredient at this position.
     */
    @Nullable
    public static Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>>>
    getRecipeInputs(Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter, IRecipeDefinition recipe, boolean simulate,
                    Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemories,
                    boolean collectMissingIngredients) {
        Map<IngredientComponent<?, ?>, List<?>> ingredientsAvailable = Maps.newIdentityHashMap();
        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> ingredientsMissing = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> ingredientComponent : recipe.getInputComponents()) {
            IIngredientComponentStorage storage = storageGetter.apply(ingredientComponent);
            IngredientCollectionPrototypeMap<?, ?> simulatedExtractionMemory = simulatedExtractionMemories.get(ingredientComponent);
            if (simulatedExtractionMemory == null) {
                simulatedExtractionMemory = new IngredientCollectionPrototypeMap<>(ingredientComponent);
                simulatedExtractionMemories.put(ingredientComponent, simulatedExtractionMemory);
            }
            Pair<List<?>, List<List<IPrototypedIngredient<?, ?>>>> subIngredients = getIngredientRecipeInputs(storage,
                    (IngredientComponent) ingredientComponent, recipe, simulate, simulatedExtractionMemory, collectMissingIngredients);
            List<?> subIngredientAvailable = subIngredients.getLeft();
            List<List<IPrototypedIngredient<?, ?>>> subIngredientsMissing = subIngredients.getRight();
            if (subIngredientAvailable == null && !collectMissingIngredients) {
                return Pair.of(null, null);
            } else {
                ingredientsAvailable.put(ingredientComponent, subIngredientAvailable);
                if (collectMissingIngredients && !subIngredientsMissing.isEmpty()) {
                    ingredientsMissing.put(ingredientComponent, subIngredientsMissing);
                }
            }
        }
        return Pair.of(ingredientsAvailable, ingredientsMissing);
    }

    /**
     * Create a list of prototyped ingredients from the instances
     * of the given ingredient component type in the given mixed ingredients.
     *
     * Equal prototypes will be stacked.
     *
     * @param ingredientComponent The ingredient component type.
     * @param mixedIngredients The mixed ingredients.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return A list of prototypes.
     */
    public static <T, M> List<IPrototypedIngredient<T, M>> getCompressedIngredients(IngredientComponent<T, M> ingredientComponent,
                                                                                    IMixedIngredients mixedIngredients) {
        List<IPrototypedIngredient<T, M>> outputs = Lists.newArrayList();

        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        for (T instance : mixedIngredients.getInstances(ingredientComponent)) {
            // Try to stack this instance with an existing prototype
            boolean stacked = false;
            ListIterator<IPrototypedIngredient<T, M>> existingIt = outputs.listIterator();
            while(existingIt.hasNext()) {
                IPrototypedIngredient<T, M> prototypedIngredient = existingIt.next();
                if (matcher.matches(instance, prototypedIngredient.getPrototype(),
                        prototypedIngredient.getCondition())) {
                    T stackedInstance = matcher.withQuantity(prototypedIngredient.getPrototype(),
                            matcher.getQuantity(prototypedIngredient.getPrototype())
                                    + matcher.getQuantity(instance));
                    existingIt.set(new PrototypedIngredient<>(ingredientComponent, stackedInstance,
                            prototypedIngredient.getCondition()));
                    stacked = true;
                    break;
                }
            }

            // If not possible, just append it to the list
            if (!stacked) {
                outputs.add(new PrototypedIngredient<>(ingredientComponent, instance,
                        matcher.getExactMatchNoQuantityCondition()));
            }
        }

        return outputs;
    }

    /**
     * Create a collection of prototypes from the given recipe's outputs.
     *
     * Equal prototypes will be stacked.
     *
     * @param recipe A recipe.
     * @return A map from ingredient component types to their list of prototypes.
     */
    public static Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> getRecipeOutputs(IRecipeDefinition recipe) {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> outputs = Maps.newHashMap();

        IMixedIngredients mixedIngredients = recipe.getOutput();
        for (IngredientComponent ingredientComponent : mixedIngredients.getComponents()) {
            outputs.put(ingredientComponent, getCompressedIngredients(ingredientComponent, mixedIngredients));
        }

        return outputs;
    }

    /**
     * Creates a new recipe outputs object with all ingredient quantities multiplied by the given amount.
     * @param recipeOutputs A recipe objects holder.
     * @param amount An amount to multiply all instances by.
     * @return A new recipe objects holder.
     */
    public static Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> multiplyRecipeOutputs(
            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> recipeOutputs, int amount) {
        if (amount == 1) {
            return recipeOutputs;
        }

        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> newRecipeOutputs = Maps.newIdentityHashMap();
        for (Map.Entry<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> entry : recipeOutputs.entrySet()) {
            IngredientComponent<?, ?> ingredientComponent = entry.getKey();
            IIngredientMatcher matcher = ingredientComponent.getMatcher();
            List<IPrototypedIngredient<?, ?>> prototypes = entry.getValue()
                    .stream()
                    .map(p -> (PrototypedIngredient<?, ?>) new PrototypedIngredient(ingredientComponent,
                            matcher.withQuantity(p.getPrototype(), matcher.getQuantity(p.getPrototype()) * amount),
                            p.getCondition()))
                    .collect(Collectors.toList());
            newRecipeOutputs.put(ingredientComponent, prototypes);
        }
        return newRecipeOutputs;
    }

    /**
     * Insert the ingredients of the given ingredient component type into the target to make it start crafting.
     * @param ingredientComponent The ingredient component type.
     * @param capabilityProvider The target capability provider.
     * @param side The target side.
     * @param ingredients The ingredients to insert.
     * @param simulate If insertion should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return If all instances could be inserted.
     */
    public static <T, M> boolean insertIngredientCrafting(IngredientComponent<T, M> ingredientComponent,
                                                          ICapabilityProvider capabilityProvider,
                                                          @Nullable EnumFacing side,
                                                          IMixedIngredients ingredients, boolean simulate) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        IIngredientComponentStorage<T, M> storage = ingredientComponent.getStorage(capabilityProvider, side);
        List<T> instances = ingredients.getInstances(ingredientComponent);
        for (T instance : instances) {
            T remaining = storage.insert(instance, simulate);
            if (!matcher.isEmpty(remaining)) {
                if (!simulate) {
                    throw new IllegalStateException("Insertion for a crafting recipe failed" +
                            "due to inconsistent insertion behaviour by destination in simulation " +
                            "and non-simulation: " + capabilityProvider + ". Lost: " + instances);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Insert the ingredients of all applicable ingredient component types into the target to make it start crafting.
     * @param target The target position.
     * @param ingredients The ingredients to insert.
     * @param simulate If insertion should be simulated.
     * @return If all instances could be inserted.
     */
    public static boolean insertCrafting(PartPos target, IMixedIngredients ingredients, boolean simulate) {
        EnumFacing side = target.getSide();
        TileEntity tile = TileHelpers.getSafeTile(target.getPos(), TileEntity.class);
        if (tile != null) {
            for (IngredientComponent<?, ?> ingredientComponent : ingredients.getComponents()) {
                if (!insertIngredientCrafting(ingredientComponent, tile, side, ingredients, simulate)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Generates semni-unique IDs.
     */
    public static interface IIdentifierGenerator {
        public int getNext();
    }

}
