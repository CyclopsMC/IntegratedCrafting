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
import org.cyclops.cyclopscore.ingredient.collection.IIngredientCollectionMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.cyclopscore.ingredient.storage.IngredientComponentStorageCollectionWrapper;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.RecursiveCraftingRecipeException;
import org.cyclops.integratedcrafting.api.crafting.UnknownCraftingRecipeException;
import org.cyclops.integratedcrafting.ingredient.ComplexStack;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This corresponds to the crafting recipe of the redstone comparator with a single redstone block.
 * @author rubensworks
 */
public class TestCaseJobCalculationRedstoneComparator {

    private static final ComplexStack C_REDSTONE_BLOCK = new ComplexStack(ComplexStack.Group.A, 0, 1, null);
    private static final ComplexStack C_REDSTONE_DUST = new ComplexStack(ComplexStack.Group.A, 1, 1, null);
    private static final ComplexStack C_STICK = new ComplexStack(ComplexStack.Group.A, 2, 1, null);
    private static final ComplexStack C_REDSTONE_TORCH = new ComplexStack(ComplexStack.Group.A, 3, 1, null);
    private static final ComplexStack C_STONE = new ComplexStack(ComplexStack.Group.A, 4, 1, null);
    private static final ComplexStack C_NETHER_QUARTZ = new ComplexStack(ComplexStack.Group.A, 5, 1, null);
    private static final ComplexStack C_REDSTONE_COMPARATOR = new ComplexStack(ComplexStack.Group.A, 6, 1, null);

    private RecipeIndexDefault recipeIndex;
    private IRecipeDefinition recipeRedstoneDust;
    private IRecipeDefinition recipeRedstoneTorch;
    private IRecipeDefinition recipeRedstoneComparator;

    private Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter;
    private Map<IngredientComponent<?, ?>, IIngredientCollectionMutable<?, ?>> simulatedExtractionMemoryReusable;
    private CraftingHelpers.IIdentifierGenerator identifierGenerator;
    private CraftingJobDependencyGraph craftingJobDependencyGraph;
    private Set<IPrototypedIngredient> parentDependencies;

    @BeforeEach
    public void beforeEach() {
        recipeIndex = new RecipeIndexDefault();

        recipeRedstoneDust = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_REDSTONE_BLOCK),
                Lists.newArrayList(C_REDSTONE_DUST.withAmount(9))
        );
        recipeIndex.addRecipe(recipeRedstoneDust);

        recipeRedstoneTorch = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_STICK, C_REDSTONE_DUST),
                Lists.newArrayList(C_REDSTONE_TORCH)
        );
        recipeIndex.addRecipe(recipeRedstoneTorch);

        recipeRedstoneComparator = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_STONE, C_STONE, C_STONE, C_REDSTONE_TORCH, C_REDSTONE_TORCH, C_REDSTONE_TORCH, C_NETHER_QUARTZ),
                Lists.newArrayList(C_REDSTONE_COMPARATOR)
        );
        recipeIndex.addRecipe(recipeRedstoneComparator);

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
        // We have exactly enough for crafting one comparator with a single redstone block that produces 9 redstone dusts that need to be reused for crafting 3 redstone torches
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(C_REDSTONE_BLOCK, false);
        storage.insert(C_STONE.withAmount(3), false);
        storage.insert(C_STICK.withAmount(3), false);
        storage.insert(C_NETHER_QUARTZ, false);
        storageGetter = (c) -> storage;

        CraftingJob jobMain = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, C_REDSTONE_COMPARATOR, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemoryReusable, identifierGenerator, craftingJobDependencyGraph, parentDependencies, true);

        assertThat(jobMain.getId(), equalTo(2));
        assertThat(jobMain.getChannel(), equalTo(0));
        assertThat(jobMain.getAmount(), equalTo(1));
        assertThat(jobMain.getRecipe(), equalTo(recipeRedstoneComparator));
        assertThat(jobMain.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(jobMain.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_STONE.withAmount(3),
                C_NETHER_QUARTZ
        )));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(3));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(jobMain), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(jobMain).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(jobMain).size(), equalTo(0));

        CraftingJob j1 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(jobMain), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).contains(jobMain), equalTo(true));
        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getAmount(), equalTo(3));
        assertThat(j1.getRecipe(), equalTo(recipeRedstoneTorch));
        assertThat(j1.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j1.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_STICK.withAmount(3)
        )));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(1));
        assertThat(j0.getRecipe(), equalTo(recipeRedstoneDust));
        assertThat(j0.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j0.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_REDSTONE_BLOCK
        )));
    }

    @Test
    public void testCraft4Valid() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        // We have exactly enough for crafting one comparator with a single redstone block that produces 9 redstone dusts that need to be reused for crafting 3 redstone torches
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(C_REDSTONE_BLOCK.withAmount(2), false);
        storage.insert(C_STONE.withAmount(12), false);
        storage.insert(C_STICK.withAmount(12), false);
        storage.insert(C_NETHER_QUARTZ.withAmount(4), false);
        storageGetter = (c) -> storage;

        CraftingJob jobMain = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, C_REDSTONE_COMPARATOR.withAmount(4), ComplexStack.Match.EXACT, true,
                simulatedExtractionMemoryReusable, identifierGenerator, craftingJobDependencyGraph, parentDependencies, true);

        assertThat(jobMain.getId(), equalTo(2));
        assertThat(jobMain.getChannel(), equalTo(0));
        assertThat(jobMain.getAmount(), equalTo(4));
        assertThat(jobMain.getRecipe(), equalTo(recipeRedstoneComparator));
        assertThat(jobMain.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(jobMain.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_STONE.withAmount(12),
                C_NETHER_QUARTZ.withAmount(4)
        )));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(3));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(jobMain), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(jobMain).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(jobMain).size(), equalTo(0));

        CraftingJob j1 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(jobMain), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).contains(jobMain), equalTo(true));
        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getAmount(), equalTo(12));
        assertThat(j1.getRecipe(), equalTo(recipeRedstoneTorch));
        assertThat(j1.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j1.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_STICK.withAmount(12)
        )));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(2));
        assertThat(j0.getRecipe(), equalTo(recipeRedstoneDust));
        assertThat(j0.getIngredientsStorage().getComponents().size(), equalTo(1));
        assertThat(j0.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                C_REDSTONE_BLOCK.withAmount(2)
        )));
    }


}
