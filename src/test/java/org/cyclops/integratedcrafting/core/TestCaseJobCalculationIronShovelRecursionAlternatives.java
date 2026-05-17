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
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This corresponds to the crafting recipe of an iron shovel that has an iron block recipe available, which should be skipped.
 * @author rubensworks
 */
public class TestCaseJobCalculationIronShovelRecursionAlternatives {

    private static final ComplexStack C_IRON_SHOVEL = new ComplexStack(ComplexStack.Group.A, 0, 1, null);
    private static final ComplexStack C_STICK = new ComplexStack(ComplexStack.Group.A, 1, 1, null);
    private static final ComplexStack C_IRON_INGOT = new ComplexStack(ComplexStack.Group.A, 2, 1, null);
    private static final ComplexStack C_IRON_BLOCK = new ComplexStack(ComplexStack.Group.A, 3, 1, null);
    private static final ComplexStack C_IRON_ORE = new ComplexStack(ComplexStack.Group.A, 4, 1, null);

    private RecipeIndexDefault recipeIndex;
    private IRecipeDefinition recipeShovel;
    private IRecipeDefinition recipeIngotFromBlock;
    private IRecipeDefinition recipeBlockFromIngots;
    private IRecipeDefinition recipeIngotFromOre;

    private Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter;
    private Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemory;
    private Map<IngredientComponent<?, ?>, IIngredientCollectionMutable<?, ?>> simulatedExtractionMemoryReusable;
    private CraftingHelpers.IIdentifierGenerator identifierGenerator;
    private CraftingJobDependencyGraph craftingJobDependencyGraph;
    private Set<IPrototypedIngredient> parentDependencies;

    @BeforeEach
    public void beforeEach() {
        recipeIndex = new RecipeIndexDefault();

        recipeShovel = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_IRON_INGOT.withAmount(2), C_STICK),
                Lists.newArrayList(C_IRON_SHOVEL)
        );
        recipeIndex.addRecipe(recipeShovel);

        recipeIngotFromBlock = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_IRON_BLOCK),
                Lists.newArrayList(C_IRON_INGOT.withAmount(9))
        );
        recipeIndex.addRecipe(recipeIngotFromBlock);

        recipeBlockFromIngots = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_IRON_INGOT.withAmount(9)),
                Lists.newArrayList(C_IRON_BLOCK)
        );
        recipeIndex.addRecipe(recipeBlockFromIngots);

        recipeIngotFromOre = RecipeHelpers.newSimpleRecipe(
                Lists.newArrayList(C_IRON_ORE),
                Lists.newArrayList(C_IRON_INGOT)
        );
        recipeIndex.addRecipe(recipeIngotFromOre);

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
        try (Transaction transaction = Transaction.openRoot()) {
            // We have exactly enough for crafting one comparator with a single redstone block that produces 9 redstone dusts that need to be reused for crafting 3 redstone torches
            IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
            storage.insert(C_IRON_ORE.withAmount(9), transaction);
            storage.insert(C_STICK.withAmount(1), transaction);
            storageGetter = (c) -> storage;
    
            CraftingJob jobMain = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                    IngredientComponentStubs.COMPLEX, C_IRON_SHOVEL, ComplexStack.Match.EXACT, true,
                    simulatedExtractionMemory, simulatedExtractionMemoryReusable, identifierGenerator, craftingJobDependencyGraph, parentDependencies, true, transaction);
    
            assertThat(jobMain.getId(), equalTo(1));
            assertThat(jobMain.getChannel(), equalTo(0));
            assertThat(jobMain.getAmount(), equalTo(1));
            assertThat(jobMain.getRecipe(), equalTo(recipeShovel));
            assertThat(jobMain.getIngredientsStorage().getComponents().size(), equalTo(1));
            assertThat(jobMain.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                    C_STICK
            )));
    
            assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(2));
            assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(jobMain), equalTo(true));
            assertThat(craftingJobDependencyGraph.getDependencies(jobMain).size(), equalTo(1));
            assertThat(craftingJobDependencyGraph.getDependents(jobMain).size(), equalTo(0));
    
            CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(jobMain), null);
            assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
            assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
            assertThat(craftingJobDependencyGraph.getDependents(j0).contains(jobMain), equalTo(true));
            assertThat(j0.getId(), equalTo(0));
            assertThat(j0.getAmount(), equalTo(2));
            assertThat(j0.getRecipe(), equalTo(recipeIngotFromOre));
            assertThat(j0.getIngredientsStorage().getComponents().size(), equalTo(1));
            assertThat(j0.getIngredientsStorage().getInstances(IngredientComponentStubs.COMPLEX), equalTo(Lists.newArrayList(
                    C_IRON_ORE.withAmount(2)
            )));
        }
    }

}
