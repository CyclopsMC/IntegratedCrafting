package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.*;
import org.cyclops.commoncapabilities.api.ingredient.capability.ICapabilityGetter;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IngredientComponentStorageEmpty;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.cyclopscore.helper.IModHelpers;
import org.cyclops.cyclopscore.ingredient.collection.*;
import org.cyclops.cyclopscore.ingredient.storage.IngredientComponentStorageSlottedCollectionWrapper;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.*;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.PartStateException;
import org.cyclops.integrateddynamics.api.ingredient.capability.IPositionedAddonsNetworkIngredientsHandler;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.network.IngredientChannelAdapter;
import org.cyclops.integrateddynamics.core.network.IngredientChannelIndexed;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Helpers related to handling crafting jobs.
 * @author rubensworks
 */
public class CraftingHelpers {

    /**
     * Get the network at the given position,
     * or throw a PartStateException if it is null.
     * @param pos A position.
     * @return A network.
     * @throws PartStateException If the network could not be found.
     */
    public static INetwork getNetworkChecked(PartPos pos) throws PartStateException {
        INetwork network = NetworkHelpers.getNetwork(pos.getPos().getLevel(true), pos.getPos().getBlockPos(), pos.getSide()).orElse(null);
        if (network == null) {
            IntegratedDynamics.clog(Level.ERROR, "Could not get the network for transfer as no network was found.");
            throw new PartStateException(pos.getPos(), pos.getSide());
        }
        return network;
    }

    /**
     * Get the crafting network in the given network.
     * @param network A network.
     * @return The crafting network.
     */
    public static Optional<ICraftingNetwork> getCraftingNetwork(@Nullable INetwork network) {
        if (network != null) {
            return network.getCapability(Capabilities.CraftingNetwork.NETWORK);
        }
        return Optional.empty();
    }

    /**
     * Get the crafting network in the given network.
     * @param network A network.
     * @return The crafting network.
     */
    public static ICraftingNetwork getCraftingNetworkChecked(@Nullable INetwork network) {
        return getCraftingNetwork(network)
                .orElseThrow(() -> new IllegalStateException("Could not find a crafting network"));
    }

    /**
     * Get the storage network of the given type in the given network.
     * @param network A network.
     * @param ingredientComponent The ingredient component type of the network.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The storage network.
     */
    public static <T, M> Optional<IPositionedAddonsNetworkIngredients<T, M>> getIngredientsNetwork(INetwork network,
                                                                                                       IngredientComponent<T, M> ingredientComponent) {
        return ingredientComponent
                .getCapability(org.cyclops.integrateddynamics.Capabilities.PositionedAddonsNetworkIngredientsHandler.INGREDIENT)
                .flatMap(ingredientsHandler -> ((IPositionedAddonsNetworkIngredientsHandler<T, M>) ingredientsHandler).getStorage(network));
    }

    /**
     * Get the storage network of the given type in the given network.
     * @param network A network.
     * @param ingredientComponent The ingredient component type of the network.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The storage network.
     */
    public static <T, M> IPositionedAddonsNetworkIngredients<T, M> getIngredientsNetworkChecked(INetwork network,
                                                                                                IngredientComponent<T, M> ingredientComponent) {
        return getIngredientsNetwork(network, ingredientComponent)
                .orElseThrow(() -> new IllegalStateException("Could not find an ingredients network"));
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
        IPositionedAddonsNetworkIngredients<T, M> ingredientsNetwork = getIngredientsNetwork(network, ingredientComponent).orElse(null);
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
        ICraftingNetwork craftingNetwork = getCraftingNetworkChecked(network);
        IRecipeIndex recipeIndex = craftingNetwork.getRecipeIndex(channel);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = getNetworkStorageGetter(network, channel, true);

        CraftingJob craftingJob = calculateCraftingJobs(recipeIndex, channel, storageGetter, ingredientComponent, instance, matchCondition,
                craftMissing, Maps.newIdentityHashMap(), identifierGenerator, craftingJobsGraph, Sets.newHashSet(),
                collectMissingRecipes);
        craftingJobsGraph.addCraftingJobId(craftingJob);
        return craftingJob;
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
                                                    IRecipeDefinition recipe, int amount, boolean craftMissing,
                                                    IIdentifierGenerator identifierGenerator,
                                                    CraftingJobDependencyGraph craftingJobsGraph,
                                                    boolean collectMissingRecipes)
            throws FailedCraftingRecipeException, RecursiveCraftingRecipeException {
        ICraftingNetwork craftingNetwork = getCraftingNetworkChecked(network);
        IRecipeIndex recipeIndex = craftingNetwork.getRecipeIndex(channel);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = getNetworkStorageGetter(network, channel, true);

        PartialCraftingJobCalculation result = calculateCraftingJobs(recipeIndex, channel, storageGetter, recipe, amount,
                craftMissing, Maps.newIdentityHashMap(), identifierGenerator, craftingJobsGraph, Sets.newHashSet(),
                collectMissingRecipes);
        if (result.getCraftingJob() == null) {
            throw new FailedCraftingRecipeException(recipe, amount, result.getMissingDependencies(),
                    compressMixedIngredients(new MixedIngredients(result.getIngredientsStorage())), result.getPartialCraftingJobs());
        } else {
            craftingJobsGraph.addCraftingJobId(result.getCraftingJob());
            return result.getCraftingJob();
        }
    }

    /**
     * @return An identifier generator for crafting jobs.
     */
    public static IIdentifierGenerator getGlobalCraftingJobIdentifier() {
        return () -> IntegratedCrafting.globalCounters.get().getNext("craftingJob");
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
     * @param extractionMemoryReusable Stores the reusable ingredients.
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
                                                                      IIngredientCollectionMutable<?, ?>> extractionMemoryReusable,
                                                              IIdentifierGenerator identifierGenerator,
                                                              CraftingJobDependencyGraph craftingJobsGraph,
                                                              Set<IPrototypedIngredient> parentDependencies,
                                                              boolean collectMissingRecipes)
            throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        // Create a root planning transaction that spans the entire calculation.
        // All ingredient extractions during planning are done within this transaction,
        // so that sub-dependency planning correctly sees already-"consumed" items.
        // The transaction is never committed, so storage is never actually modified.
        try (Transaction planningTx = Transaction.openRoot()) {
            return calculateCraftingJobsWithPlanningTx(recipeIndex, channel, storageGetter, ingredientComponent, instance, matchCondition, craftMissing, extractionMemoryReusable, identifierGenerator, craftingJobsGraph, parentDependencies, collectMissingRecipes, planningTx);
        }
    }

    private static <T, M> CraftingJob calculateCraftingJobsWithPlanningTx(IRecipeIndex recipeIndex, int channel,
                                                              Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
                                                              IngredientComponent<T, M> ingredientComponent,
                                                              T instance, M matchCondition, boolean craftMissing,
                                                              Map<IngredientComponent<?, ?>,
                                                                      IIngredientCollectionMutable<?, ?>> extractionMemoryReusable,
                                                              IIdentifierGenerator identifierGenerator,
                                                              CraftingJobDependencyGraph craftingJobsGraph,
                                                              Set<IPrototypedIngredient> parentDependencies,
                                                              boolean collectMissingRecipes,
                                                              TransactionContext planningTx)
            throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        // This matching condition makes it so that the recipe output does not have to match with the requested input by quantity.
        M quantifierlessCondition = matcher.withoutCondition(matchCondition,
                ingredientComponent.getPrimaryQuantifier().getMatchCondition());
        long instanceQuantity = matcher.getQuantity(instance);

        // Loop over all available recipes, and return the first valid one.
        Iterator<IRecipeDefinition> recipes = recipeIndex.getRecipes(ingredientComponent, instance, quantifierlessCondition);
        List<UnknownCraftingRecipeException> firstMissingDependencies = Lists.newArrayList();
        Map<IngredientComponent<?, ?>, List<?>> firstIngredientsStorage = Collections.emptyMap();
        List<CraftingJob> firstPartialCraftingJobs = Lists.newArrayList();
        RecursiveCraftingRecipeException firstRecursiveException = null;
        while (recipes.hasNext()) {
            IRecipeDefinition recipe = recipes.next();

            // Calculate the quantity for the given instance that the recipe outputs
            long recipeOutputQuantity = getOutputQuantityForRecipe(recipe, ingredientComponent, instance, quantifierlessCondition);
            // Based on the quantity of the recipe output, calculate the amount of required recipe jobs.
            int amount = (int) Math.ceil(((float) instanceQuantity) / (float) recipeOutputQuantity);

            // Calculate jobs for the given recipe.
            // Each recipe attempt gets its own nested transaction within the planning tx.
            // If the recipe attempt fails, the nested tx is rolled back (ingredients returned).
            // If it succeeds, the nested tx is committed into planningTx.
            try (Transaction recipeTx = Transaction.open(planningTx)) {
                try {
                    PartialCraftingJobCalculation result = calculateCraftingJobsWithPlanningTx(recipeIndex, channel,
                            storageGetter, recipe, amount, craftMissing,
                            extractionMemoryReusable, identifierGenerator, craftingJobsGraph, parentDependencies,
                            collectMissingRecipes && firstMissingDependencies.isEmpty(), recipeTx);
                    if (result.getCraftingJob() == null) {
                        firstMissingDependencies = result.getMissingDependencies();
                        firstIngredientsStorage = result.getIngredientsStorage();
                        if (result.getPartialCraftingJobs() != null) {
                            firstPartialCraftingJobs = result.getPartialCraftingJobs();
                        }
                        // recipeTx is rolled back (auto-close without commit)
                    } else {
                        recipeTx.commit(); // This recipe attempt succeeded; commit into planningTx
                        return result.getCraftingJob();
                    }
                } catch (RecursiveCraftingRecipeException e) {
                    if (firstRecursiveException == null) {
                        firstRecursiveException = e;
                    }
                    // recipeTx is rolled back (auto-close without commit)
                }
            }
        }

        if (firstRecursiveException != null) {
            throw firstRecursiveException;
        }

        // No valid recipes were available, so we error or collect the missing instance.
        throw new UnknownCraftingRecipeException(new PrototypedIngredient<>(ingredientComponent, instance, matchCondition),
                matcher.getQuantity(instance), firstMissingDependencies, compressMixedIngredients(new MixedIngredients(firstIngredientsStorage)),
                firstPartialCraftingJobs);
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
     * @param extractionMemoryReusable This map remembers all extracted reusable instances.
     *                                 This is to make sure that instances can not be extracted multiple times
     *                                 when simulating.
     *                                 The quantities can also go negatives,
     *                                 which means that a surplus of the given instance is present,
     *                                 which will be used up first before a call to the storage.
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
    protected static PartialCraftingJobCalculation calculateCraftingJobs(
            IRecipeIndex recipeIndex, int channel,
            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
            IRecipeDefinition recipe, int amount, boolean craftMissing,
            Map<IngredientComponent<?, ?>,
                    IIngredientCollectionMutable<?, ?>> extractionMemoryReusable,
            IIdentifierGenerator identifierGenerator,
            CraftingJobDependencyGraph craftingJobsGraph,
            Set<IPrototypedIngredient> parentDependencies,
            boolean collectMissingRecipes)
            throws RecursiveCraftingRecipeException {
        // Create a root planning transaction that spans the entire calculation.
        // All ingredient extractions during planning are done within this transaction.
        // The transaction is never committed, so storage is never actually modified.
        try (Transaction planningTx = Transaction.openRoot()) {
            return calculateCraftingJobsWithPlanningTx(recipeIndex, channel, storageGetter, recipe, amount, craftMissing, extractionMemoryReusable, identifierGenerator, craftingJobsGraph, parentDependencies, collectMissingRecipes, planningTx);
        }
    }

    private static PartialCraftingJobCalculation calculateCraftingJobsWithPlanningTx(
            IRecipeIndex recipeIndex, int channel,
            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
            IRecipeDefinition recipe, int amount, boolean craftMissing,
            Map<IngredientComponent<?, ?>,
                    IIngredientCollectionMutable<?, ?>> extractionMemoryReusable,
            IIdentifierGenerator identifierGenerator,
            CraftingJobDependencyGraph craftingJobsGraph,
            Set<IPrototypedIngredient> parentDependencies,
            boolean collectMissingRecipes,
            TransactionContext planningTx)
            throws RecursiveCraftingRecipeException {
        List<UnknownCraftingRecipeException> missingDependencies = Lists.newArrayList();
        List<CraftingJob> partialCraftingJobs = Lists.newArrayList();

        // Check if all requirements are met for this recipe using the planning transaction.
        // Items extracted here remain "consumed" within planningTx, so subsequent dependency
        // planning correctly sees reduced storage availability.
        Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> simulation;
        simulation = getRecipeInputs(storageGetter, recipe, planningTx, extractionMemoryReusable,
                true, amount);
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> missingIngredients = simulation.getRight();
        if (!craftMissing && !missingIngredients.isEmpty()) {
            if (collectMissingRecipes) {
                // Collect missing ingredients as missing recipes when we don't want to craft sub-components,
                // but they are missing.
                for (Map.Entry<IngredientComponent<?, ?>, MissingIngredients<?, ?>> entry : missingIngredients.entrySet()) {
                    for (MissingIngredients.Element<?, ?> element : entry.getValue().getElements()) {
                        MissingIngredients.PrototypedWithRequested<?, ?> alternative = element.getAlternatives().get(0);

                        // Calculate the instance that was available in storage
                        IngredientComponent<?, ?> component = alternative.getRequestedPrototype().getComponent();
                        IIngredientMatcher matcher = component.getMatcher();
                        long storedQuantity = matcher.getQuantity(alternative.getRequestedPrototype().getPrototype()) - alternative.getQuantityMissing();
                        Map<IngredientComponent<?, ?>, List<?>> storageMap;
                        if (storedQuantity > 0) {
                            storageMap = Maps.newIdentityHashMap();
                            storageMap.put(component, Collections.singletonList(
                                    matcher.withQuantity(alternative.getRequestedPrototype().getPrototype(), storedQuantity)
                            ));
                        } else {
                            storageMap = Collections.emptyMap();
                        }

                        missingDependencies.add(new UnknownCraftingRecipeException(
                                alternative.getRequestedPrototype(), alternative.getQuantityMissing(),
                                Collections.emptyList(), compressMixedIngredients(new MixedIngredients(storageMap)), Lists.newArrayList()));
                    }
                }
            }
            return new PartialCraftingJobCalculation(null, missingDependencies, simulation.getLeft(), null);
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
                PartialCraftingJobCalculationDependency resultDependency = calculateCraftingJobDependencyComponentWithPlanningTx(
                        dependencyComponent, dependenciesOutputSurplus, missingIngredients.get(dependencyComponent), parentDependencies,
                        dependencies, recipeIndex, channel, storageGetter, extractionMemoryReusable,
                        identifierGenerator, craftingJobsGraph, collectMissingRecipes, planningTx);
                // Don't check the other components once we have an invalid dependency.
                if (!resultDependency.isValid()) {
                    missingDependencies.addAll(resultDependency.getUnknownCrafingRecipes());
                    partialCraftingJobs.addAll(resultDependency.getPartialCraftingJobs());
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
            return new PartialCraftingJobCalculation(null, missingDependencies, simulation.getLeft(), partialCraftingJobs);
        }

        // Propagate remaining surplus from dep outputs to storage within planningTx.
        // This is equivalent to the old simulatedExtractionMemory negative tracking:
        // any surplus produced by sub-recipe planning is made "virtually available" in storage
        // for subsequent sibling dep planning calls (at the caller's level) to extract.
        propagateSurplusToStorage(storageGetter, dependenciesOutputSurplus, planningTx);

        CraftingJob craftingJob = new CraftingJob(identifierGenerator.getNext(), channel, recipe, amount,
                compressMixedIngredients(new MixedIngredients(simulation.getLeft())));
        for (CraftingJob dependency : dependencies.values()) {
            craftingJob.addDependency(dependency);
            craftingJobsGraph.addDependency(craftingJob, dependency);
        }
        return new PartialCraftingJobCalculation(craftingJob, null, simulation.getLeft(), null);
    }

    // Helper function for calculateCraftingJobs, returns a list of non-craftable ingredients and craftable ingredients.
    protected static <T, M> PartialCraftingJobCalculationDependency calculateCraftingJobDependencyComponent(
            IngredientComponent<T, M> dependencyComponent,
            Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> dependenciesOutputSurplus,
            MissingIngredients<T, M> missingIngredients,
            Set<IPrototypedIngredient> parentDependencies,
            Map<IRecipeDefinition, CraftingJob> dependencies,
            IRecipeIndex recipeIndex,
            int channel,
            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
            Map<IngredientComponent<?, ?>,
                    IIngredientCollectionMutable<?, ?>> extractionMemoryReusable,
            IIdentifierGenerator identifierGenerator,
            CraftingJobDependencyGraph craftingJobsGraph,
            boolean collectMissingRecipes)
            throws RecursiveCraftingRecipeException {
        try (Transaction planningTx = Transaction.openRoot()) {
            return calculateCraftingJobDependencyComponentWithPlanningTx(dependencyComponent, dependenciesOutputSurplus, missingIngredients, parentDependencies, dependencies, recipeIndex, channel, storageGetter, extractionMemoryReusable, identifierGenerator, craftingJobsGraph, collectMissingRecipes, planningTx);
        }
    }

    private static <T, M> PartialCraftingJobCalculationDependency calculateCraftingJobDependencyComponentWithPlanningTx(
            IngredientComponent<T, M> dependencyComponent,
            Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> dependenciesOutputSurplus,
            MissingIngredients<T, M> missingIngredients,
            Set<IPrototypedIngredient> parentDependencies,
            Map<IRecipeDefinition, CraftingJob> dependencies,
            IRecipeIndex recipeIndex,
            int channel,
            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
            Map<IngredientComponent<?, ?>,
                    IIngredientCollectionMutable<?, ?>> extractionMemoryReusable,
            IIdentifierGenerator identifierGenerator,
            CraftingJobDependencyGraph craftingJobsGraph,
            boolean collectMissingRecipes,
            TransactionContext planningTx)
            throws RecursiveCraftingRecipeException {
        IIngredientMatcher<T, M> dependencyMatcher = dependencyComponent.getMatcher();
        List<UnknownCraftingRecipeException> missingDependencies = Lists.newArrayList();
        for (MissingIngredients.Element<T, M> missingElement : missingIngredients.getElements()) {
            CraftingJob dependency = null;
            T dependencyInstance = null;
            boolean skipDependency = false;
            UnknownCraftingRecipeException firstError = null;
            // Loop over all prototype alternatives, at least one has to match.
            for (MissingIngredients.PrototypedWithRequested<T, M> prototypedAlternative : missingElement.getAlternatives()) {
                // Check if the missing element is reusable, and was triggered for craft earlier.
                if (missingElement.isInputReusable() && ((IIngredientCollectionMutable<T, M>) extractionMemoryReusable.get(dependencyComponent))
                        .contains(prototypedAlternative.getRequestedPrototype().getPrototype())) {
                    // Nothing has to be crafted anymore, jump to next dependency
                    skipDependency = true;
                    break;
                }

                IPrototypedIngredient<T, M> prototype = new PrototypedIngredient<>(
                        dependencyComponent,
                        dependencyMatcher.withQuantity(prototypedAlternative.getRequestedPrototype().getPrototype(), prototypedAlternative.getQuantityMissing()),
                        prototypedAlternative.getRequestedPrototype().getCondition()
                );
                // First check if we can grab it from previous surplus
                IngredientCollectionPrototypeMap<T, M> dependencyComponentSurplusOld = (IngredientCollectionPrototypeMap<T, M>) dependenciesOutputSurplus.get(dependencyComponent);
                IngredientCollectionPrototypeMap<T, M> dependencyComponentSurplus = null;
                if (dependencyComponentSurplusOld != null) {
                    // First create a copy of the given surplus store,
                    // and only once we see that the prototype is valid,
                    // save this copy again.
                    // This is because if this prototype is invalid,
                    // then we don't want these invalid surpluses.
                    dependencyComponentSurplus = new IngredientCollectionPrototypeMap<>(dependencyComponentSurplusOld.getComponent(), true);
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
                    IPrototypedIngredient<T, M> dependencyPrototype = prototype;
                    if (dependencyMatcher.getQuantity(dependencyPrototype.getPrototype()) != 1) {
                        // Ensure 1-quantity is stored, for proper comparisons in future calls.
                        dependencyPrototype = new PrototypedIngredient<>(dependencyComponent,
                                dependencyMatcher.withQuantity(prototype.getPrototype(), 1),
                                prototype.getCondition());
                    }
                    if (!childDependencies.add(dependencyPrototype)) {
                        throw new RecursiveCraftingRecipeException(prototype);
                    }

                    dependency = calculateCraftingJobsWithPlanningTx(recipeIndex, channel, storageGetter,
                            dependencyComponent, prototype.getPrototype(),
                            prototype.getCondition(), true, extractionMemoryReusable,
                            identifierGenerator, craftingJobsGraph, childDependencies, collectMissingRecipes, planningTx);
                    dependencyInstance = prototype.getPrototype();

                    // The prototype is valid,
                    // so we can finally store our temporary surplus
                    if (dependencyComponentSurplus != null) {
                        dependenciesOutputSurplus.put(dependencyComponent, dependencyComponentSurplus);
                    }

                    // Add the auxiliary recipe outputs that are not requested to the surplus
                    Object dependencyQuantifierlessCondition = dependencyMatcher.withoutCondition(prototype.getCondition(),
                            dependencyComponent.getPrimaryQuantifier().getMatchCondition());
                    long requestedQuantity = dependencyMatcher.getQuantity(prototype.getPrototype());
                    for (IngredientComponent outputComponent : dependency.getRecipe().getOutput().getComponents()) {
                        IngredientCollectionPrototypeMap<?, ?> componentSurplus = dependenciesOutputSurplus.get(outputComponent);
                        if (componentSurplus == null) {
                            componentSurplus = new IngredientCollectionPrototypeMap<>(outputComponent, true);
                            dependenciesOutputSurplus.put(outputComponent, componentSurplus);
                        }
                        List<Object> instances = dependency.getRecipe().getOutput().getInstances(outputComponent);
                        long recipeAmount = dependency.getAmount();
                        if (recipeAmount > 1) {
                            IIngredientMatcher matcher = outputComponent.getMatcher();
                            // If more than one recipe amount was crafted, correctly multiply the outputs to calculate the effective surplus.
                            instances = instances
                                    .stream()
                                    .map(instance -> matcher.withQuantity(instance, matcher.getQuantity(instance) * recipeAmount))
                                    .collect(Collectors.toList());
                        }
                        addRemainderAsSurplusForComponent(outputComponent, (List) instances, componentSurplus,
                                (IngredientComponent) prototype.getComponent(), prototype.getPrototype(), dependencyQuantifierlessCondition,
                                requestedQuantity);
                    }

                    break;
                } catch (UnknownCraftingRecipeException e) {
                    // Save the first error, and check the next prototype
                    if (firstError == null) {
                        // Modify the error so that the correct missing quantity is stored
                        firstError = new UnknownCraftingRecipeException(
                                e.getIngredient(), prototypedAlternative.getQuantityMissing(),
                                e.getMissingChildRecipes(), compressMixedIngredients(e.getIngredientsStorage()), e.getPartialCraftingJobs());
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

            // --- When we reach this point, a valid sub-recipe was found ---

            // If the dependency instance is reusable, mark it as available in our reusable extraction memory
            if (missingElement.isInputReusable()) {
                ((IIngredientCollectionMutable<T, M>) extractionMemoryReusable.get(dependencyComponent)).add(dependencyInstance);
            }

            // Add the valid sub-recipe it to our dependencies
            // If the recipe was already present at this level, just increment the amount of the existing job.
            CraftingJob existingJob = dependencies.get(dependency.getRecipe());
            if (existingJob == null) {
                dependencies.put(dependency.getRecipe(), dependency);
            } else {
                // Remove the standalone dependency job by merging it into the existing job
                craftingJobsGraph.mergeCraftingJobs(existingJob, dependency, true);
            }
        }

        return new PartialCraftingJobCalculationDependency(missingDependencies, dependencies.values());
    }

    // Helper function for calculateCraftingJobDependencyComponent: adds the remainder (surplus) of recipe outputs
    // to the surplus tracking map after subtracting what was requested.
    protected static <T1, M1, T2, M2> void addRemainderAsSurplusForComponent(IngredientComponent<T1, M1> ingredientComponent,
                                                                             List<T1> instances,
                                                                             IngredientCollectionPrototypeMap<T1, M1> componentSurplus,
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
            long quantity = componentSurplus.getQuantity(instance) + (outputMatcher.getQuantity(instance) - reduceQuantity);
            if (quantity > 0) {
                componentSurplus.setQuantity(instance, quantity);
            }
        }
    }

    /**
     * Propagates surplus items from dep-loop output surplus tracking into the storage within {@code planningTx}.
     * This is the transaction-based equivalent of the old simulatedExtractionMemory negative tracking:
     * any surplus produced by sub-recipe dep outputs is made "virtually available" in storage
     * so that subsequent sibling plannings (at the caller's recipe level) can extract it via normal storage access.
     */
    @SuppressWarnings("unchecked")
    private static void propagateSurplusToStorage(
            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
            Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> dependenciesOutputSurplus,
            TransactionContext planningTx) {
        for (Map.Entry<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> entry : dependenciesOutputSurplus.entrySet()) {
            IngredientComponent surplusComponent = entry.getKey();
            IngredientCollectionPrototypeMap surplusInstances = entry.getValue();
            if (surplusInstances == null) continue;
            IIngredientComponentStorage surplusStorage = storageGetter.apply(surplusComponent);
            if (surplusStorage == null) continue;
            // Ensure the storage handles the same component type to avoid type-mismatch errors
            if (surplusStorage.getComponent() != surplusComponent) continue;
            IIngredientMatcher matcher = surplusComponent.getMatcher();
            for (Object instance : surplusInstances) {
                long quantity = surplusInstances.getQuantity(instance);
                if (quantity > 0) {
                    Object surplusInstance = matcher.withQuantity(instance, quantity);
                    if (surplusStorage instanceof IngredientChannelAdapter)
                        ((IngredientChannelAdapter) surplusStorage).disableLimits();
                    surplusStorage.insert(surplusInstance, planningTx);
                    if (surplusStorage instanceof IngredientChannelAdapter)
                        ((IngredientChannelAdapter) surplusStorage).enableLimits();
                }
            }
        }
    }

    /**
     * Schedule all crafting jobs in the given dependency graph in the given network.
     *
     * @param craftingNetwork            The target crafting network.
     * @param storageGetter              The storage getter.
     * @param craftingJobDependencyGraph The crafting job dependency graph.
     * @param allowDistribution          If the crafting jobs are allowed to be split over multiple crafting interfaces.
     * @param initiator                  Optional UUID of the initiator.
     * @throws UnavailableCraftingInterfacesException If no crafting interfaces were available.
     */
    public static void scheduleCraftingJobs(ICraftingNetwork craftingNetwork,
                                            Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
                                            CraftingJobDependencyGraph craftingJobDependencyGraph,
                                            boolean allowDistribution,
                                            @Nullable UUID initiator) throws UnavailableCraftingInterfacesException {
        List<CraftingJob> startedJobs = Lists.newArrayList();
        craftingNetwork.getCraftingJobDependencyGraph().importDependencies(craftingJobDependencyGraph);
        for (CraftingJob craftingJob : craftingJobDependencyGraph.getCraftingJobs()) {
            try {
                craftingNetwork.scheduleCraftingJob(craftingJob, allowDistribution, storageGetter);
            } catch (UnavailableCraftingInterfacesException e) {
                // First, cancel all jobs that were already started
                for (CraftingJob startedJob : startedJobs) {
                    CraftingHelpers.insertIngredientsGuaranteed(startedJob.getIngredientsStorageBuffer(), storageGetter, (ICraftingResultsSink) Iterables.getFirst(craftingNetwork.getCraftingInterfaces(startedJob.getChannel()), null));
                    craftingNetwork.cancelCraftingJob(startedJob.getChannel(), startedJob.getId());
                }

                // Then, throw an exception for all jobs in this dependency graph
                throw new UnavailableCraftingInterfacesException(craftingJobDependencyGraph.getCraftingJobs());
            }
            startedJobs.add(craftingJob);
            if (initiator != null) {
                craftingJob.setInitiatorUuid(initiator.toString());
            }
        }
    }

    /**
     * Schedule the given crafting job  in the given network.
     *
     * @param craftingNetwork   The target crafting network.
     * @param storageGetter     The storage getter.
     * @param craftingJob       The crafting job to schedule.
     * @param allowDistribution If the crafting job is allowed to be split over multiple crafting interfaces.
     * @param initiator         Optional UUID of the initiator.
     * @return The scheduled crafting job.
     * @throws UnavailableCraftingInterfacesException If no crafting interfaces were available.
     */
    public static CraftingJob scheduleCraftingJob(ICraftingNetwork craftingNetwork,
                                                  Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
                                                  CraftingJob craftingJob,
                                                  boolean allowDistribution,
                                                  @Nullable UUID initiator) throws UnavailableCraftingInterfacesException {
        craftingNetwork.scheduleCraftingJob(craftingJob, allowDistribution, storageGetter);
        if (initiator != null) {
            craftingJob.setInitiatorUuid(initiator.toString());
        }
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
     * @param allowDistribution If the crafting job is allowed to be split over multiple crafting interfaces.
     * @param identifierGenerator An ID generator for crafting jobs.
     * @param initiator Optional UUID of the initiator.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The scheduled crafting job, or null if no recipe was found.
     */
    @Nullable
    public static <T, M> CraftingJob calculateAndScheduleCraftingJob(INetwork network, int channel,
                                                                     IngredientComponent<T, M> ingredientComponent,
                                                                     T instance, M matchCondition,
                                                                     boolean craftMissing, boolean allowDistribution,
                                                                     IIdentifierGenerator identifierGenerator,
                                                                     @Nullable UUID initiator) {
        try {
            CraftingJobDependencyGraph dependencyGraph = new CraftingJobDependencyGraph();
            CraftingJob craftingJob = calculateCraftingJobs(network, channel, ingredientComponent, instance,
                    matchCondition, craftMissing, identifierGenerator, dependencyGraph, false);

            ICraftingNetwork craftingNetwork = getCraftingNetworkChecked(network);

            scheduleCraftingJobs(craftingNetwork, getNetworkStorageGetter(network, channel, false), dependencyGraph, allowDistribution, initiator);

            return craftingJob;
        } catch (UnknownCraftingRecipeException | RecursiveCraftingRecipeException | UnavailableCraftingInterfacesException e) {
            return null;
        }
    }

    /**
     * Schedule a crafting job for the given recipe in the given network.
     * @param network The target network.
     * @param channel The target channel.
     * @param recipe The recipe to craft.
     * @param amount The amount to craft.
     * @param craftMissing If the missing required ingredients should also be crafted.
     * @param allowDistribution If the crafting job is allowed to be split over multiple crafting interfaces.
     * @param identifierGenerator An ID generator for crafting jobs.
     * @param initiator Optional UUID of the initiator.
     * @return The scheduled crafting job, or null if no recipe was found.
     */
    @Nullable
    public static CraftingJob calculateAndScheduleCraftingJob(INetwork network, int channel,
                                                              IRecipeDefinition recipe, int amount,
                                                              boolean craftMissing, boolean allowDistribution,
                                                              IIdentifierGenerator identifierGenerator,
                                                              @Nullable UUID initiator) {
        try {
            CraftingJobDependencyGraph dependencyGraph = new CraftingJobDependencyGraph();
            CraftingJob craftingJob = calculateCraftingJobs(network, channel, recipe, amount, craftMissing,
                    identifierGenerator, dependencyGraph, false);

            ICraftingNetwork craftingNetwork = getCraftingNetworkChecked(network);

            scheduleCraftingJobs(craftingNetwork, getNetworkStorageGetter(network, channel, false), dependencyGraph, allowDistribution, initiator);

            return craftingJob;
        } catch (RecursiveCraftingRecipeException | FailedCraftingRecipeException | UnavailableCraftingInterfacesException e) {
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
        IIngredientComponentStorage<T, M> storage = getNetworkStorage(network, channel, ingredientComponent, true);
        if (storage instanceof IngredientChannelAdapter) ((IngredientChannelAdapter) storage).disableLimits();
        boolean contains;
        if (storage instanceof IngredientChannelIndexed) {
            IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
            long quantityPresent = ((IngredientChannelIndexed<T, M>) storage).getIndex().getQuantity(instance);
            contains = matcher.hasCondition(matchCondition, ingredientComponent.getPrimaryQuantifier().getMatchCondition()) ? quantityPresent >= matcher.getQuantity(instance) : quantityPresent > 0;
        } else {
            contains = !ingredientComponent.getMatcher().isEmpty(storage.extract(instance, matchCondition, true));
        }
        if (storage instanceof IngredientChannelAdapter) ((IngredientChannelAdapter) storage).enableLimits();
        return contains;
    }

    /**
     * Check the quantity of the given instance in the network.
     * @param network The target network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to check.
     * @param matchCondition The match condition of the instance.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The quantity in the network.
     */
    public static <T, M> long getStorageInstanceQuantity(INetwork network, int channel,
                                                         IngredientComponent<T, M> ingredientComponent,
                                                         T instance, M matchCondition) {
        IIngredientComponentStorage<T, M> storage = getNetworkStorage(network, channel, ingredientComponent, true);
        if (storage instanceof IngredientChannelAdapter) ((IngredientChannelAdapter) storage).disableLimits();
        long quantityPresent;
        if (storage instanceof IngredientChannelIndexed) {
            quantityPresent = ((IngredientChannelIndexed<T, M>) storage).getIndex().getQuantity(instance);
        } else {
            quantityPresent = ingredientComponent.getMatcher().getQuantity(storage.extract(instance, matchCondition, true));
        }
        if (storage instanceof IngredientChannelAdapter) ((IngredientChannelAdapter) storage).enableLimits();
        return quantityPresent;
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
     * Check if there is a scheduled crafting job for the given recipe.
     * @param craftingNetwork The target crafting network.
     * @param channel The target channel.
     * @param recipe The recipe to check.
     * @return If the instance has a crafting job.
     */
    public static boolean isCrafting(ICraftingNetwork craftingNetwork, int channel,
                                     IRecipeDefinition recipe) {
        Iterator<CraftingJob> it = craftingNetwork.getCraftingJobs(channel);
        while (it.hasNext()) {
            if (it.next().getRecipe().equals(recipe)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all required recipe input ingredients from the network for the given ingredient component.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * @param storage The target storage.
     * @param ingredientComponent The ingredient component to get the ingredients for.
     * @param recipe The recipe to get the inputs from.
     * @param transaction The transaction context.
     * @param recipeOutputQuantity The number of times the given recipe should be applied.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter, may be Void.
     * @return A list of slot-based ingredients, or null if no valid inputs could be found.
     */
    @Nullable
    public static <T, M> List<T> getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage,
                                                           IngredientComponent<T, M> ingredientComponent,
                                                           IRecipeDefinition recipe, TransactionContext transaction,
                                                           long recipeOutputQuantity) {
        return getIngredientRecipeInputs(storage, ingredientComponent, recipe, transaction,
                new IngredientHashSet<>(ingredientComponent),
                false, recipeOutputQuantity).getLeft();
    }

    /**
     * @deprecated Use {@link #getIngredientRecipeInputs(IIngredientComponentStorage, IngredientComponent, IRecipeDefinition, TransactionContext, long)} instead.
     */
    @Deprecated // TODO: remove in next major
    @Nullable
    public static <T, M> List<T> getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage,
                                                           IngredientComponent<T, M> ingredientComponent,
                                                           IRecipeDefinition recipe, boolean simulate,
                                                           long recipeOutputQuantity) {
        try (Transaction tx = Transaction.openRoot()) {
            List<T> result = getIngredientRecipeInputs(storage, ingredientComponent, recipe, tx, recipeOutputQuantity);
            if (!simulate && result != null) {
                tx.commit();
            }
            return result;
        }
    }

    /**
     * Get all required recipe input ingredients from the network for the given ingredient component,
     * and optionally, explicitly calculate the missing ingredients.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * @param storage The target storage.
     * @param ingredientComponent The ingredient component to get the ingredients for.
     * @param recipe The recipe to get the inputs from.
     * @param transaction The transaction context.
     * @param extractionMemoryReusable This map remembers all extracted reusable instances in simulation mode.
     *                                 This is to make sure that instances can not be extracted multiple times
     *                                 when simulating.
     *                                 The quantities can also go negatives,
     *                                 which means that a surplus of the given instance is present,
     *                                 which will be used up first before a call to the storage.
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
                              IRecipeDefinition recipe, TransactionContext transaction,
                              IIngredientCollectionMutable<T, M> extractionMemoryReusable,
                              boolean collectMissingIngredients, long recipeOutputQuantity) {
        return getIngredientRecipeInputs(storage, ingredientComponent, recipe, transaction,
                extractionMemoryReusable, collectMissingIngredients, recipeOutputQuantity, false);
    }

    /**
     * @deprecated Use {@link #getIngredientRecipeInputs(IIngredientComponentStorage, IngredientComponent, IRecipeDefinition, TransactionContext, IIngredientCollectionMutable, boolean, long)} instead.
     */
    @Deprecated // TODO: remove in next major
    public static <T, M> Pair<List<T>, MissingIngredients<T, M>>
    getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage, IngredientComponent<T, M> ingredientComponent,
                              IRecipeDefinition recipe, boolean simulate,
                              IIngredientCollectionMutable<T, M> extractionMemoryReusable,
                              boolean collectMissingIngredients, long recipeOutputQuantity) {
        try (Transaction tx = Transaction.openRoot()) {
            Pair<List<T>, MissingIngredients<T, M>> result = getIngredientRecipeInputs(storage, ingredientComponent, recipe, tx,
                    extractionMemoryReusable,
                    collectMissingIngredients, recipeOutputQuantity);
            if (!simulate) {
                tx.commit();
            }
            return result;
        }
    }

    public static <T, M> Pair<List<T>, MissingIngredients<T, M>>
    getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage, IngredientComponent<T, M> ingredientComponent,
                              IRecipeDefinition recipe, TransactionContext transaction,
                              IIngredientCollectionMutable<T, M> extractionMemoryReusable,
                              boolean collectMissingIngredients, long recipeOutputQuantity,
                              boolean skipReusableIngredients) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();

        // Quickly return if the storage is empty
        // We can't take this shortcut if we have a reusable ingredient AND extractionMemoryReusable is not empty
        if (storage.getMaxQuantity() == 0 &&
                extractionMemoryReusable.isEmpty() &&
                IntStream.range(0, recipe.getInputs(ingredientComponent).size())
                        .noneMatch(i -> recipe.isInputReusable(ingredientComponent, i))) {
            if (collectMissingIngredients) {
                List<IPrototypedIngredientAlternatives<T, M>> recipeInputs = recipe.getInputs(ingredientComponent);
                MissingIngredients<T, M> missing = new MissingIngredients<>(recipeInputs.stream().map(IPrototypedIngredientAlternatives::getAlternatives)
                        .map(l -> multiplyPrototypedIngredients(l, recipeOutputQuantity)) // If the input is reusable, don't multiply the expected input quantity
                        .map(ps -> new MissingIngredients.Element<>(ps
                                .stream()
                                .map(p -> new MissingIngredients.PrototypedWithRequested<>(p, matcher.getQuantity(p.getPrototype())))
                                .collect(Collectors.toList()), false)
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
        List<IPrototypedIngredientAlternatives<T, M>> inputAlternativePrototypes = recipe.getInputs(ingredientComponent);
        List<T> inputInstances = Lists.newArrayList();
        List<MissingIngredients.Element<T, M>> missingElements =
                collectMissingIngredients ? Lists.newArrayList() : null;
        for (int inputIndex = 0; inputIndex < inputAlternativePrototypes.size(); inputIndex++) {
            IPrototypedIngredientAlternatives<T, M> inputPrototypes = inputAlternativePrototypes.get(inputIndex);

            // If reusable ingredients should be skipped, treat them as empty (not needed yet).
            // This is used when a job has dependencies, so that reusable ingredients remain available
            // for other jobs to use, and will be extracted lazily in the update loop.
            if (skipReusableIngredients && recipe.isInputReusable(ingredientComponent, inputIndex)) {
                inputInstances.add(matcher.getEmptyInstance());
                continue;
            }

            T firstInputInstance = null;
            boolean setFirstInputInstance = false;
            T inputInstance = null;
            boolean hasInputInstance = false;
            IIngredientCollectionMutable<T, M> extractionMemoryReusableBufferFirst = null;

            // Iterate over all alternatives for this input slot, and take the first matching ingredient.
            List<MissingIngredients.PrototypedWithRequested<T, M>> missingAlternatives = Lists.newArrayList();
            // Track the first partial-success alternative to re-extract into the main transaction after the loop.
            // This prevents "alternative pollution" where probing an alternative consumes items that
            // would have been needed by subsequent alternatives or other slots.
            IPrototypedIngredient<T, M> chosenPartialAlternative = null;
            M chosenPartialMatchCondition = null;
            boolean chosenPartialIsReusable = false;
            for (IPrototypedIngredient<T, M> inputPrototype : inputPrototypes.getAlternatives()) {
                boolean inputReusable = recipe.isInputReusable(ingredientComponent, inputIndex);
                IIngredientCollectionMutable<T, M> extractionMemoryReusableBuffer = inputReusable ? new IngredientHashSet<>(ingredientComponent) : null;
                boolean shouldBreak = false;

                // Multiply required prototype if recipe quantity is higher than one, AND if the input is NOT reusable.
                if (recipeOutputQuantity > 1 && !inputReusable) {
                    inputPrototype = multiplyPrototypedIngredient(inputPrototype, recipeOutputQuantity);
                }

                // If the prototype is empty, we can skip network extraction
                if (matcher.isEmpty(inputPrototype.getPrototype())) {
                    inputInstance = inputPrototype.getPrototype();
                    hasInputInstance = true;
                    setFirstInputInstance = true;
                    firstInputInstance = inputInstance;
                    break;
                }

                long prototypeQuantity = matcher.getQuantity(inputPrototype.getPrototype());
                if (inputReusable && extractionMemoryReusable.contains(inputPrototype.getPrototype())) {
                    // If the reusable item has been extracted before, mark as valid, and don't extract again.
                    inputInstance = inputPrototype.getComponent().getMatcher().getEmptyInstance();
                    hasInputInstance = true;
                    shouldBreak = true;
                    setFirstInputInstance = true;
                    firstInputInstance = inputInstance;
                    extractionMemoryReusableBufferFirst = extractionMemoryReusableBuffer;
                } else {
                    M matchCondition = matcher.withoutCondition(inputPrototype.getCondition(),
                            ingredientComponent.getPrimaryQuantifier().getMatchCondition());
                    // Each alternative is probed in a nested transaction to prevent "alternative pollution":
                    // if alternative A partially extracts items, alternative B should not see reduced storage.
                    // Only the CHOSEN alternative's extraction is committed into the main transaction.
                    T altExtracted;
                    long quantityExtracted;
                    try (Transaction altTx = Transaction.open(transaction)) {
                        if (storage instanceof IngredientChannelAdapter)
                            ((IngredientChannelAdapter) storage).disableLimits();
                        altExtracted = storage.extract(inputPrototype.getPrototype(), matchCondition, altTx);
                        if (storage instanceof IngredientChannelAdapter)
                            ((IngredientChannelAdapter) storage).enableLimits();
                        quantityExtracted = matcher.getQuantity(altExtracted);
                        if (quantityExtracted == prototypeQuantity) {
                            altTx.commit(); // Full success - commit this alternative's extraction
                        }
                        // Else: partial or zero - altTx is rolled back on close
                    }
                    if (quantityExtracted == prototypeQuantity) {
                        // Full success - altExtracted was committed into the main transaction
                        inputInstance = altExtracted;
                        hasInputInstance = true;
                        shouldBreak = true;
                        setFirstInputInstance = true;
                        firstInputInstance = inputInstance;
                        if (inputReusable) {
                            extractionMemoryReusableBuffer.add(inputPrototype.getPrototype());
                            extractionMemoryReusableBufferFirst = extractionMemoryReusableBuffer;
                        } else {
                            extractionMemoryReusableBufferFirst = null;
                        }
                    } else if (collectMissingIngredients) {
                        long quantityMissing = prototypeQuantity - quantityExtracted;
                        missingAlternatives.add(new MissingIngredients.PrototypedWithRequested<>(inputPrototype, quantityMissing));
                        // Track the first partial alternative (quantityExtracted > 0) for re-extraction after loop
                        if (quantityExtracted > 0 && chosenPartialAlternative == null) {
                            chosenPartialAlternative = inputPrototype;
                            chosenPartialMatchCondition = matchCondition;
                            chosenPartialIsReusable = inputReusable;
                        }
                    }
                }

                if (shouldBreak) {
                    break;
                }
            }

            // If no alternative fully succeeded but a partial was available,
            // re-extract the first partial alternative into the main transaction.
            if (!hasInputInstance && chosenPartialAlternative != null) {
                if (storage instanceof IngredientChannelAdapter)
                    ((IngredientChannelAdapter) storage).disableLimits();
                firstInputInstance = storage.extract(chosenPartialAlternative.getPrototype(), chosenPartialMatchCondition, transaction);
                if (storage instanceof IngredientChannelAdapter)
                    ((IngredientChannelAdapter) storage).enableLimits();
                setFirstInputInstance = true;
            }

            if (extractionMemoryReusableBufferFirst != null) {
                for (T instance : extractionMemoryReusableBufferFirst) {
                    extractionMemoryReusable.add(instance);
                }
            }

            // If none of the alternatives were found, fail immediately
            if (!hasInputInstance) {
                if (!collectMissingIngredients) {
                    // This input failed, return immediately.
                    return Pair.of(null, null);
                } else {
                    // Multiply missing collection if recipe quantity is higher than one
                    if (missingAlternatives.size() > 0) {
                        missingElements.add(new MissingIngredients.Element<>(missingAlternatives, recipe.isInputReusable(ingredientComponent, inputIndex)));
                    }
                }
            }

            // Otherwise, append it to the list and carry on.
            // If none of the instances were valid, we add the first partially valid instance.
            if (hasInputInstance) {
                inputInstances.add(inputInstance);
            } else if (setFirstInputInstance && !matcher.isEmpty(firstInputInstance)) {
                inputInstances.add(firstInputInstance);
            }
        }

        return Pair.of(
                inputInstances,
                collectMissingIngredients ? new MissingIngredients<>(missingElements) : null
        );
    }

    /**
     * Get all required recipe input ingredients from the network.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * @param network The target network.
     * @param channel The target channel.
     * @param recipe The recipe to get the inputs from.
     * @param transaction The transaction context.
     * @param recipeOutputQuantity The number of times the given recipe should be applied.
     * @return The found ingredients or null.
     */
    @Nullable
    public static IMixedIngredients getRecipeInputs(INetwork network, int channel,
                                                    IRecipeDefinition recipe, TransactionContext transaction,
                                                    long recipeOutputQuantity) {
        Map<IngredientComponent<?, ?>, List<?>> inputs = getRecipeInputs(getNetworkStorageGetter(network, channel, true),
                recipe, transaction, Maps.newIdentityHashMap(), false, recipeOutputQuantity).getLeft();
        return inputs == null ? null : new MixedIngredients(inputs);
    }

    /**
     * @deprecated Use {@link #getRecipeInputs(INetwork, int, IRecipeDefinition, TransactionContext, long)} instead.
     */
    @Deprecated // TODO: remove in next major
    @Nullable
    public static IMixedIngredients getRecipeInputs(INetwork network, int channel,
                                                    IRecipeDefinition recipe, boolean simulate,
                                                    long recipeOutputQuantity) {
        try (Transaction tx = Transaction.openRoot()) {
            IMixedIngredients result = getRecipeInputs(network, channel, recipe, tx, recipeOutputQuantity);
            if (!simulate) {
                tx.commit();
            }
            return result;
        }
    }

    /**
     * Get all required recipe input ingredients from the crafting job's buffer.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * @param craftingJob The crafting job.
     * @param recipe The recipe to get the inputs from.
     * @param transaction The transaction context.
     * @param recipeOutputQuantity The number of times the given recipe should be applied.
     * @return The found ingredients or null.
     */
    @Nullable
    public static IMixedIngredients getRecipeInputsFromCraftingJobBuffer(CraftingJob craftingJob,
                                                    IRecipeDefinition recipe, TransactionContext transaction,
                                                    long recipeOutputQuantity) {
        Map<IngredientComponent<?, ?>, List<?>> inputs = getRecipeInputs(getCraftingJobBufferStorageGetter(craftingJob),
                recipe, transaction, null, false, recipeOutputQuantity).getLeft();
        return inputs == null ? null : new MixedIngredients(inputs);
    }

    /**
     * @deprecated Use {@link #getRecipeInputsFromCraftingJobBuffer(CraftingJob, IRecipeDefinition, TransactionContext, long)} instead.
     */
    @Deprecated // TODO: remove in next major
    @Nullable
    public static IMixedIngredients getRecipeInputsFromCraftingJobBuffer(CraftingJob craftingJob,
                                                    IRecipeDefinition recipe, boolean simulate,
                                                    long recipeOutputQuantity) {
        try (Transaction tx = Transaction.openRoot()) {
            IMixedIngredients result = getRecipeInputsFromCraftingJobBuffer(craftingJob, recipe, tx, recipeOutputQuantity);
            if (!simulate) {
                tx.commit();
            }
            return result;
        }
    }

    /**
     * Create a callback function for getting a storage for an ingredient component from the given network channel.
     * @param network The target network.
     * @param channel The target channel.
     * @param scheduleObservation If an observation inside the ingredients network should be scheduled.
     * @return A callback function for getting a storage for an ingredient component.
     */
    public static Function<IngredientComponent<?, ?>, IIngredientComponentStorage> getNetworkStorageGetter(INetwork network, int channel, boolean scheduleObservation) {
        return ingredientComponent -> getNetworkStorage(network, channel, ingredientComponent, scheduleObservation);
    }

    /**
     * Create a callback function for getting a storage for an ingredient component from the given crafting job buffer.
     * @param craftingJob The crafting job.
     * @return A callback function for getting a storage for an ingredient component.
     */
    public static Function<IngredientComponent<?, ?>, IIngredientComponentStorage> getCraftingJobBufferStorageGetter(CraftingJob craftingJob) {
        return ingredientComponent -> {
            List<?> list = craftingJob.getIngredientsStorageBuffer().getInstances(ingredientComponent);
            return new IngredientComponentStorageSlottedCollectionWrapper<>(new IngredientList(ingredientComponent, list), Integer.MAX_VALUE, Integer.MAX_VALUE);
        };
    }

    /**
     * Get all required recipe input ingredients based on a given storage callback.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * @param storageGetter A callback function to get a storage for the given ingredient component.
     * @param recipe The recipe to get the inputs from.
     * @param transaction The transaction context.
     * @param extractionMemoriesReusable This map remembers all extracted reusable instances in simulation mode.
     *                                   This is to make sure that instances can not be extracted multiple times
     *                                   when simulating.
     *                                   The quantities can also go negatives,
     *                                   which means that a surplus of the given instance is present,
     *                                   which will be used up first before a call to the storage.
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
    getRecipeInputs(Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter, IRecipeDefinition recipe, TransactionContext transaction,
                    Map<IngredientComponent<?, ?>, IIngredientCollectionMutable<?, ?>> extractionMemoriesReusable,
                    boolean collectMissingIngredients, long recipeOutputQuantity) {
        return getRecipeInputs(storageGetter, recipe, transaction, extractionMemoriesReusable,
                collectMissingIngredients, recipeOutputQuantity, false);
    }

    /**
     * @deprecated Use {@link #getRecipeInputs(Function, IRecipeDefinition, TransactionContext, Map, boolean, long)} instead.
     */
    @Deprecated // TODO: remove in next major
    public static Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>>
    getRecipeInputs(Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter, IRecipeDefinition recipe, boolean simulate,
                    Map<IngredientComponent<?, ?>, IIngredientCollectionMutable<?, ?>> extractionMemoriesReusable,
                    boolean collectMissingIngredients, long recipeOutputQuantity) {
        try (Transaction tx = Transaction.openRoot()) {
            Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> result =
                    getRecipeInputs(storageGetter, recipe, tx,
                            extractionMemoriesReusable,
                            collectMissingIngredients, recipeOutputQuantity);
            if (!simulate) {
                tx.commit();
            }
            return result;
        }
    }

    public static Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>>
    getRecipeInputs(Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter, IRecipeDefinition recipe, TransactionContext transaction,
                    @Nullable Map<IngredientComponent<?, ?>, IIngredientCollectionMutable<?, ?>> extractionMemoriesReusable, // TODO: is this really nullable?
                    boolean collectMissingIngredients, long recipeOutputQuantity, boolean skipReusableIngredients) {
        // Determine available and missing ingredients
        Map<IngredientComponent<?, ?>, List<?>> ingredientsAvailable = Maps.newIdentityHashMap();
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> ingredientsMissing = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> ingredientComponent : recipe.getInputComponents()) {
            IIngredientComponentStorage storage = storageGetter.apply(ingredientComponent);
            IIngredientCollectionMutable extractionMemoryReusable;
            if (extractionMemoriesReusable != null) {
                extractionMemoryReusable = extractionMemoriesReusable.get(ingredientComponent);
                if (extractionMemoryReusable == null) {
                    extractionMemoryReusable = new IngredientHashSet<>(ingredientComponent);
                    extractionMemoriesReusable.put(ingredientComponent, extractionMemoryReusable);
                }
            } else {
                extractionMemoryReusable = new IngredientHashSet<>(ingredientComponent);
            }
            Pair<List<?>, MissingIngredients<?, ?>> subIngredients = getIngredientRecipeInputs(storage,
                    (IngredientComponent) ingredientComponent, recipe, transaction, extractionMemoryReusable,
                    collectMissingIngredients, recipeOutputQuantity, skipReusableIngredients);
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

        // Compress missing ingredients
        // We do this to ensure that instances missing multiple times can be easily combined
        // when triggering a crafting job for them.
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> ingredientsMissingCompressed = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> ingredientComponent : ingredientsMissing.keySet()) {
            ingredientsMissingCompressed.put(ingredientComponent, compressMissingIngredients(ingredientsMissing.get(ingredientComponent)));
        }

        return Pair.of(ingredientsAvailable, ingredientsMissingCompressed);
    }

    /**
     * @deprecated Use {@link #getRecipeInputs(Function, IRecipeDefinition, TransactionContext, Map, boolean, long, boolean)} instead.
     */
    @Deprecated // TODO: remove in next major
    public static Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>>
    getRecipeInputs(Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter, IRecipeDefinition recipe, boolean simulate,
                    Map<IngredientComponent<?, ?>, IIngredientCollectionMutable<?, ?>> extractionMemoriesReusable,
                    boolean collectMissingIngredients, long recipeOutputQuantity, boolean skipReusableIngredients) {
        try (Transaction tx = Transaction.openRoot()) {
            Pair<Map<IngredientComponent<?, ?>, List<?>>, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>>> result =
                    getRecipeInputs(storageGetter, recipe, tx,
                            extractionMemoriesReusable,
                            collectMissingIngredients, recipeOutputQuantity, skipReusableIngredients);
            if (!simulate) {
                tx.commit();
            }
            return result;
        }
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
     * Compress the given missing ingredients so that equal instances just have an incremented quantity.
     *
     * @param missingIngredients The missing ingredients.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return A new missing ingredients object.
     */
    public static <T, M> MissingIngredients<T, M> compressMissingIngredients(MissingIngredients<T, M> missingIngredients) {
        // Index identical missing ingredients in a map, to group them by quantity
        Map<MissingIngredients.Element<T, M>, Long> elementsCompressedMap = Maps.newLinkedHashMap(); // Must be a linked map to maintain our order!!!
        for (MissingIngredients.Element<T, M> element : missingIngredients.getElements()) {
            elementsCompressedMap.merge(element, 1L, Long::sum);
        }

        // Create a new missing ingredients list where we multiply the missing quantities
        List<MissingIngredients.Element<T, M>> elementsCompressed = Lists.newArrayList();
        for (Map.Entry<MissingIngredients.Element<T, M>, Long> entry : elementsCompressedMap.entrySet()) {
            Long quantity = entry.getValue();
            if (quantity == 1L || entry.getKey().isInputReusable()) {
                elementsCompressed.add(entry.getKey());
            } else {
                MissingIngredients.Element<T, M> elementOld = entry.getKey();
                MissingIngredients.Element<T, M> elementNewQuantity = new MissingIngredients.Element<>(
                        elementOld.getAlternatives().stream()
                                .map(alt -> new MissingIngredients.PrototypedWithRequested<>(alt.getRequestedPrototype(), alt.getQuantityMissing() * quantity))
                                .toList(),
                        elementOld.isInputReusable()
                );
                elementsCompressed.add(elementNewQuantity);
            }
        }
        return new MissingIngredients<>(elementsCompressed);
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
    public static <T, M> List<IPrototypedIngredient<T, M>> multiplyPrototypedIngredients(Collection<IPrototypedIngredient<T, M>> prototypedIngredients,
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
     * Merge two mixed ingredients in a new mixed ingredients object.
     * Instances will be stacked.
     * @param a A first mixed ingredients object.
     * @param b A second mixed ingredients object.
     * @return A merged mixed ingredients object.
     */
    public static IMixedIngredients mergeMixedIngredients(IMixedIngredients a, IMixedIngredients b) {
        // Temporarily store instances in IngredientCollectionPrototypeMaps
        Map<IngredientComponent<?, ?>, IngredientCollectionQuantitativeGrouper<?, ?, IngredientArrayList<?, ?>>> groupings = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> component : a.getComponents()) {
            IngredientCollectionQuantitativeGrouper grouping = new IngredientCollectionQuantitativeGrouper<>(new IngredientArrayList<>(component));
            groupings.put(component, grouping);
            grouping.addAll(a.getInstances(component));
        }
        for (IngredientComponent<?, ?> component : b.getComponents()) {
            IngredientCollectionQuantitativeGrouper grouping = groupings.get(component);
            if (grouping == null) {
                grouping = new IngredientCollectionQuantitativeGrouper<>(new IngredientArrayList<>(component));
                groupings.put(component, grouping);
            }
            grouping.addAll(b.getInstances(component));
        }

        // Convert IngredientCollectionPrototypeMaps to lists
        Map<IngredientComponent<?, ?>, List<?>> ingredients = Maps.newIdentityHashMap();
        for (Map.Entry<IngredientComponent<?, ?>, IngredientCollectionQuantitativeGrouper<?, ?, IngredientArrayList<?, ?>>> entry : groupings.entrySet()) {
            ingredients.put(entry.getKey(), Lists.newArrayList(entry.getValue()));
        }
        return new MixedIngredients(ingredients);
    }

    /**
     * Stack all ingredients in the given mixed ingredients object.
     * @param mixedIngredients A mixed ingredients object.
     * @return A new mixed ingredients object.
     */
    protected static IMixedIngredients compressMixedIngredients(IMixedIngredients mixedIngredients) {
        // Temporarily store instances in IngredientCollectionPrototypeMaps
        Map<IngredientComponent<?, ?>, IngredientCollectionQuantitativeGrouper<?, ?, IngredientArrayList<?, ?>>> groupings = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> component : mixedIngredients.getComponents()) {
            IngredientCollectionQuantitativeGrouper grouping = new IngredientCollectionQuantitativeGrouper<>(new IngredientArrayList<>(component));
            groupings.put(component, grouping);
            grouping.addAll(mixedIngredients.getInstances(component));
        }

        // Convert IngredientCollectionPrototypeMaps to lists
        Map<IngredientComponent<?, ?>, List<?>> ingredients = Maps.newIdentityHashMap();
        for (Map.Entry<IngredientComponent<?, ?>, IngredientCollectionQuantitativeGrouper<?, ?, IngredientArrayList<?, ?>>> entry : groupings.entrySet()) {
            IIngredientMatcher matcher = entry.getKey().getMatcher();
            List<?> values = entry.getValue()
                    .stream()
                    .filter(i -> !matcher.isEmpty(i))
                    .collect(Collectors.toList());
            if (!values.isEmpty()) {
                ingredients.put(entry.getKey(), values);
            }
        }
        return new MixedIngredients(ingredients);
    }

    /**
     * Insert the ingredients of the given ingredient component type into the target to make it start crafting.
     *
     * If insertion in non-simulation mode fails,
     * ingredients will be re-inserted into the network.
     *
     * @param ingredientComponent The ingredient component type.
     * @param capabilityType capabilityType The type of capability, usually Block.class, Item.class, or Entity.class.
     * @param capabilityGetter A capability provider.
     * @param capabilityTarget The capability target for error reporting.
     * @param side The target side.
     * @param ingredients The ingredients to insert.
     * @param storageFallback The storage to insert any failed ingredients back into. Can be null in simulation mode.
     * @param transaction The transaction context.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return If all instances could be inserted.
     */
    public static <T, M, C> boolean insertIngredientCrafting(IngredientComponent<T, M> ingredientComponent,
                                                          Class<?> capabilityType,
                                                          ICapabilityGetter<Direction> capabilityGetter,
                                                          Object capabilityTarget,
                                                          @Nullable Direction side,
                                                          IMixedIngredients ingredients,
                                                          IIngredientComponentStorage<T, M> storageFallback,
                                                          TransactionContext transaction) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        IIngredientComponentStorage<T, M> storage = ingredientComponent.getStorage(capabilityType, capabilityGetter, side);
        List<T> instances = ingredients.getInstances(ingredientComponent);
        boolean ok = true;
        for (T instance : instances) {
            T remaining = storage.insert(instance, transaction);
            if (!matcher.isEmpty(remaining)) {
                ok = false;
            }
        }

        return ok;
    }

    /**
     * @deprecated Use {@link #insertIngredientCrafting(IngredientComponent, Class, ICapabilityGetter, Object, Direction, IMixedIngredients, IIngredientComponentStorage, TransactionContext)} instead.
     */
    @Deprecated // TODO: remove in next major
    public static <T, M, C> boolean insertIngredientCrafting(IngredientComponent<T, M> ingredientComponent,
                                                          Class<?> capabilityType,
                                                          ICapabilityGetter<Direction> capabilityGetter,
                                                          Object capabilityTarget,
                                                          @Nullable Direction side,
                                                          IMixedIngredients ingredients,
                                                          IIngredientComponentStorage<T, M> storageFallback,
                                                          boolean simulate) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        IIngredientComponentStorage<T, M> storage = ingredientComponent.getStorage(capabilityType, capabilityGetter, side);
        List<T> instances = ingredients.getInstances(ingredientComponent);
        List<T> failedInstances = Lists.newArrayList();
        boolean ok = true;
        for (T instance : instances) {
            T remaining = storage.insert(instance, simulate);
            if (!matcher.isEmpty(remaining)) {
                ok = false;
                if (!simulate) {
                    failedInstances.add(remaining);
                }
            }
        }

        // If we had failed insertions, try to insert them back into the network.
        for (T instance : failedInstances) {
            T remaining = storageFallback.insert(instance, false);
            if (!matcher.isEmpty(remaining)) {
                throw new IllegalStateException(String.format("Insertion for a crafting recipe failed " +
                        "due to inconsistent insertion behaviour by destination in simulation " +
                        "and non-simulation: %s. Lost: %s", capabilityTarget, instances));
            }
        }

        return ok;
    }

    /**
     * Insert the ingredients of all applicable ingredient component types into the target to make it start crafting.
     *
     * If insertion in non-simulation mode fails,
     * ingredients will be re-inserted into the network.
     *
     * @param targetGetter A function to get the target position.
     * @param ingredients The ingredients to insert.
     * @param network The network.
     * @param channel The channel.
     * @param transaction The transaction context.
     * @return If all instances could be inserted.
     */
    public static boolean insertCrafting(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                                         IMixedIngredients ingredients,
                                         INetwork network, int channel,
                                         TransactionContext transaction) {
        Map<IngredientComponent<?, ?>, BlockEntity> tileMap = Maps.newIdentityHashMap();

        // First, check if we can find valid tiles for all ingredient components
        for (IngredientComponent<?, ?> ingredientComponent : ingredients.getComponents()) {
            DimPos dimPos = targetGetter.apply(ingredientComponent).getPos();
            BlockEntity tile = IModHelpers.get().getBlockEntityHelpers().get(dimPos.getLevel(true), dimPos.getBlockPos(), BlockEntity.class).orElse(null);
            if (tile != null) {
                tileMap.put(ingredientComponent, tile);
            } else {
                return false;
            }
        }

        // Next, insert the instances into the respective tiles
        boolean ok = true;
        for (Map.Entry<IngredientComponent<?, ?>, BlockEntity> entry : tileMap.entrySet()) {
            if (!insertIngredientCrafting((IngredientComponent) entry.getKey(),
                    Block.class, ICapabilityGetter.forBlockEntity(entry.getValue()), entry.getValue(),
                    targetGetter.apply(entry.getKey()).getSide(), ingredients,
                    null, transaction)) {
                ok = false;
            }
        }

        return ok;
    }

    /**
     * @deprecated Use {@link #insertCrafting(Function, IMixedIngredients, INetwork, int, TransactionContext)} instead.
     */
    @Deprecated // TODO: remove in next major
    public static boolean insertCrafting(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                                         IMixedIngredients ingredients,
                                         INetwork network, int channel,
                                         boolean simulate) {
        Map<IngredientComponent<?, ?>, BlockEntity> tileMap = Maps.newIdentityHashMap();

        // First, check if we can find valid tiles for all ingredient components
        for (IngredientComponent<?, ?> ingredientComponent : ingredients.getComponents()) {
            DimPos dimPos = targetGetter.apply(ingredientComponent).getPos();
            BlockEntity tile = IModHelpers.get().getBlockEntityHelpers().get(dimPos.getLevel(true), dimPos.getBlockPos(), BlockEntity.class).orElse(null);
            if (tile != null) {
                tileMap.put(ingredientComponent, tile);
            } else {
                return false;
            }
        }

        // Next, insert the instances into the respective tiles
        boolean ok = true;
        for (Map.Entry<IngredientComponent<?, ?>, BlockEntity> entry : tileMap.entrySet()) {
            IIngredientComponentStorage<?, ?> storageNetwork = simulate ? null : getNetworkStorage(network, channel, entry.getKey(), false);
            if (!insertIngredientCrafting((IngredientComponent) entry.getKey(),
                    Block.class, ICapabilityGetter.forBlockEntity(entry.getValue()), entry.getValue(),
                    targetGetter.apply(entry.getKey()).getSide(), ingredients,
                    storageNetwork, simulate)) {
                ok = false;
            }
        }

        return ok;
    }

    /**
     * Split the given crafting job amount into new jobs with a given split factor.
     * @param craftingJob A crafting job to split.
     * @param splitFactor The number of jobs to split the job over.
     * @param dependencyGraph The dependency graph that will be updated if there are dependencies in the original job.
     * @param identifierGenerator An identifier generator for the crafting jobs ids.
     * @return The newly created crafting jobs.
     */
    public static List<CraftingJob> splitCraftingJobs(CraftingJob craftingJob, int splitFactor,
                                                      CraftingJobDependencyGraph dependencyGraph,
                                                      IIdentifierGenerator identifierGenerator) {
        splitFactor = Math.min(splitFactor, craftingJob.getAmount());
        int division = craftingJob.getAmount() / splitFactor;
        int modulus = craftingJob.getAmount() % splitFactor;

        // Clone original job into splitFactor jobs
        List<CraftingJob> newCraftingJobs = Lists.newArrayList();
        for (int i = 0; i < splitFactor; i++) {
            CraftingJob clonedJob = craftingJob.clone(identifierGenerator);
            newCraftingJobs.add(clonedJob);

            // Update amount
            int newAmount = division;
            if (modulus > 0) {
                // No amounts will be lost, as modulus is guaranteed to be smaller than splitFactor
                newAmount++;
                modulus--;
            }
            clonedJob.setAmount(newAmount);
        }

        // Collect dependency links
        Collection<CraftingJob> originalDependencies = dependencyGraph.getDependencies(craftingJob);
        Collection<CraftingJob> originalDependents = dependencyGraph.getDependents(craftingJob);

        // Remove dependency links to and from the original jobs
        for (CraftingJob dependency : originalDependencies) {
            craftingJob.removeDependency(dependency);
            dependencyGraph.removeDependency(craftingJob.getId(), dependency.getId());
        }
        for (CraftingJob dependent : originalDependents) {
            dependent.removeDependency(craftingJob);
            dependencyGraph.removeDependency(dependent.getId(), craftingJob.getId());
        }

        // Create dependency links to and from the new crafting jobs
        for (CraftingJob dependency : originalDependencies) {
            for (CraftingJob newCraftingJob : newCraftingJobs) {
                newCraftingJob.addDependency(dependency);
                dependencyGraph.addDependency(newCraftingJob, dependency);
            }
        }
        for (CraftingJob originalDependent : originalDependents) {
            for (CraftingJob newCraftingJob : newCraftingJobs) {
                originalDependent.addDependency(newCraftingJob);
                dependencyGraph.addDependency(originalDependent, newCraftingJob);
            }
        }

        return newCraftingJobs;
    }

    /**
     * Insert the given ingredients into the given storage networks.
     * @param ingredients A collection of ingredients.
     * @param storageGetter A storage network getter.
     * @param transaction The transaction context.
     * @return The remaining ingredients that were not inserted.
     */
    public static IMixedIngredients insertIngredients(IMixedIngredients ingredients,
                                                      Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
                                                      TransactionContext transaction) {
        Map<IngredientComponent<?, ?>, List<?>> remainingIngredients = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> component : ingredients.getComponents()) {
            IIngredientComponentStorage storage = storageGetter.apply(component);
            IIngredientMatcher matcher = component.getMatcher();
            for (Object instance : ingredients.getInstances(component)) {
                Object remainder = storage.insert(instance, transaction);
                if (!matcher.isEmpty(remainder)) {
                    List remainingInstances = remainingIngredients.get(component);
                    if (remainingInstances == null) {
                        remainingInstances = Lists.newArrayList();
                        remainingIngredients.put(component, remainingInstances);
                    }
                    remainingInstances.add(instance);
                }
            }
        }
        return new MixedIngredients(remainingIngredients);
    }

    /**
     * @deprecated Use {@link #insertIngredients(IMixedIngredients, Function, TransactionContext)} instead.
     */
    @Deprecated // TODO: remove in next major
    public static IMixedIngredients insertIngredients(IMixedIngredients ingredients,
                                                      Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
                                                      boolean simulate) {
        try (Transaction tx = Transaction.openRoot()) {
            IMixedIngredients result = insertIngredients(ingredients, storageGetter, tx);
            if (!simulate) {
                tx.commit();
            }
            return result;
        }
    }

    /**
     * Insert the given ingredients into the given storage networks.
     * If something fails to be inserted, produce a warning.
     * @param ingredients A collection of ingredients.
     * @param storageGetter A storage network getter.
     * @param failureSink Ingredients that failed to be inserted will be inserted to this sink.
     */
    public static void insertIngredientsGuaranteed(IMixedIngredients ingredients,
                                                   Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter,
                                                   ICraftingResultsSink failureSink) {
        IMixedIngredients remaining;
        try (Transaction tx = Transaction.openRoot()) {
            remaining = insertIngredients(ingredients, storageGetter, tx);
            tx.commit();
        }
        // If re-insertion into the network fails, insert it into the buffer of the crafting interface,
        // to avoid loss of ingredients.
        if (!remaining.isEmpty()) {
            for (IngredientComponent<?, ?> component : remaining.getComponents()) {
                for (Object instance : remaining.getInstances(component)) {
                    failureSink.addResult((IngredientComponent) component, instance);
                }
            }
        }
    }

    /**
     * Generates semi-unique IDs.
     */
    public static interface IIdentifierGenerator {
        public int getNext();
    }

}
