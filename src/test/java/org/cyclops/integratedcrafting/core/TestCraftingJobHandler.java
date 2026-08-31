package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.network.CraftingNetwork;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author rubensworks
 */
public class TestCraftingJobHandler {

    private static final int MAX_RECIPE_DURATION_ENTRIES = 32;

    private TickingCraftingJobHandler handler;
    private ICraftingNetwork craftingNetwork;
    private IRecipeDefinition recipeA;
    private IRecipeDefinition recipeB;

    @Before
    public void beforeEach() {
        this.handler = new TickingCraftingJobHandler();
        this.craftingNetwork = new CraftingNetwork();
        this.recipeA = newRecipe(0);
        this.recipeB = newRecipe(1);
    }

    protected static IRecipeDefinition newRecipe(long output) {
        Map<IngredientComponent<?, ?>, List<?>> outputs = Maps.newIdentityHashMap();
        outputs.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(output));
        return new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(outputs));
    }

    protected static Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> newPendingIngredients() {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredients = Maps.newIdentityHashMap();
        pendingIngredients.put(IngredientComponentStubs.SIMPLE, Lists.<IPrototypedIngredient<?, ?>>newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.SIMPLE, 1L, true)));
        return pendingIngredients;
    }

    protected CraftingJob newCraftingJob(int id, int amount) {
        return new CraftingJob(id, 0, recipeA, amount, new MixedIngredients(Maps.newIdentityHashMap()));
    }

    @Test
    public void testRecipeDurationUnknown() {
        assertThat(handler.getEstimatedRecipeDuration(recipeA), equalTo(-1L));
    }

    @Test
    public void testRecipeDurationSingle() {
        handler.reportRecipeDuration(recipeA, 100);
        assertThat(handler.getEstimatedRecipeDuration(recipeA), equalTo(100L));
    }

    @Test
    public void testRecipeDurationSmoothed() {
        handler.reportRecipeDuration(recipeA, 100);
        handler.reportRecipeDuration(recipeA, 200);
        assertThat(handler.getEstimatedRecipeDuration(recipeA), equalTo(125L));
        handler.reportRecipeDuration(recipeA, 200);
        assertThat(handler.getEstimatedRecipeDuration(recipeA), equalTo(144L));
    }

    @Test
    public void testRecipeDurationFallsBackToAverage() {
        handler.reportRecipeDuration(recipeA, 100);
        assertThat(handler.getEstimatedRecipeDuration(recipeB), equalTo(100L));
    }

    @Test
    public void testRecipeDurationsAreBounded() {
        for (int i = 0; i < MAX_RECIPE_DURATION_ENTRIES + 10; i++) {
            handler.reportRecipeDuration(newRecipe(i), 100);
        }

        assertThat(handler.getRecipeDurationStatistics().getEntryCount(), equalTo(MAX_RECIPE_DURATION_ENTRIES));
    }

    @Test
    public void testSerializationDoesNotGrowWithRecipes() {
        handler.reportRecipeDuration(recipeA, 100);
        CompoundTag tagSingle = new CompoundTag();
        handler.writeToNBT(null, tagSingle);

        for (int i = 0; i < 100; i++) {
            handler.reportRecipeDuration(newRecipe(i), 100);
        }
        CompoundTag tagMany = new CompoundTag();
        handler.writeToNBT(null, tagMany);

        // Only the average duration is serialized, so crafting more recipes must not grow the crafting interface
        assertThat(tagMany.toString(), equalTo(tagSingle.toString()));
    }

    @Test
    public void testCraftingJobEntryStartTickUnknown() {
        assertThat(handler.getCraftingJobEntryStartTick(0), equalTo(-1L));
    }

    @Test
    public void testCraftingOperationIsMeasured() {
        CraftingJob craftingJob = newCraftingJob(1, 2);

        handler.setCurrentTick(100);
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(100L));

        handler.setCurrentTick(160);
        handler.onCraftingJobEntryFinished(craftingNetwork, 1);

        assertThat(craftingJob.getAmount(), equalTo(1));
        assertThat(craftingJob.getAmountTotal(), equalTo(2));
        assertThat(handler.getEstimatedRecipeDuration(recipeA), equalTo(60L));
        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(-1L));
    }

    @Test
    public void testParallelCraftingOperationsAreMeasured() {
        CraftingJob craftingJob = newCraftingJob(1, 2);

        // Two operations of the same job are running at the same time in non-blocking mode
        handler.setCurrentTick(100);
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        handler.setCurrentTick(120);
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(100L));

        handler.setCurrentTick(200);
        handler.onCraftingJobEntryFinished(craftingNetwork, 1);
        assertThat(handler.getEstimatedRecipeDuration(recipeA), equalTo(100L));
        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(120L));

        handler.setCurrentTick(220);
        handler.onCraftingJobEntryFinished(craftingNetwork, 1);
        assertThat(handler.getEstimatedRecipeDuration(recipeA), equalTo(100L));
        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(-1L));
    }

    @Test
    public void testCraftingOperationsAreForgottenWhenTheJobStopsProcessing() {
        CraftingJob craftingJob = newCraftingJob(1, 1);

        handler.setCurrentTick(100);
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        handler.unmarkCraftingJobProcessing(craftingJob);

        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(-1L));
    }

    @Test
    public void testCraftingOperationsAreForgottenWhenTheJobIsCancelled() {
        CraftingJob craftingJob = newCraftingJob(1, 1);

        handler.setCurrentTick(100);
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        handler.markCraftingJobFinished(1);

        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(-1L));
    }

    @Test
    public void testCraftingOperationsAreForgottenWhenTheJobFinishes() {
        CraftingJob craftingJob = newCraftingJob(1, 1);

        handler.setCurrentTick(100);
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        handler.onCraftingJobFinished(craftingJob);

        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(-1L));
    }

    @Test
    public void testCraftingOperationsAreForgottenWithoutPendingIngredients() {
        CraftingJob craftingJob = newCraftingJob(1, 1);

        handler.setCurrentTick(100);
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, Maps.newIdentityHashMap());

        assertThat(handler.getCraftingJobEntryStartTick(1), equalTo(-1L));
    }

    @Test
    public void testRecipeDurationsSurviveSerialization() {
        handler.reportRecipeDuration(recipeA, 100);

        CompoundTag tag = new CompoundTag();
        handler.writeToNBT(null, tag);

        TickingCraftingJobHandler deserialized = new TickingCraftingJobHandler();
        deserialized.readFromNBT(null, tag);

        assertThat(deserialized.getEstimatedRecipeDuration(recipeA), equalTo(100L));
        assertThat(deserialized.getRecipeDurationStatistics().getEntryCount(), equalTo(0));
    }

    protected static class TickingCraftingJobHandler extends CraftingJobHandler {

        private long currentTick;

        public TickingCraftingJobHandler() {
            super(1, true, Collections.emptyList(), new ICraftingResultsSink() {
                @Override
                public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance) {

                }
            });
        }

        @Override
        protected RecipeDurationStatistics createRecipeDurationStatistics() {
            return new RecipeDurationStatistics(MAX_RECIPE_DURATION_ENTRIES, 24000);
        }

        public void setCurrentTick(long currentTick) {
            this.currentTick = currentTick;
        }

        @Override
        protected long getCurrentTick() {
            return currentTick;
        }
    }

}
