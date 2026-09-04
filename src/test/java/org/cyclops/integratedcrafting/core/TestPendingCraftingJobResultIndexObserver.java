package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import org.cyclops.integrateddynamics.core.network.IIngredientChannelInsertPreConsumer;
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
public class TestPendingCraftingJobResultIndexObserver {

    private ICraftingNetwork craftingNetwork;
    private IRecipeDefinition recipe;

    @Before
    public void beforeEach() {
        this.craftingNetwork = new CraftingNetwork();
        Map<IngredientComponent<?, ?>, List<?>> outputs = Maps.newIdentityHashMap();
        outputs.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(1L));
        this.recipe = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(outputs));
    }

    protected static Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> newPendingIngredients() {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingIngredients = Maps.newIdentityHashMap();
        pendingIngredients.put(IngredientComponentStubs.SIMPLE, Lists.<IPrototypedIngredient<?, ?>>newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.SIMPLE, 1L, true)));
        return pendingIngredients;
    }

    protected CraftingJobHandler newHandler() {
        return new CraftingJobHandler(1, true, Collections.emptyList(), new ICraftingResultsSink() {
            @Override
            public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance) {
            }
        }) {
            @Override
            protected long getCurrentTick() {
                return 0;
            }
        };
    }

    protected CraftingJob newProcessingJob(CraftingJobHandler handler, int id) {
        CraftingJob craftingJob = new CraftingJob(id, 0, recipe, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        handler.addCraftingJobProcessingPendingIngredientsEntry(craftingJob, newPendingIngredients());
        return craftingJob;
    }

    protected PendingCraftingJobResultIndexObserver<Long, Boolean> newObserver(CraftingJobHandler handler) {
        return new PendingCraftingJobResultIndexObserver<>(IngredientComponentStubs.SIMPLE, handler, craftingNetwork);
    }

    /**
     * Every crafting interface observes the same insertion,
     * so a single produced output may only resolve the pending output of one job.
     * If it resolved all of them, parallel jobs would report completion before their outputs exist,
     * after which a crafting writer would schedule the same job all over again.
     */
    @Test
    public void testSingleOutputOnlyResolvesOneParallelJob() {
        CraftingJobHandler handlerA = newHandler();
        CraftingJobHandler handlerB = newHandler();
        CraftingJob jobA = newProcessingJob(handlerA, 1);
        CraftingJob jobB = newProcessingJob(handlerB, 2);

        long remaining = IIngredientChannelInsertPreConsumer.applyAll(
                Lists.newArrayList(newObserver(handlerA), newObserver(handlerB)),
                IngredientComponentStubs.SIMPLE.getMatcher(), 0, 1L, 1L, false);

        assertThat("the output resolves the first job", jobA.getAmount(), equalTo(0));
        assertThat("the output does not resolve the second job", jobB.getAmount(), equalTo(1));
        assertThat("the output is still inserted into the network", remaining, equalTo(1L));
    }

    @Test
    public void testTwoOutputsResolveTwoParallelJobs() {
        CraftingJobHandler handlerA = newHandler();
        CraftingJobHandler handlerB = newHandler();
        CraftingJob jobA = newProcessingJob(handlerA, 1);
        CraftingJob jobB = newProcessingJob(handlerB, 2);
        List<IIngredientChannelInsertPreConsumer<Long>> observers = Lists.newArrayList(
                newObserver(handlerA), newObserver(handlerB));

        IIngredientChannelInsertPreConsumer.applyAll(observers,
                IngredientComponentStubs.SIMPLE.getMatcher(), 0, 1L, 1L, false);
        IIngredientChannelInsertPreConsumer.applyAll(observers,
                IngredientComponentStubs.SIMPLE.getMatcher(), 0, 1L, 1L, false);

        assertThat(jobA.getAmount(), equalTo(0));
        assertThat(jobB.getAmount(), equalTo(0));
    }

}
