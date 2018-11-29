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
import org.cyclops.integratedcrafting.api.crafting.FailedCraftingRecipeException;
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
     * @param scheduleObservation If an observation inside the ingredients network should be scheduled.
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
     * @param collectMissingRecipes If the missing recipes should be collected inside
     *                              {@link UnknownCraftingRecipeException}.
     *                              This may slow down calculation for deeply nested recipe graphs.
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
                                                           CraftingJobDependencyGraph craftingJobsGraph,
                                                           boolean collectMissingRecipes)
            throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        ICraftingNetwork craftingNetwork = getCraftingNetwork(network);
        IRecipeIndex recipeIndex = craftingNetwork.getRecipeIndex(channel);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = getNetworkStorageGetter(network, channel);
        return calculateCraftingJobs(recipeIndex, channel, storageGetter, ingredientComponent, instance, matchCondition,
                craftMissing, Maps.newIdentityHashMap(), identifierGenerator, craftingJobsGraph, Sets.newHashSet(),
                collectMissingRecipes);
    }

    /**
     * Calculate the required crafting jobs and their dependencies for the given instance in the given network.
     * @param network The target network.
     * @param channel The target channel.
     * @param recipe The recipe to calculate a job for.
     * @param amount The amount of times the recipe should be crafted.
     * @param craftMissing If the missing required ingredients should also be crafted.
     * @param identifierGenerator identifierGenerator An ID generator for crafting jobs.
     * @param craftingJobsGraph The target graph where all dependencies will be stored.
     * @param collectMissingRecipes If the missing recipes should be collected inside
     *                              {@link FailedCraftingRecipeException}.
     *                              This may slow down calculation for deeply nested recipe graphs.
     * @return The crafting job for the given instance.
     * @throws FailedCraftingRecipeException If the recipe could not be crafted due to missing sub-dependencies.
     * @throws RecursiveCraftingRecipeException If an infinite recursive recipe was detected.
     */
    public static CraftingJob calculateCraftingJobs(INetwork network, int channel,
                                                    PrioritizedRecipe recipe, int amount, boolean craftMissing,
                                                    IIdentifierGenerator identifierGenerator,
                                                    CraftingJobDependencyGraph craftingJobsGraph,
                                                    boolean collectMissingRecipes)
            throws FailedCraftingRecipeException, RecursiveCraftingRecipeException {
        ICraftingNetwork craftingNetwork = getCraftingNetwork(network);
        IRecipeIndex recipeIndex = craftingNetwork.getRecipeIndex(channel);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = getNetworkStorageGetter(network, channel);
        Pair<CraftingJob, List<UnknownCraftingRecipeException>> result = calculateCraftingJobs(recipeIndex, channel, storageGetter, recipe, amount,
                craftMissing, Maps.newIdentityHashMap(), identifierGenerator, craftingJobsGraph, Sets.newHashSet(),
                collectMissingRecipes);
        if (result.getLeft() == null) {
            throw new FailedCraftingRecipeException(recipe, amount, result.getRight());
        } else {
            return result.getLeft();
        }
    }

    /**
     * @return An identifier generator for crafting jobs.
     */
    public static IIdentifierGenerator getGlobalCraftingJobIdentifier() {
        return () -> IntegratedCrafting.globalCounters.getNext("craftingJob");
    }

    /**
     * Calculate the effective quantity for the given instance in the output of the given recipe.
     * @param recipe A recipe.
     * @param ingredientComponent The ingredient component.
     * @param instance An instance.
     * @param matchCondition A match condition.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The effective quantity.
     */
    public static <T, M> long getOutputQuantityForRecipe(IRecipeDefinition recipe,
                                                         IngredientComponent<T, M> ingredientComponent,
                                                         T instance, M matchCondition) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        return recipe.getOutput().getInstances(ingredientComponent)
                .stream()
                .filter(i -> matcher.matches(i, instance, matchCondition))
                .mapToLong(matcher::getQuantity)
                .sum();
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
     * @param collectMissingRecipes If the missing recipes should be collected inside
     *                              {@link UnknownCraftingRecipeException}.
     *                              This may slow down calculation for deeply nested recipe graphs.
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
                                                              Set<IPrototypedIngredient> parentDependencies,
                                                              boolean collectMissingRecipes)
            throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        // This matching condition makes it so that the recipe output does not have to match with the requested input by quantity.
        M quantifierlessCondition = matcher.withoutCondition(matchCondition,
                ingredientComponent.getPrimaryQuantifier().getMatchCondition());
        long instanceQuantity = matcher.getQuantity(instance);

        // Loop over all available recipes, and return the first valid one.
        Iterator<PrioritizedRecipe> recipes = recipeIndex.getRecipes(ingredientComponent, instance, quantifierlessCondition);
        List<UnknownCraftingRecipeException> firstMissingDependencies = Lists.newArrayList();
        while (recipes.hasNext()) {
            PrioritizedRecipe recipe = recipes.next();

            // Calculate the quantity for the given instance that the recipe outputs
            long recipeOutputQuantity = getOutputQuantityForRecipe(recipe.getRecipe(), ingredientComponent, instance, quantifierlessCondition);
            // Based on the quantity of the recipe output, calculate the amount of required recipe jobs.
            int amount = (int) Math.ceil(((float) instanceQuantity) / (float) recipeOutputQuantity);

            // Calculate jobs for the given recipe
            Pair<CraftingJob, List<UnknownCraftingRecipeException>> result = calculateCraftingJobs(recipeIndex, channel,
                    storageGetter, recipe, amount, craftMissing,
                    simulatedExtractionMemory, identifierGenerator, craftingJobsGraph, parentDependencies,
                    collectMissingRecipes && firstMissingDependencies.isEmpty());
            if (result.getLeft() == null) {
                firstMissingDependencies = result.getRight();
            } else {
                return result.getLeft();
            }
        }

        // No valid recipes were available, so we error or collect the missing instance.
        throw new UnknownCraftingRecipeException(new PrototypedIngredient<>(ingredientComponent, instance, matchCondition),
                matcher.getQuantity(instance), firstMissingDependencies);
    }

    /**
     * Calculate a crafting job for the given recipe.
     *
     * This method is merely an easily-stubbable implementation for the method above,
     * to simplify unit testing.
     *
     * @param recipeIndex The recipe index.
     * @param channel The target channel that will be stored in created crafting jobs.
     * @param storageGetter A callback function to get a storage for the given ingredient component.
     * @param recipe The recipe to calculate a job for.
     * @param amount The amount of times the recipe should be crafted.
     * @param craftMissing If the missing required ingredients should also be crafted.
     * @param simulatedExtractionMemory This map remembers all extracted instances in simulation mode.
     *                                  This is to make sure that instances can not be extracted multiple times
     *                                  when simulating.
     * @param identifierGenerator An ID generator for crafting jobs.
     * @param craftingJobsGraph The target graph where all dependencies will be stored.
     * @param parentDependencies A set of parent recipe dependencies that are pending.
     *                           This is used to check for infinite recursion in recipes.
     * @param collectMissingRecipes If the missing recipes should be collected inside
     *                              {@link UnknownCraftingRecipeException}.
     *                              This may slow down calculation for deeply nested recipe graphs.
     * @return The crafting job for the given instance.
     * @throws RecursiveCraftingRecipeException If an infinite recursive recipe was detected.
     */
    protected static Pair<CraftingJob, List<UnknownCraftingRecipeException>> calculateCraftingJobs(
            IRecipeIndex recipeIndex, int channel,
            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
            PrioritizedRecipe recipe, int amount, boolean craftMissing,
            Map<IngredientComponent<?, ?>,
                    IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemory,
            IIdentifierGenerator identifierGenerator,
            CraftingJobDependencyGraph craftingJobsGraph,
            Set<IPrototypedIngredient> parentDependencies,
            boolean collectMissingRecipes)
            throws RecursiveCraftingRecipeException {
        List<UnknownCraftingRecipeException> missingDependencies = Lists.newArrayList();

        // Check if all requirements are met for this recipe, if so return directly (don't schedule yet)
        Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> simulation =
                getRecipeInputs(storageGetter, recipe.getRecipe(), true, simulatedExtractionMemory,
                        true, amount);
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> missingIngredients = simulation.getRight();
        if (!craftMissing && !missingIngredients.isEmpty()) {
            if (collectMissingRecipes) {
                // Collect missing ingredients as missing recipes when we don't want to craft sub-components,
                // but they are missing.
                for (Map.Entry<IngredientComponent<?, ?>, MissingIngredients<?, ?>> entry : missingIngredients.entrySet()) {
                    for (MissingIngredients.Element<?, ?> element : entry.getValue().getElements()) {
                        MissingIngredients.PrototypedWithRequested<?, ?> alternative = element.getAlternatives().get(0);
                        missingDependencies.add(new UnknownCraftingRecipeException(
                                alternative.getRequestedPrototype(), alternative.getQuantityMissing(), Collections.emptyList()));
                    }
                }
            }
            return Pair.of(null, missingDependencies);
        }

        // For all missing ingredients, recursively call this method for all missing items, and add as dependencies
        // We store dependencies as a mapping from recipe to job,
        // so that only one job exist per unique recipe,
        // so that a job amount can be incremented once another equal recipe is found.
        Map<IRecipeDefinition, CraftingJob> dependencies = Maps.newHashMapWithExpectedSize(missingIngredients.size());
        Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> dependenciesOutputSurplus = Maps.newIdentityHashMap();
        // We must be able to find crafting jobs for all dependencies
        for (IngredientComponent dependencyComponent : missingIngredients.keySet()) {
            try {
                List<UnknownCraftingRecipeException> missingSubDependencies = calculateCraftingJobDependencyComponent(
                        dependencyComponent, dependenciesOutputSurplus, missingIngredients.get(dependencyComponent), parentDependencies,
                        dependencies, recipeIndex, channel, storageGetter, simulatedExtractionMemory,
                        identifierGenerator, craftingJobsGraph, collectMissingRecipes);
                // Don't check the other components once we have an invalid dependency.
                if (!missingSubDependencies.isEmpty()) {
                    missingDependencies.addAll(missingSubDependencies);
                    if (!collectMissingRecipes) {
                        break;
                    }
                }
            } catch (RecursiveCraftingRecipeException e) {
                e.addRecipe(recipe);
                throw e;
            }
        }

        // If at least one of our dependencies does not have a valid recipe or is not available,
        // go check the next recipe.
        if (!missingDependencies.isEmpty()) {
            return Pair.of(null, missingDependencies);
        }

        CraftingJob craftingJob = new CraftingJob(identifierGenerator.getNext(), channel, recipe, amount, new MixedIngredients(simulation.getLeft()));
        for (CraftingJob dependency : dependencies.values()) {
            craftingJob.addDependency(dependency);
            craftingJobsGraph.addDependency(craftingJob, dependency);
        }
        return Pair.of(craftingJob, null);
    }

    // Helper function for calculateCraftingJobs, returns a list of non-craftable ingredients.
    protected static <T, M> List<UnknownCraftingRecipeException> calculateCraftingJobDependencyComponent(
            IngredientComponent<T, M> dependencyComponent,
            Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> dependenciesOutputSurplus,
            MissingIngredients<T, M> missingIngredients,
            Set<IPrototypedIngredient> parentDependencies,
            Map<IRecipeDefinition, CraftingJob> dependencies,
            IRecipeIndex recipeIndex,
            int channel,
            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
            Map<IngredientComponent<?, ?>,
                    IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemory,
            IIdentifierGenerator identifierGenerator,
            CraftingJobDependencyGraph craftingJobsGraph,
            boolean collectMissingRecipes)
            throws RecursiveCraftingRecipeException {
        IIngredientMatcher<T, M> dependencyMatcher = dependencyComponent.getMatcher();
        List<UnknownCraftingRecipeException> missingDependencies = Lists.newArrayList();
        for (MissingIngredients.Element<T, M> missingElement : missingIngredients.getElements()) {
            CraftingJob dependency = null;
            boolean skipDependency = false;
            UnknownCraftingRecipeException firstError = null;
            // Loop over all prototype alternatives, at least one has to match.
            for (MissingIngredients.PrototypedWithRequested<T, M> prototypedAlternative : missingElement.getAlternatives()) {
                IPrototypedIngredient<T, M> prototype = prototypedAlternative.getRequestedPrototype();
                // First check if we can grab it from previous surplus
                IngredientCollectionPrototypeMap<T, M> dependencyComponentSurplusOld = (IngredientCollectionPrototypeMap<T, M>) dependenciesOutputSurplus.get(dependencyComponent);
                IngredientCollectionPrototypeMap<T, M> dependencyComponentSurplus = null;
                if (dependencyComponentSurplusOld != null) {
                    // First create a copy of the given surplus store,
                    // and only once we see that the prototype is valid,
                    // save this copy again.
                    // This is because if this prototype is invalid,
                    // then we don't want these invalid surpluses.
                    dependencyComponentSurplus = new IngredientCollectionPrototypeMap<>(dependencyComponentSurplusOld.getComponent());
                    dependencyComponentSurplus.addAll(dependencyComponentSurplusOld);

                    long remainingQuantity = dependencyMatcher.getQuantity(prototype.getPrototype());
                    IIngredientMatcher<T, M> prototypeMatcher = prototype.getComponent().getMatcher();
                    // Check all instances in the surplus that match with the given prototype
                    // For each match, we subtract its quantity from the required quantity.
                    Iterator<T> surplusIt = dependencyComponentSurplus.iterator(prototype.getPrototype(),
                            prototypeMatcher.withoutCondition(prototype.getCondition(), prototype.getComponent().getPrimaryQuantifier().getMatchCondition()));
                    boolean updatedRemainingQuantity = false;
                    while (remainingQuantity > 0 && surplusIt.hasNext()) {
                        updatedRemainingQuantity = true;
                        T matchingInstance = surplusIt.next();
                        long matchingInstanceQuantity = dependencyMatcher.getQuantity(matchingInstance);
                        if (matchingInstanceQuantity <= remainingQuantity) {
                            // This whole surplus instance can be consumed
                            remainingQuantity -= matchingInstanceQuantity;
                            surplusIt.remove();
                        } else {
                            // Only part of this surplus instance can be consumed.
                            matchingInstanceQuantity -= remainingQuantity;
                            remainingQuantity = 0;
                            surplusIt.remove();
                            dependencyComponentSurplus.setQuantity(matchingInstance, matchingInstanceQuantity);
                        }
                    }
                    if (updatedRemainingQuantity) {
                        if (remainingQuantity == 0) {
                            // The prototype is valid,
                            // so we can finally store our temporary surplus
                            dependenciesOutputSurplus.put(dependencyComponent, dependencyComponentSurplus);

                            // Nothing has to be crafted anymore, jump to next dependency
                            skipDependency = true;
                            break;
                        } else {
                            // Partial availability, other part needs to be crafted still.
                            prototype = new PrototypedIngredient<>(dependencyComponent,
                                    dependencyMatcher.withQuantity(prototype.getPrototype(), remainingQuantity),
                                    prototype.getCondition());
                        }
                    }
                }

                // Try to craft the given prototype
                try {
                    Set<IPrototypedIngredient> childDependencies = Sets.newHashSet(parentDependencies);
                    if (!childDependencies.add(prototype)) {
                        throw new RecursiveCraftingRecipeException(prototype);
                    }
                    dependency = calculateCraftingJobs(recipeIndex, channel, storageGetter,
                            dependencyComponent, prototype.getPrototype(),
                            prototype.getCondition(), true, simulatedExtractionMemory,
                            identifierGenerator, craftingJobsGraph, childDependencies, collectMissingRecipes);

                    // The prototype is valid,
                    // so we can finally store our temporary surplus
                    if (dependencyComponentSurplus != null) {
                        dependenciesOutputSurplus.put(dependencyComponent, dependencyComponentSurplus);
                    }

                    // Add the auxiliary recipe outputs that are not requested to the surplus
                    Object dependencyQuantifierlessCondition = dependencyMatcher.withoutCondition(prototype.getCondition(),
                            dependencyComponent.getPrimaryQuantifier().getMatchCondition());
                    long requestedQuantity = dependencyMatcher.getQuantity(prototype.getPrototype());
                    for (IngredientComponent outputComponent : dependency.getRecipe().getRecipe().getOutput().getComponents()) {
                        IngredientCollectionPrototypeMap<?, ?> componentSurplus = dependenciesOutputSurplus.get(outputComponent);
                        if (componentSurplus == null) {
                            componentSurplus = new IngredientCollectionPrototypeMap<>(outputComponent);
                            dependenciesOutputSurplus.put(outputComponent, componentSurplus);
                        }
                        addRemainderAsSurplusForComponent(outputComponent, dependency.getRecipe().getRecipe().getOutput().getInstances(outputComponent), componentSurplus,
                                (IngredientComponent) prototype.getComponent(), prototype.getPrototype(), dependencyQuantifierlessCondition,
                                requestedQuantity);
                    }

                    break;
                } catch (UnknownCraftingRecipeException e) {
                    // Save the first error, and check the next prototype
                    if (firstError == null) {
                        // Modify the error so that the correct missing quantity is stored
                        firstError = new UnknownCraftingRecipeException(
                                e.getIngredient(), prototypedAlternative.getQuantityMissing(), e.getMissingChildRecipes());
                    }
                }
            }

            // Check if this dependency can be skipped
            if (skipDependency) {
                continue;
            }

            // If no valid crafting recipe was found for the current sub-instance, re-throw its error
            if (dependency == null) {
                missingDependencies.add(firstError);
                if (collectMissingRecipes) {
                    continue;
                } else {
                    break;
                }
            }

            // When we reach this point, a valid sub-recipe was found, so add it to our dependencies
            // If the recipe was already present at this level, just increment the amount of the existing job.
            CraftingJob existingJob = dependencies.get(dependency.getRecipe().getRecipe());
            if (existingJob == null) {
                dependencies.put(dependency.getRecipe().getRecipe(), dependency);
            } else {
                existingJob.setAmount(existingJob.getAmount() + 1);
                craftingJobsGraph.onCraftingJobFinished(dependency);
            }
        }

        return missingDependencies;
    }

    // Helper function for calculateCraftingJobDependencyComponent
    protected static <T1, M1, T2, M2> void addRemainderAsSurplusForComponent(IngredientComponent<T1, M1> ingredientComponent,
                                                                             List<T1> instances,
                                                                             IngredientCollectionPrototypeMap<T1, M1> simulatedExtractionMemory,
                                                                             IngredientComponent<T2, M2> blackListComponent,
                                                                             T2 blacklistInstance, M2 blacklistCondition,
                                                                             long blacklistQuantity) {
        IIngredientMatcher<T2, M2> blacklistMatcher = blackListComponent.getMatcher();
        for (T1 instance : instances) {
            IIngredientMatcher<T1, M1> outputMatcher = ingredientComponent.getMatcher();
            long reduceQuantity = 0;
            if (blackListComponent == ingredientComponent
                    && blacklistMatcher.matches(blacklistInstance, (T2) instance, blacklistCondition)) {
                reduceQuantity = blacklistQuantity;
            }
            long quantity = simulatedExtractionMemory.getQuantity(instance) + (outputMatcher.getQuantity(instance) - reduceQuantity);
            if (quantity > 0) {
                simulatedExtractionMemory.setQuantity(instance, quantity);
            }
        }
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
                    matchCondition, craftMissing, identifierGenerator, dependencyGraph, false);

            dependencyGraph.addCraftingJobId(craftingJob);

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
     * @param recipeOutputQuantity The number of times the given recipe should be applied.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter, may be Void.
     * @return A list of slot-based ingredients, or null if no valid inputs could be found.
     */
    @Nullable
    public static <T, M> List<T> getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage,
                                                           IngredientComponent<T, M> ingredientComponent,
                                                           IRecipeDefinition recipe, boolean simulate,
                                                           long recipeOutputQuantity) {
        return getIngredientRecipeInputs(storage, ingredientComponent, recipe, simulate,
                simulate ? new IngredientCollectionPrototypeMap<>(ingredientComponent, true) : null,
                false, recipeOutputQuantity).getLeft();
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
     *                                  The quantities can also go negatives,
     *                                  which means that a surplus of the given instance is present,
     *                                  which will be used up first before a call to the storage.
     * @param collectMissingIngredients If missing ingredients should be collected.
     *                                  If false, then the first returned list may be null
     *                                  if no valid matches can be found,
     *                                  and the second returned list is always null,
     * @param recipeOutputQuantity The number of times the given recipe should be applied.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter, may be Void.
     * @return A pair with two lists:
     *           1. A list of available slot-based ingredients.
     *           2. A missing ingredients object.
     */
    public static <T, M> Pair<List<T>, MissingIngredients<T, M>>
    getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage, IngredientComponent<T, M> ingredientComponent,
                              IRecipeDefinition recipe, boolean simulate,
                              IngredientCollectionPrototypeMap<T, M> simulatedExtractionMemory,
                              boolean collectMissingIngredients, long recipeOutputQuantity) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();

        // Quickly return if the storage is empty
        if (storage.getMaxQuantity() == 0) {
            if (collectMissingIngredients) {
                MissingIngredients<T, M> missing = new MissingIngredients<>(recipe.getInputs(ingredientComponent)
                        .stream()
                        .map(l -> multiplyPrototypedIngredients(l, recipeOutputQuantity))
                        .map(ps -> new MissingIngredients.Element<>(ps
                                .stream()
                                .map(p -> new MissingIngredients.PrototypedWithRequested<>(p, matcher.getQuantity(p.getPrototype())))
                                .collect(Collectors.toList()))
                        )
                        .collect(Collectors.toList()));
                return Pair.of(
                        Lists.newArrayList(Collections.nCopies(recipe.getInputs(ingredientComponent).size(),
                                ingredientComponent.getMatcher().getEmptyInstance())),
                        missing);
            } else {
                return Pair.of(null, null);
            }
        }

        // Iterate over all input slots
        List<List<IPrototypedIngredient<T, M>>> inputAlternativePrototypes = recipe.getInputs(ingredientComponent);
        List<T> inputInstances = Lists.newArrayList();
        List<MissingIngredients.Element<T, M>> missingElements =
                collectMissingIngredients ? Lists.newArrayList() : null;
        for (List<IPrototypedIngredient<T, M>> inputPrototypes : inputAlternativePrototypes) {
            T inputInstance = null;
            boolean hasInputInstance = false;

            // Iterate over all alternatives for this input slot, and take the first matching ingredient.
            List<MissingIngredients.PrototypedWithRequested<T, M>> missingAlternatives = Lists.newArrayList();
            for (IPrototypedIngredient<T, M> inputPrototype : inputPrototypes) {
                // Multiply required prototype if recipe quantity is higher than one
                if (recipeOutputQuantity > 1) {
                    inputPrototype = multiplyPrototypedIngredient(inputPrototype, recipeOutputQuantity);
                }

                // If the prototype is empty, we can skip network extraction
                if (matcher.isEmpty(inputPrototype.getPrototype())) {
                    inputInstance = inputPrototype.getPrototype();
                    hasInputInstance = true;
                    break;
                }

                long prototypeQuantity = matcher.getQuantity(inputPrototype.getPrototype());
                long memoryQuantity;
                if (simulate && (memoryQuantity = simulatedExtractionMemory
                        .getQuantity(inputPrototype.getPrototype())) != 0) {
                    long newQuantity = memoryQuantity + prototypeQuantity;
                    if (newQuantity > 0) {
                        // Part of our quantity can be provided via simulatedExtractionMemory,
                        // but not all of it,
                        // so we need to extract from storage as well.
                        T newInstance = matcher.withQuantity(inputPrototype.getPrototype(), newQuantity);
                        M matchCondition = matcher.withoutCondition(inputPrototype.getCondition(),
                                ingredientComponent.getPrimaryQuantifier().getMatchCondition());
                        T extracted = storage.extract(newInstance, matchCondition, true);
                        long quantityExtracted = matcher.getQuantity(extracted);
                        if (quantityExtracted == newQuantity) {
                            inputInstance = inputPrototype.getPrototype();
                            hasInputInstance = true;
                            simulatedExtractionMemory.add(inputPrototype.getPrototype());
                            break;
                        } else if (collectMissingIngredients) {
                            long quantityMissing = newQuantity - quantityExtracted;
                            missingAlternatives.add(new MissingIngredients.PrototypedWithRequested<>(inputPrototype, quantityMissing));
                        }
                    } else {
                        // All of our quantity can be provided via our surplus in simulatedExtractionMemory
                        inputInstance = inputPrototype.getPrototype();
                        hasInputInstance = true;
                        simulatedExtractionMemory.add(inputPrototype.getPrototype());
                        missingAlternatives.add(new MissingIngredients.PrototypedWithRequested<>(inputPrototype, 0));
                        break;
                    }
                } else {
                    M matchCondition = matcher.withoutCondition(inputPrototype.getCondition(),
                            ingredientComponent.getPrimaryQuantifier().getMatchCondition());
                    T extracted = storage.extract(inputPrototype.getPrototype(), matchCondition, simulate);
                    long quantityExtracted = matcher.getQuantity(extracted);
                    if (prototypeQuantity == quantityExtracted) {
                        inputInstance = extracted;
                        hasInputInstance = true;
                        if (simulate) {
                            simulatedExtractionMemory.add(extracted);
                        }
                        break;
                    } else if (collectMissingIngredients) {
                        long quantityMissing = prototypeQuantity - quantityExtracted;
                        missingAlternatives.add(new MissingIngredients.PrototypedWithRequested<>(inputPrototype, quantityMissing));
                    }
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
                    // Multiply missing collection if recipe quantity is higher than one
                    missingElements.add(new MissingIngredients.Element<>(missingAlternatives));
                }
            }

            // Otherwise, append it to the list and carry on.
            inputInstances.add(inputInstance);
        }

        return Pair.of(
                collectMissingIngredients && missingElements.size() > 0 ? null : inputInstances,
                collectMissingIngredients ? new MissingIngredients<>(missingElements) : null
        );
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
     * @param recipeOutputQuantity The number of times the given recipe should be applied.
     * @return The found ingredients or null.
     */
    @Nullable
    public static IMixedIngredients getRecipeInputs(INetwork network, int channel,
                                                    IRecipeDefinition recipe, boolean simulate,
                                                    long recipeOutputQuantity) {
        Map<IngredientComponent<?, ?>, List<?>> inputs = getRecipeInputs(getNetworkStorageGetter(network, channel),
                recipe, simulate, Maps.newIdentityHashMap(), false, recipeOutputQuantity).getLeft();
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
     * @param recipeOutputQuantity The number of times the given recipe should be applied.
     * @return A pair with two objects:
     *           1. The found ingredients or null.
     *           2. A mapping from ingredient component to missing ingredients (non-slot-based).
     */
    public static Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>>
    getRecipeInputs(Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter, IRecipeDefinition recipe, boolean simulate,
                    Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemories,
                    boolean collectMissingIngredients, long recipeOutputQuantity) {
        Map<IngredientComponent<?, ?>, List<?>> ingredientsAvailable = Maps.newIdentityHashMap();
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> ingredientsMissing = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> ingredientComponent : recipe.getInputComponents()) {
            IIngredientComponentStorage storage = storageGetter.apply(ingredientComponent);
            IngredientCollectionPrototypeMap<?, ?> simulatedExtractionMemory = simulatedExtractionMemories.get(ingredientComponent);
            if (simulatedExtractionMemory == null) {
                simulatedExtractionMemory = new IngredientCollectionPrototypeMap<>(ingredientComponent, true);
                simulatedExtractionMemories.put(ingredientComponent, simulatedExtractionMemory);
            }
            Pair<List<?>, MissingIngredients<?, ?>> subIngredients = getIngredientRecipeInputs(storage,
                    (IngredientComponent) ingredientComponent, recipe, simulate, simulatedExtractionMemory,
                    collectMissingIngredients, recipeOutputQuantity);
            List<?> subIngredientAvailable = subIngredients.getLeft();
            MissingIngredients<?, ?> subIngredientsMissing = subIngredients.getRight();
            if (subIngredientAvailable == null && !collectMissingIngredients) {
                return Pair.of(null, null);
            } else {
                if (subIngredientAvailable != null && !subIngredientAvailable.isEmpty()) {
                    ingredientsAvailable.put(ingredientComponent, subIngredientAvailable);
                }
                if (collectMissingIngredients && !subIngredientsMissing.getElements().isEmpty()) {
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
            newRecipeOutputs.put(entry.getKey(), multiplyPrototypedIngredients((List) entry.getValue(), amount));
        }
        return newRecipeOutputs;
    }

    /**
     * Multiply the quantity of a given prototyped ingredient list with the given amount.
     * @param prototypedIngredients A prototyped ingredient list.
     * @param amount An amount to multiply by.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return A multiplied prototyped ingredient list.
     */
    public static <T, M> List<IPrototypedIngredient<T, M>> multiplyPrototypedIngredients(List<IPrototypedIngredient<T, M>> prototypedIngredients,
                                                                                         long amount) {
        return prototypedIngredients
                .stream()
                .map(p -> multiplyPrototypedIngredient(p, amount))
                .collect(Collectors.toList());
    }

    /**
     * Multiply the quantity of a given prototyped ingredient with the given amount.
     * @param prototypedIngredient A prototyped ingredient.
     * @param amount An amount to multiply by.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return A multiplied prototyped ingredient.
     */
    public static <T, M> IPrototypedIngredient<T, M> multiplyPrototypedIngredient(IPrototypedIngredient<T, M> prototypedIngredient,
                                                                                  long amount) {
        IIngredientMatcher<T, M> matcher = prototypedIngredient.getComponent().getMatcher();
        return new PrototypedIngredient<>(prototypedIngredient.getComponent(),
                matcher.withQuantity(prototypedIngredient.getPrototype(),
                        matcher.getQuantity(prototypedIngredient.getPrototype()) * amount),
                prototypedIngredient.getCondition());
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
