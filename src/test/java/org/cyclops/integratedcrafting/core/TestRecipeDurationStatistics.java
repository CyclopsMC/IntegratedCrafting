package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author rubensworks
 */
public class TestRecipeDurationStatistics {

    private static final int MAX_AGE = 1000;

    private RecipeDurationStatistics statistics;
    private IRecipeDefinition recipeA;
    private IRecipeDefinition recipeB;
    private IRecipeDefinition recipeC;

    @BeforeEach
    public void beforeEach() {
        this.statistics = new RecipeDurationStatistics(2, MAX_AGE);
        this.recipeA = newRecipe(0);
        this.recipeB = newRecipe(1);
        this.recipeC = newRecipe(2);
    }

    protected static IRecipeDefinition newRecipe(long output) {
        Map<IngredientComponent<?, ?>, List<?>> outputs = Maps.newIdentityHashMap();
        outputs.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(output));
        return new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(outputs));
    }

    @Test
    public void testUnknown() {
        assertThat(statistics.getEstimatedDuration(recipeA, 0), equalTo(-1L));
        assertThat(statistics.getAverageDuration(0), equalTo(-1L));
    }

    @Test
    public void testSingleMeasurement() {
        statistics.reportDuration(recipeA, 100, 0);

        assertThat(statistics.getEstimatedDuration(recipeA, 0), equalTo(100L));
        assertThat(statistics.getAverageDuration(0), equalTo(100L));
    }

    @Test
    public void testSmoothedMeasurements() {
        statistics.reportDuration(recipeA, 100, 0);
        statistics.reportDuration(recipeA, 200, 1);

        assertThat(statistics.getEstimatedDuration(recipeA, 1), equalTo(125L));
    }

    @Test
    public void testMeasurementsPerRecipe() {
        statistics.reportDuration(recipeA, 100, 0);
        statistics.reportDuration(recipeB, 300, 1);

        assertThat(statistics.getEstimatedDuration(recipeA, 1), equalTo(100L));
        assertThat(statistics.getEstimatedDuration(recipeB, 1), equalTo(300L));
        assertThat(statistics.getAverageDuration(1), equalTo(150L));
    }

    @Test
    public void testUnmeasuredRecipeFallsBackToAverage() {
        statistics.reportDuration(recipeA, 100, 0);

        assertThat(statistics.getEstimatedDuration(recipeC, 0), equalTo(100L));
    }

    @Test
    public void testLeastRecentlyUsedRecipeIsForgotten() {
        statistics.reportDuration(recipeA, 100, 0);
        statistics.reportDuration(recipeB, 200, 1);
        statistics.reportDuration(recipeC, 300, 2);

        assertThat(statistics.getEntryCount(), equalTo(2));
        assertThat(statistics.getEstimatedDuration(recipeB, 2), equalTo(200L));
        assertThat(statistics.getEstimatedDuration(recipeC, 2), equalTo(300L));
        // The oldest recipe was forgotten, so it falls back to the average
        assertThat(statistics.getEstimatedDuration(recipeA, 2), equalTo(169L));
    }

    @Test
    public void testOutdatedMeasurementsAreForgotten() {
        statistics.reportDuration(recipeA, 100, 0);

        assertThat(statistics.getEstimatedDuration(recipeA, MAX_AGE), equalTo(100L));
        assertThat(statistics.getEstimatedDuration(recipeA, MAX_AGE + 1), equalTo(-1L));
        assertThat(statistics.getEntryCount(), equalTo(0));
    }

    @Test
    public void testOutdatedMeasurementsDoNotSlowDownNewOnes() {
        // The network may have been optimized since the last measurement,
        // so an outdated measurement must be replaced instead of smoothed into
        statistics.reportDuration(recipeA, 100, 0);
        statistics.reportDuration(recipeA, 20, MAX_AGE + 1);

        assertThat(statistics.getEstimatedDuration(recipeA, MAX_AGE + 1), equalTo(20L));
        assertThat(statistics.getAverageDuration(MAX_AGE + 1), equalTo(20L));
    }

    @Test
    public void testMeasurementsFromTheFutureAreForgotten() {
        // The game time can be moved backwards, which makes existing measurements meaningless
        statistics.reportDuration(recipeA, 100, 1000);

        assertThat(statistics.getEstimatedDuration(recipeA, 0), equalTo(-1L));
    }

    @Test
    public void testMeasurementsCanBeKeptForever() {
        RecipeDurationStatistics statistics = new RecipeDurationStatistics(2, 0);
        statistics.reportDuration(recipeA, 100, 0);

        assertThat(statistics.getEstimatedDuration(recipeA, 1_000_000), equalTo(100L));
    }

    @Test
    public void testRecipeSpecificMeasurementsCanBeDisabled() {
        RecipeDurationStatistics statistics = new RecipeDurationStatistics(0, MAX_AGE);
        statistics.reportDuration(recipeA, 100, 0);
        statistics.reportDuration(recipeB, 300, 1);

        assertThat(statistics.getEntryCount(), equalTo(0));
        assertThat(statistics.getEstimatedDuration(recipeA, 1), equalTo(150L));
    }

    @Test
    public void testSerializationKeepsTheAverage() {
        statistics.reportDuration(recipeA, 100, 50);

        TagValueOutput valueOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        statistics.serialize(valueOutput);
        RecipeDurationStatistics deserialized = new RecipeDurationStatistics(2, MAX_AGE);
        deserialized.deserialize(createInput(valueOutput));

        assertThat(deserialized.getAverageDuration(50), equalTo(100L));
        assertThat(deserialized.getEstimatedDuration(recipeA, 50), equalTo(100L));
        // Recipe-specific measurements are not serialized, as their number is unbounded
        assertThat(deserialized.getEntryCount(), equalTo(0));
        // Measurements from before a restart can be outdated as well
        assertThat(deserialized.getAverageDuration(50 + MAX_AGE + 1), equalTo(-1L));
    }

    @Test
    public void testSerializationWithoutMeasurements() {
        TagValueOutput valueOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        statistics.serialize(valueOutput);
        RecipeDurationStatistics deserialized = new RecipeDurationStatistics(2, MAX_AGE);
        deserialized.deserialize(createInput(valueOutput));

        assertThat(deserialized.getAverageDuration(0), equalTo(-1L));
    }


    protected static ValueInput createInput(TagValueOutput valueOutput) {
        return TagValueInput.create(ProblemReporter.DISCARDING,
                HolderLookup.Provider.create(Stream.empty()), valueOutput.buildResult());
    }

}
