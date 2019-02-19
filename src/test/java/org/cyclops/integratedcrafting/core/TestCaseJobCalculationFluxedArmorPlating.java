package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.cyclopscore.ingredient.storage.IngredientComponentStorageCollectionWrapper;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.RecursiveCraftingRecipeException;
import org.cyclops.integratedcrafting.api.crafting.UnknownCraftingRecipeException;
import org.cyclops.integratedcrafting.ingredient.ComplexStack;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * This corresponds to the crafting recipe of Redstone Arsenal's Fluxed Armor Plating
 * @author rubensworks
 */
public class TestCaseJobCalculationFluxedArmorPlating {

    private static final ComplexStack C_REDSTONE_ITEM = new ComplexStack(ComplexStack.Group.A, 0, 1, null);
    private static final ComplexStack C_REDSTONE_FLUID = new ComplexStack(ComplexStack.Group.A, 1, 1, null);
    private static final ComplexStack C_ELECTRUM = new ComplexStack(ComplexStack.Group.A, 2, 1, null);
    private static final ComplexStack C_ELECTRUM_FLUXED = new ComplexStack(ComplexStack.Group.A, 3, 1, null);
    private static final ComplexStack C_SAND = new ComplexStack(ComplexStack.Group.A, 4, 1, null);
    private static final ComplexStack C_INGOT = new ComplexStack(ComplexStack.Group.A, 5, 1, null);
    private static final ComplexStack C_PLATE = new ComplexStack(ComplexStack.Group.A, 6, 1, null);
    private static final ComplexStack C_GEM = new ComplexStack(ComplexStack.Group.A, 7, 1, null);
    private static final ComplexStack C_PLATING = new ComplexStack(ComplexStack.Group.A, 8, 1, null);

    private RecipeIndexDefault recipeIndex;
    private IRecipeDefinition recipeRedstoneFluid;
    private IRecipeDefinition recipeElectrumFluxed;
    private IRecipeDefinition recipeIngot;
    private IRecipeDefinition recipePlate;
    private IRecipeDefinition recipePlating;

    private Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter;
    private Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemory;
    private CraftingHelpers.IIdentifierGenerator identifierGenerator;
    private CraftingJobDependencyGraph craftingJobDependencyGraph;
    private Set<IPrototypedIngredient> parentDependencies;

    @Before
    public void beforeEach() {
        recipeIndex = new RecipeIndexDefault();

        recipeRedstoneFluid = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_REDSTONE_ITEM),
                Lists.newArrayList(C_REDSTONE_FLUID.withAmount(100))
        );
        recipeIndex.addRecipe(recipeRedstoneFluid);

        recipeElectrumFluxed = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_ELECTRUM, C_REDSTONE_FLUID.withAmount(500)),
                Lists.newArrayList(C_ELECTRUM_FLUXED)
        );
        recipeIndex.addRecipe(recipeElectrumFluxed);

        recipeIngot = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_ELECTRUM_FLUXED, C_SAND),
                Lists.newArrayList(C_INGOT)
        );
        recipeIndex.addRecipe(recipeIngot);

        recipePlate = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_INGOT),
                Lists.newArrayList(C_PLATE)
        );
        recipeIndex.addRecipe(recipePlate);

        recipePlating = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_PLATE, C_PLATE, C_GEM, C_PLATE, C_PLATE),
                Lists.newArrayList(C_PLATING)
        );
        recipeIndex.addRecipe(recipePlating);

        simulatedExtractionMemory = Maps.newIdentityHashMap();
        Wrapper<Integer> id = new Wrapper<>(0);
        identifierGenerator = () -> {
            int last = id.get();
            id.set(last + 1);
            return last;
        };
        craftingJobDependencyGraph = new CraftingJobDependencyGraph();
        parentDependencies = Sets.newHashSet();
    }

    @Test
    public void testCraft1Valid() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        // We have exactly enough for crafting one plating
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(C_REDSTONE_ITEM.withAmount(20), false);
        storage.insert(C_ELECTRUM.withAmount(4), false);
        storage.insert(C_SAND.withAmount(4), false);
        storage.insert(C_GEM, false);
        storageGetter = (c) -> storage;

        CraftingJob j16 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, C_PLATING, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies, true);

        assertThat(j16.getId(), equalTo(16));
        assertThat(j16.getChannel(), equalTo(0));
        assertThat(j16.getAmount(), equalTo(1));
        assertThat(j16.getRecipe(), equalTo(recipePlating));
        assertThat(j16.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j16.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_GEM
        )));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(5));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(j16), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(j16).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j16).size(), equalTo(0));

        CraftingJob j3 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j16), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j3).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j3).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j3).contains(j16), equalTo(true));
        assertThat(j3.getId(), equalTo(3));
        assertThat(j3.getAmount(), equalTo(4));
        assertThat(j3.getRecipe(), equalTo(recipePlate));
        assertThat(j3.getIngredientsStorage().getComponents().size(), equalTo(0));

        CraftingJob j2 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j3), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j2).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j2).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j2).contains(j3), equalTo(true));
        assertThat(j2.getId(), equalTo(2));
        assertThat(j2.getAmount(), equalTo(4));
        assertThat(j2.getRecipe(), equalTo(recipeIngot));
        assertThat(j2.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j2.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_SAND.withAmount(4)
        )));

        CraftingJob j1 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j2), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).contains(j2), equalTo(true));
        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getAmount(), equalTo(4));
        assertThat(j1.getRecipe(), equalTo(recipeElectrumFluxed));
        assertThat(j1.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j1.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_ELECTRUM.withAmount(4)
        )));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(20));
        assertThat(j0.getRecipe(), equalTo(recipeRedstoneFluid));
        assertThat(j0.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j0.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_REDSTONE_ITEM.withAmount(20)
        )));
    }

    @Test
    public void testCraft1DirectValid() throws RecursiveCraftingRecipeException {
        // We have exactly enough for crafting one plating
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(C_REDSTONE_ITEM.withAmount(20), false);
        storage.insert(C_ELECTRUM.withAmount(4), false);
        storage.insert(C_SAND.withAmount(4), false);
        storage.insert(C_GEM, false);
        storageGetter = (c) -> storage;

        PartialCraftingJobCalculation calculation = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                recipePlating, 1, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies, true);
        assertThat(calculation.getPartialCraftingJobs(), nullValue());
        assertThat(calculation.getIngredientsStorage().keySet().size(), equalTo(1));
        assertThat(calculation.getIngredientsStorage().get(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_GEM
        )));
        assertThat(calculation.getMissingDependencies(), nullValue());
        CraftingJob j16 = calculation.getCraftingJob();

        assertThat(j16.getId(), equalTo(16));
        assertThat(j16.getChannel(), equalTo(0));
        assertThat(j16.getAmount(), equalTo(1));
        assertThat(j16.getRecipe(), equalTo(recipePlating));
        assertThat(j16.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j16.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_GEM
        )));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(5));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(j16), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(j16).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j16).size(), equalTo(0));

        CraftingJob j3 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j16), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j3).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j3).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j3).contains(j16), equalTo(true));
        assertThat(j3.getId(), equalTo(3));
        assertThat(j3.getAmount(), equalTo(4));
        assertThat(j3.getRecipe(), equalTo(recipePlate));
        assertThat(j3.getIngredientsStorage().getComponents().size(), equalTo(0));

        CraftingJob j2 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j3), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j2).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j2).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j2).contains(j3), equalTo(true));
        assertThat(j2.getId(), equalTo(2));
        assertThat(j2.getAmount(), equalTo(4));
        assertThat(j2.getRecipe(), equalTo(recipeIngot));
        assertThat(j2.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j2.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_SAND.withAmount(4)
        )));

        CraftingJob j1 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j2), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).contains(j2), equalTo(true));
        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getAmount(), equalTo(4));
        assertThat(j1.getRecipe(), equalTo(recipeElectrumFluxed));
        assertThat(j1.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j1.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_ELECTRUM.withAmount(4)
        )));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(20));
        assertThat(j0.getRecipe(), equalTo(recipeRedstoneFluid));
        assertThat(j0.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j0.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_REDSTONE_ITEM.withAmount(20)
        )));
    }

    @Test
    public void testCraft4Valid() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        // We have exactly enough for crafting one plating
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(C_REDSTONE_ITEM.withAmount(80), false);
        storage.insert(C_ELECTRUM.withAmount(16), false);
        storage.insert(C_SAND.withAmount(16), false);
        storage.insert(C_GEM.withAmount(4), false);
        storageGetter = (c) -> storage;

        CraftingJob j16 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, C_PLATING.withAmount(4), ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies, true);

        assertThat(j16.getId(), equalTo(16));
        assertThat(j16.getChannel(), equalTo(0));
        assertThat(j16.getAmount(), equalTo(4));
        assertThat(j16.getRecipe(), equalTo(recipePlating));
        assertThat(j16.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j16.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_GEM.withAmount(4)
        )));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(5));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(j16), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(j16).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j16).size(), equalTo(0));

        CraftingJob j3 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j16), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j3).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j3).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j3).contains(j16), equalTo(true));
        assertThat(j3.getId(), equalTo(3));
        assertThat(j3.getAmount(), equalTo(16));
        assertThat(j3.getRecipe(), equalTo(recipePlate));
        assertThat(j3.getIngredientsStorage().getComponents().size(), equalTo(0));

        CraftingJob j2 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j3), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j2).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j2).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j2).contains(j3), equalTo(true));
        assertThat(j2.getId(), equalTo(2));
        assertThat(j2.getAmount(), equalTo(16));
        assertThat(j2.getRecipe(), equalTo(recipeIngot));
        assertThat(j2.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j2.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_SAND.withAmount(16)
        )));

        CraftingJob j1 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j2), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).contains(j2), equalTo(true));
        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getAmount(), equalTo(16));
        assertThat(j1.getRecipe(), equalTo(recipeElectrumFluxed));
        assertThat(j1.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j1.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_ELECTRUM.withAmount(16)
        )));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(80));
        assertThat(j0.getRecipe(), equalTo(recipeRedstoneFluid));
        assertThat(j0.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j0.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_REDSTONE_ITEM.withAmount(80)
        )));
    }


}
