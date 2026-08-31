package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
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

    private CraftingJobHandler handler;
    private IRecipeDefinition recipeA;
    private IRecipeDefinition recipeB;

    @Before
    public void beforeEach() {
        this.handler = new CraftingJobHandler(1, true, Collections.emptyList(), new ICraftingResultsSink() {
            @Override
            public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance) {

            }
        });
        this.recipeA = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(Maps.newIdentityHashMap()));

        Map<IngredientComponent<?, ?>, List<?>> outputB = Maps.newIdentityHashMap();
        outputB.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(1L));
        this.recipeB = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(outputB));
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
    public void testRecipeDurationPerRecipe() {
        handler.reportRecipeDuration(recipeA, 100);
        assertThat(handler.getEstimatedRecipeDuration(recipeB), equalTo(-1L));
    }

    @Test
    public void testCraftingJobEntryStartTickUnknown() {
        assertThat(handler.getCraftingJobEntryStartTick(0), equalTo(-1L));
    }

}
