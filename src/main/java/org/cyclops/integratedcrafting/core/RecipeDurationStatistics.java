package org.cyclops.integratedcrafting.core;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps track of how long crafting operations take, so that the duration of crafting jobs can be estimated.
 *
 * Durations are tracked per recipe, but only the average duration over all recipes is persisted.
 * This is because the number of recipes that a crafting interface can craft is unbounded,
 * and serializing them would make the crafting interface's state grow indefinitely.
 * As such, after loading, estimations start at the average duration of the crafting interface,
 * and become recipe-specific again as soon as recipes are crafted.
 *
 * Measurements are forgotten once they become too old,
 * as the time that a recipe takes can change when the network or its machines are modified.
 *
 * @author rubensworks
 */
public class RecipeDurationStatistics {

    /**
     * The weight of the latest crafting operation duration within the running average.
     */
    protected static final double SMOOTHING = 0.25D;

    private final int maxEntries;
    private final long maxAge;
    private final Map<IRecipeDefinition, Measurement> recipeDurations;
    @Nullable
    private Measurement averageDuration;

    /**
     * @param maxEntries The maximum number of recipes to remember durations for.
     *                   0 disables recipe-specific durations.
     * @param maxAge The number of ticks after which a measured duration is forgotten. 0 disables forgetting.
     */
    public RecipeDurationStatistics(int maxEntries, long maxAge) {
        this.maxEntries = maxEntries;
        this.maxAge = maxAge;
        this.recipeDurations = new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IRecipeDefinition, Measurement> eldest) {
                // Forget the least recently used recipe once we remember too many of them
                return size() > RecipeDurationStatistics.this.maxEntries;
            }
        };
    }

    /**
     * Take the duration of a finished crafting operation into account for future estimations.
     * @param recipe The recipe that was crafted.
     * @param durationTicks The number of ticks the crafting operation took.
     * @param currentTick The current game tick.
     */
    public void reportDuration(IRecipeDefinition recipe, long durationTicks, long currentTick) {
        if (this.maxEntries > 0) {
            Measurement measurement = this.recipeDurations.get(recipe);
            if (measurement == null || isExpired(measurement, currentTick)) {
                this.recipeDurations.put(recipe, new Measurement(durationTicks, currentTick));
            } else {
                measurement.update(durationTicks, currentTick);
            }
        }

        if (this.averageDuration == null || isExpired(this.averageDuration, currentTick)) {
            this.averageDuration = new Measurement(durationTicks, currentTick);
        } else {
            this.averageDuration.update(durationTicks, currentTick);
        }
    }

    /**
     * @param recipe A recipe.
     * @param currentTick The current game tick.
     * @return The estimated duration in ticks of a single crafting operation of the given recipe.
     *         Falls back to {@link #getAverageDuration(long)} if the recipe itself was not measured (recently),
     *         and is -1 if nothing was measured at all.
     */
    public long getEstimatedDuration(IRecipeDefinition recipe, long currentTick) {
        Measurement measurement = this.recipeDurations.get(recipe);
        if (measurement != null) {
            if (!isExpired(measurement, currentTick)) {
                return Math.round(measurement.getDuration());
            }
            this.recipeDurations.remove(recipe);
        }
        return getAverageDuration(currentTick);
    }

    /**
     * @param currentTick The current game tick.
     * @return The estimated duration in ticks of a single crafting operation of any recipe, or -1 if unknown.
     */
    public long getAverageDuration(long currentTick) {
        if (this.averageDuration != null) {
            if (!isExpired(this.averageDuration, currentTick)) {
                return Math.round(this.averageDuration.getDuration());
            }
            this.averageDuration = null;
        }
        return -1;
    }

    /**
     * @return The number of recipes that durations are remembered for.
     */
    public int getEntryCount() {
        return this.recipeDurations.size();
    }

    protected boolean isExpired(Measurement measurement, long currentTick) {
        if (this.maxAge <= 0) {
            return false;
        }
        long age = currentTick - measurement.getLastMeasuredTick();
        // Negative ages can occur when the game time is moved backwards, in which case the measurement is useless
        return age < 0 || age > this.maxAge;
    }

    public void serialize(ValueOutput valueOutput) {
        if (this.averageDuration != null) {
            valueOutput.putDouble("averageDuration", this.averageDuration.getDuration());
            valueOutput.putLong("averageDurationTick", this.averageDuration.getLastMeasuredTick());
        }
    }

    public void deserialize(ValueInput valueInput) {
        this.recipeDurations.clear();
        this.averageDuration = valueInput.getLong("averageDurationTick")
                .map(tick -> new Measurement(valueInput.getDoubleOr("averageDuration", 0D), tick))
                .orElse(null);
    }

    protected static class Measurement {

        private double duration;
        private long lastMeasuredTick;

        public Measurement(double duration, long lastMeasuredTick) {
            this.duration = duration;
            this.lastMeasuredTick = lastMeasuredTick;
        }

        public double getDuration() {
            return duration;
        }

        public long getLastMeasuredTick() {
            return lastMeasuredTick;
        }

        /**
         * Smooth the given duration into this measurement,
         * as crafting durations can vary due to for example varying machine speeds.
         */
        public void update(long durationTicks, long currentTick) {
            this.duration = this.duration + (durationTicks - this.duration) * SMOOTHING;
            this.lastMeasuredTick = currentTick;
        }
    }

}
