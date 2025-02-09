package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollectionMutable;
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
import static org.junit.Assert.assertThat;

/**
 * This corresponds to the crafting recipe of the bronze drill head.
 * https://github.com/CyclopsMC/IntegratedCrafting/issues/125
 * @author rubensworks
 */
public class TestCaseJobCalculationBronzeDrillHead {

    private static final ComplexStack C_INGOT = new ComplexStack(ComplexStack.Group.A, 0, 1, null);
    private static final ComplexStack C_BOLT = new ComplexStack(ComplexStack.Group.A, 1, 1, null);
    private static final ComplexStack C_PLATE = new ComplexStack(ComplexStack.Group.A, 2, 1, null);
    private static final ComplexStack C_ROD = new ComplexStack(ComplexStack.Group.A, 3, 1, null);
    private static final ComplexStack C_DUST = new ComplexStack(ComplexStack.Group.A, 4, 1, null);
    private static final ComplexStack C_GEAR = new ComplexStack(ComplexStack.Group.A, 5, 1, null);
    private static final ComplexStack C_CURVED_PLATE = new ComplexStack(ComplexStack.Group.A, 6, 1, null);
    private static final ComplexStack C_RING = new ComplexStack(ComplexStack.Group.A, 7, 1, null);
    private static final ComplexStack C_DRILL_HEAD = new ComplexStack(ComplexStack.Group.A, 8, 1, null);
    private static final ComplexStack C_COPPER_DUST = new ComplexStack(ComplexStack.Group.A, 9, 1, null);
    private static final ComplexStack C_TIN_DUST = new ComplexStack(ComplexStack.Group.A, 10, 1, null);

    private RecipeIndexDefault recipeIndex;
    private IRecipeDefinition recipeRing;
    private IRecipeDefinition recipeGear;
    private IRecipeDefinition recipeDrillHead;
    private IRecipeDefinition recipeDust;
    private IRecipeDefinition recipePlate;
    private IRecipeDefinition recipeCurvedPlate;
    private IRecipeDefinition recipeIngot;
    private IRecipeDefinition recipeRod;
    private IRecipeDefinition recipeBolt;

    private Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter;
    private Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemory;
    private Map<IngredientComponent<?, ?>, IIngredientCollectionMutable<?, ?>> simulatedExtractionMemoryReusable;
    private CraftingHelpers.IIdentifierGenerator identifierGenerator;
    private CraftingJobDependencyGraph craftingJobDependencyGraph;
    private Set<IPrototypedIngredient> parentDependencies;

    @Before
    public void beforeEach() {
        recipeIndex = new RecipeIndexDefault();

        recipeRing = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_ROD),
                Lists.newArrayList(C_RING)
        );
        recipeIndex.addRecipe(recipeRing);

        recipeGear = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_PLATE, C_BOLT, C_PLATE, C_BOLT, C_RING, C_BOLT, C_PLATE, C_BOLT, C_PLATE),
                Lists.newArrayList(C_GEAR)
        );
        recipeIndex.addRecipe(recipeGear);

        recipeDrillHead = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_BOLT, C_CURVED_PLATE, C_PLATE, C_GEAR, C_ROD, C_CURVED_PLATE, C_BOLT, C_GEAR, C_BOLT),
                Lists.newArrayList(C_DRILL_HEAD)
        );
        recipeIndex.addRecipe(recipeDrillHead);

        recipeDust = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_TIN_DUST, C_COPPER_DUST.withAmount(3)),
                Lists.newArrayList(C_DUST.withAmount(4))
        );
        recipeIndex.addRecipe(recipeDust);

        recipePlate = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_INGOT),
                Lists.newArrayList(C_PLATE)
        );
        recipeIndex.addRecipe(recipePlate);

        recipeCurvedPlate = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_PLATE),
                Lists.newArrayList(C_CURVED_PLATE)
        );
        recipeIndex.addRecipe(recipeCurvedPlate);

        recipeIngot = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_DUST),
                Lists.newArrayList(C_INGOT)
        );
        recipeIndex.addRecipe(recipeIngot);

        recipeRod = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_INGOT),
                Lists.newArrayList(C_ROD.withAmount(2))
        );
        recipeIndex.addRecipe(recipeRod);

        recipeBolt = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_ROD),
                Lists.newArrayList(C_BOLT.withAmount(2))
        );
        recipeIndex.addRecipe(recipeBolt);

        simulatedExtractionMemory = Maps.newIdentityHashMap();
        simulatedExtractionMemoryReusable = Maps.newIdentityHashMap();
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
        // This test makes sure that multi-output recipes don't assume the storage contains more items in actually does.
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(C_DUST.withAmount(8), false);
        storage.insert(C_COPPER_DUST.withAmount(64), false);
        storage.insert(C_TIN_DUST.withAmount(64), false);
        storageGetter = (c) -> storage;

        CraftingJob jobMain = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, C_DRILL_HEAD, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, simulatedExtractionMemoryReusable, identifierGenerator, craftingJobDependencyGraph, parentDependencies, true);

        assertThat(jobMain.getId(), equalTo(21));
        assertThat(jobMain.getChannel(), equalTo(0));
        assertThat(jobMain.getAmount(), equalTo(1));
        assertThat(jobMain.getRecipe(), equalTo(recipeDrillHead));
        assertThat(jobMain.getIngredientsStorage().getComponents().size(), equalTo(0));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(22));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(jobMain), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(jobMain).size(), equalTo(5));
        assertThat(craftingJobDependencyGraph.getDependents(jobMain).size(), equalTo(0));

        IMixedIngredients fullStorage = RecipeHelpers.collectIngredientStoragesDependencies(craftingJobDependencyGraph, jobMain);
        assertThat(fullStorage.getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_TIN_DUST.withAmount(2),
                C_COPPER_DUST.withAmount(6),
                C_DUST.withAmount(10) // This is 10 instead of 8, because we have a surplus of 2 that is used by other sub-jobs.
        )));
    }


}
