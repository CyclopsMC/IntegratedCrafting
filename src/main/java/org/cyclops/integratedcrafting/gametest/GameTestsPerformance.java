package org.cyclops.integratedcrafting.gametest;

import com.google.common.math.Stats;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TimeUtil;
import org.apache.logging.log4j.Level;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.command.CommandGenerateCrafting;
import org.cyclops.integrateddynamics.core.network.diagnostics.NetworkDiagnostics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Game tests for performance benchmarking of crafting operations.
 * These tests generate networks with different presets and measure their performance.
 * Results are written to run/logs/benchmark_results.txt for CI processing.
 *
 * These tests only do actual work when the PERFORMANCE_BENCHMARK_ENABLED environment variable is set,
 * so that regular game test runs are not slowed down by them.
 *
 * @author rubensworks
 */
public class GameTestsPerformance {

    public static final int EXECUTION_SECONDS = 30;
    public static final int WARMUP_TICKS = 200;
    public static final int TIMEOUT_TICKS = (EXECUTION_SECONDS + 20) * 20;
    public static final int SIZE = 9; // Max 9, as the grid would otherwise leak out of the template.
    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
    public static final BlockPos START_POS = BlockPos.ZERO;

    /**
     * The number of cells that are added or removed after warming up, for the churn benchmarks.
     */
    public static final int CHURN_CELLS = 50;

    private static final String RESULTS_FILE = "logs/benchmark_results.txt";

    /**
     * Check if performance benchmarking is enabled via environment variable.
     *
     * @return true if PERFORMANCE_BENCHMARK_ENABLED environment variable is set to "true"
     */
    private static boolean isBenchmarkingEnabled() {
        // Check environment variable first
        String envVar = System.getenv("PERFORMANCE_BENCHMARK_ENABLED");
        if (envVar != null && "true".equalsIgnoreCase(envVar)) {
            return true;
        }

        // Check system property as fallback
        String sysProp = System.getProperty("PERFORMANCE_BENCHMARK_ENABLED");
        return sysProp != null && "true".equalsIgnoreCase(sysProp);
    }

    static {
        if (isBenchmarkingEnabled()) {
            // Initialize empty file
            writeResults(new ArrayList<>(), false);
        }
    }

    /*
     * Crafting interface observation: the standing cost of having crafting interfaces in a network,
     * without anything ever requesting a craft.
     */

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_interfaces_crafting_idle")
    public void testPerformanceCraftingInterfacesIdle(GameTestHelper helper) {
        testPerformance(helper, "interfaces_crafting_idle", (measureServerTickTimeNow) ->
                CommandGenerateCrafting.CraftingGenerationHelper.generateInterfacesIdle(helper.getLevel(), helper.absolutePos(START_POS), SIZE));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_interfaces_crafting_idle_recipes")
    public void testPerformanceCraftingInterfacesIdleRecipes(GameTestHelper helper) {
        testPerformance(helper, "interfaces_crafting_idle_recipes", (measureServerTickTimeNow) ->
                CommandGenerateCrafting.CraftingGenerationHelper.generateInterfacesIdleRecipes(helper.getLevel(), helper.absolutePos(START_POS), SIZE));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_craft_satisfied_idle")
    public void testPerformanceCraftSatisfiedIdle(GameTestHelper helper) {
        testPerformance(helper, "craft_satisfied_idle", (measureServerTickTimeNow) ->
                CommandGenerateCrafting.CraftingGenerationHelper.generateCraftSatisfiedIdle(helper.getLevel(), helper.absolutePos(START_POS), SIZE));
    }

    /*
     * Active crafting: the cost of continuously scheduling and executing crafting jobs.
     */

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_craft_simple")
    public void testPerformanceCraftSimple(GameTestHelper helper) {
        testPerformance(helper, "craft_simple", (measureServerTickTimeNow) ->
                CommandGenerateCrafting.CraftingGenerationHelper.generateCraftSimple(helper.getLevel(), helper.absolutePos(START_POS), SIZE));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_craft_nested")
    public void testPerformanceCraftNested(GameTestHelper helper) {
        testPerformance(helper, "craft_nested", (measureServerTickTimeNow) ->
                CommandGenerateCrafting.CraftingGenerationHelper.generateCraftNested(helper.getLevel(), helper.absolutePos(START_POS), SIZE));
    }

    /*
     * Recipe index scaling: the cost of crafting while the network's recipe index is large.
     */

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_craft_recipe_index")
    public void testPerformanceCraftRecipeIndex(GameTestHelper helper) {
        testPerformance(helper, "craft_recipe_index", (measureServerTickTimeNow) ->
                CommandGenerateCrafting.CraftingGenerationHelper.generateCraftRecipeIndex(helper.getLevel(), helper.absolutePos(START_POS), SIZE));
    }

    /*
     * Topology churn: the cost of registering and unregistering crafting interfaces in the crafting network.
     */

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_interfaces_crafting_append")
    public void testPerformanceCraftingInterfacesAppend(GameTestHelper helper) {
        testPerformance(helper, "interfaces_crafting_append", (measureServerTickTimeNow) -> {
            CommandGenerateCrafting.CraftingGenerationHelper.generateEmptyGrid(helper.getLevel(), helper.absolutePos(START_POS), SIZE);
            addInterfacesPostWarmup(helper, CHURN_CELLS, WARMUP_TICKS);
            // Measure server tick time right after the interfaces have been added
            helper.runAfterDelay(WARMUP_TICKS + CHURN_CELLS, measureServerTickTimeNow);
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT_TICKS, environment = Reference.MOD_ID + ":performance_interfaces_crafting_remove")
    public void testPerformanceCraftingInterfacesRemove(GameTestHelper helper) {
        testPerformance(helper, "interfaces_crafting_remove", (measureServerTickTimeNow) -> {
            CommandGenerateCrafting.CraftingGenerationHelper.generateInterfacesIdle(helper.getLevel(), helper.absolutePos(START_POS), SIZE);
            removeInterfacesPostWarmup(helper, CHURN_CELLS, WARMUP_TICKS);
            // Measure server tick time right after the interfaces have been removed
            helper.runAfterDelay(WARMUP_TICKS + CHURN_CELLS, measureServerTickTimeNow);
        });
    }

    /**
     * Construct the given network, let it warm up, and measure its performance for {@link #EXECUTION_SECONDS}.
     * @param helper The game test helper.
     * @param networkName The name of the network preset, used as benchmark identifier.
     * @param networkConstructor Constructs the network.
     *                           It is passed a runnable that captures the server tick time at the moment it is called,
     *                           which is useful for presets that only cause load during a part of the measurement.
     */
    public static void testPerformance(GameTestHelper helper, String networkName, Consumer<Runnable> networkConstructor) {
        if (!isBenchmarkingEnabled()) {
            IntegratedCrafting.clog(Level.INFO, "Performance benchmarking disabled (PERFORMANCE_BENCHMARK_ENABLED not set)");
            helper.succeed();
            return;
        }

        ensureResultsDirectory();

        // Calculate average server-wide tick time
        Wrapper<Double> avgServerTickTime = new Wrapper<>(0D);
        Runnable measureServerTickTimeNow = () -> avgServerTickTime.set(Stats.meanOf(helper.getLevel().getServer().getTickTimesNanos()) / TimeUtil.NANOSECONDS_PER_MILLISECOND);
        networkConstructor.accept(measureServerTickTimeNow);

        // Measure the network performance
        String measurementId = networkName + "_" + System.currentTimeMillis();
        Wrapper<UUID> measurementUUID = new Wrapper<>();
        helper.runAfterDelay(WARMUP_TICKS, () -> {
            // Wait a few seconds to warm up the code before starting measurement
            measurementUUID.set(NetworkDiagnostics.getInstance().startMeasurementWithoutPlayer(measurementId, EXECUTION_SECONDS));
        });

        // Assert that all generated parts actually activated, right after the warmup.
        // A part that silently failed to activate would still cost tick time,
        // which would turn this benchmark into an expensive no-op.
        helper.runAfterDelay(WARMUP_TICKS, () -> {
            List<String> problems = CommandGenerateCrafting.CraftingGenerationHelper
                    .getPartProblems(helper.getLevel(), helper.absolutePos(START_POS), SIZE);
            if (!problems.isEmpty()) {
                throw new GameTestAssertException(Component.literal("Preset " + networkName + " generated " + problems.size()
                        + " broken parts, first: " + problems.get(0)), (int) helper.getTick());
            }
        });

        // Wait for measurement to complete, then retrieve results
        helper.succeedWhen(() -> {
            if (measurementUUID.get() == null || !NetworkDiagnostics.getInstance().isMeasurementComplete(measurementUUID.get())) {
                throw new GameTestAssertException(Component.literal("Measurement did not complete in time: " + measurementId), (int) helper.getTick());
            }

            double avgTickTime = NetworkDiagnostics.getInstance().getMeasurementAverageTickTime(measurementUUID.get());
            NetworkDiagnostics.getInstance().clearMeasurement(measurementUUID.get());

            // Calculate average server-wide tick time
            if (avgServerTickTime.get() == 0D) {
                measureServerTickTimeNow.run();
            }

            List<String> results = new ArrayList<>();
            results.add(String.format("preset=%s size=%d avgNetworkTickTime=%.2f avgServerTickTime=%.2f", networkName, SIZE, avgTickTime, avgServerTickTime.get()));
            writeResults(results, true);

            CommandGenerateCrafting.CraftingGenerationHelper.clearGrid(helper.getLevel(), helper.absolutePos(START_POS), SIZE);
        });
    }

    private static void ensureResultsDirectory() {
        try {
            Files.createDirectories(Paths.get("logs"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized void writeResults(List<String> results, boolean append) {
        try {
            String content = String.join("\n", results);
            if (append && Files.exists(Paths.get(RESULTS_FILE))) {
                String existingString = Files.readString(Paths.get(RESULTS_FILE));
                content = (existingString.isEmpty() ? content : existingString + content) + "\n";
            }
            Files.write(Paths.get(RESULTS_FILE), content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add one crafting table with a crafting interface per tick, starting at the given delay.
     */
    private static void addInterfacesPostWarmup(GameTestHelper helper, int count, int delayOffset) {
        List<BlockPos> cells = CommandGenerateCrafting.CraftingGenerationHelper.getCells(helper.absolutePos(START_POS), SIZE);
        for (int i = 0; i < Math.min(count, cells.size()); i++) {
            final BlockPos cell = cells.get(i);
            final int index = i;
            helper.runAfterDelay(delayOffset + i, () -> CommandGenerateCrafting.CraftingGenerationHelper
                    .addCraftingInterfaceCell(helper.getLevel(), cell, index));
        }
    }

    /**
     * Remove one crafting table with its crafting interface per tick, starting at the given delay.
     */
    private static void removeInterfacesPostWarmup(GameTestHelper helper, int count, int delayOffset) {
        List<BlockPos> cells = CommandGenerateCrafting.CraftingGenerationHelper.getCells(helper.absolutePos(START_POS), SIZE);
        for (int i = 0; i < Math.min(count, cells.size()); i++) {
            final BlockPos cell = cells.get(i);
            helper.runAfterDelay(delayOffset + i, () -> CommandGenerateCrafting.CraftingGenerationHelper
                    .removeCell(helper.getLevel(), cell));
        }
    }
}
