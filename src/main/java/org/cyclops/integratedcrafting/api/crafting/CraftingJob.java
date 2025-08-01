package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.core.MissingIngredients;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * @author rubensworks
 */
public class CraftingJob {

    private final int id;
    private final int channel;
    private final IRecipeDefinition recipe;
    private final IntList dependencyCraftingJobs;
    private final IntList dependentCraftingJobs;
    private int amount;
    private IMixedIngredients ingredientsStorage;
    private Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients;
    private long startTick;
    private boolean invalidInputs;
    @Nullable
    private String initiatorUuid;
    private boolean ignoreDependencyCheck;

    public CraftingJob(int id, int channel, IRecipeDefinition recipe, int amount, IMixedIngredients ingredientsStorage) {
        this.id = id;
        this.channel = channel;
        this.recipe = recipe;
        this.amount = amount;
        this.ingredientsStorage = ingredientsStorage;
        this.lastMissingIngredients = Maps.newIdentityHashMap();
        this.dependencyCraftingJobs = new IntArrayList();
        this.dependentCraftingJobs = new IntArrayList();
        this.invalidInputs = false;
        this.ignoreDependencyCheck = false;
    }

    public int getId() {
        return id;
    }

    public int getChannel() {
        return this.channel;
    }

    public IRecipeDefinition getRecipe() {
        return this.recipe;
    }

    public IntList getDependencyCraftingJobs() {
        return dependencyCraftingJobs;
    }

    public IntList getDependentCraftingJobs() {
        return dependentCraftingJobs;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void addDependency(CraftingJob dependency) {
        dependencyCraftingJobs.add(dependency.getId());
        dependency.dependentCraftingJobs.add(this.getId());
    }

    public void removeDependency(CraftingJob dependency) {
        dependencyCraftingJobs.rem(dependency.getId());
        dependency.dependentCraftingJobs.rem(this.getId());
    }

    /**
     * @return The ingredients that will be taken from storage
     *         The amount of this job is already taken into account.
     */
    public IMixedIngredients getIngredientsStorage() {
        return ingredientsStorage;
    }

    public void setIngredientsStorage(IMixedIngredients ingredientsStorage) {
        this.ingredientsStorage = ingredientsStorage;
    }

    /**
     * @return The ingredients that were missing for 1 job amount. This will mostly be an empty map.
     */
    public Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> getLastMissingIngredients() {
        return lastMissingIngredients;
    }

    public void setLastMissingIngredients(Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients) {
        this.lastMissingIngredients = lastMissingIngredients;
    }

    public long getStartTick() {
        return startTick;
    }

    public void setStartTick(long startTick) {
        this.startTick = startTick;
    }

    public boolean isInvalidInputs() {
        return invalidInputs;
    }

    public void setInvalidInputs(boolean invalidInputs) {
        this.invalidInputs = invalidInputs;
    }

    @Nullable
    public String getInitiatorUuid() {
        return initiatorUuid;
    }

    public void setInitiatorUuid(String initiatorUuid) {
        this.initiatorUuid = initiatorUuid;
    }

    public void setIgnoreDependencyCheck(boolean ignoreDependencyCheck) {
        this.ignoreDependencyCheck = ignoreDependencyCheck;
    }

    public boolean isIgnoreDependencyCheck() {
        return ignoreDependencyCheck;
    }

    public static void serialize(ValueOutput valueOutput, CraftingJob craftingJob) {
        valueOutput.putInt("id", craftingJob.id);
        valueOutput.putInt("channel", craftingJob.channel);
        IRecipeDefinition.serialize(valueOutput.child("recipe"), craftingJob.recipe);
        valueOutput.putIntArray("dependencies", craftingJob.getDependencyCraftingJobs().toIntArray());
        valueOutput.putIntArray("dependents", craftingJob.getDependentCraftingJobs().toIntArray());
        valueOutput.putInt("amount", craftingJob.amount);
        IMixedIngredients.serialize(valueOutput.child("ingredientsStorage"), craftingJob.ingredientsStorage);
        MissingIngredients.serialize(valueOutput.child("lastMissingIngredients"), craftingJob.lastMissingIngredients);
        valueOutput.putLong("startTick", craftingJob.startTick);
        valueOutput.putBoolean("invalidInputs", craftingJob.invalidInputs);
        if (craftingJob.initiatorUuid != null) {
            valueOutput.putString("initiatorUuid", craftingJob.initiatorUuid);
        }
        valueOutput.putBoolean("ignoreDependencyCheck", craftingJob.ignoreDependencyCheck);
    }

    public static CraftingJob deserialize(ValueInput valueInput) {
        int id = valueInput.getInt("id").orElseThrow();
        int channel = valueInput.getInt("channel").orElseThrow();
        IRecipeDefinition recipe = IRecipeDefinition.deserialize(valueInput.child("recipe").orElseThrow());
        int amount = valueInput.getInt("amount").orElseThrow();
        IMixedIngredients ingredientsStorage = IMixedIngredients.deserialize(valueInput.child("ingredientsStorage").orElseThrow());
        CraftingJob craftingJob = new CraftingJob(id, channel, recipe, amount, ingredientsStorage);
        for (int dependency : valueInput.getIntArray("dependencies").orElseThrow()) {
            craftingJob.dependencyCraftingJobs.add(dependency);
        }
        for (int dependent : valueInput.getIntArray("dependents").orElseThrow()) {
            craftingJob.dependentCraftingJobs.add(dependent);
        }
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients = MissingIngredients
                .deserialize(valueInput.child("lastMissingIngredients").orElseThrow());
        craftingJob.setLastMissingIngredients(lastMissingIngredients);
        craftingJob.setStartTick(valueInput.getLong("startTick").orElseThrow());
        craftingJob.setInvalidInputs(valueInput.getBooleanOr("invalidInputs", false));
        valueInput.getString("initiatorUuid").ifPresent(craftingJob::setInitiatorUuid);
        craftingJob.setIgnoreDependencyCheck(valueInput.getBooleanOr("ignoreDependencyCheck", false));
        return craftingJob;
    }

    @Override
    public String toString() {
        return String.format("[Crafting Job id: %s, channel: %s, recipe: %s, dependencies: %s, dependents: %s, amount: %s, storage: %s]",
                getId(), getChannel(), getRecipe(), getDependencyCraftingJobs(), getDependentCraftingJobs(), getAmount(), getIngredientsStorage());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CraftingJob)) {
            return false;
        }
        CraftingJob that = (CraftingJob) obj;
        return this.getId() == that.getId()
                && this.getChannel() == that.getChannel()
                && Objects.equals(this.getRecipe(), that.getRecipe())
                && this.getDependencyCraftingJobs().equals(that.getDependencyCraftingJobs())
                && this.getDependentCraftingJobs().equals(that.getDependentCraftingJobs())
                && this.getAmount() == that.getAmount()
                && this.getIngredientsStorage().equals(that.getIngredientsStorage());
    }

    public CraftingJob clone(CraftingHelpers.IIdentifierGenerator identifierGenerator) {
        return new CraftingJob(
                identifierGenerator.getNext(),
                getChannel(),
                getRecipe(),
                getAmount(),
                getIngredientsStorage()
        );
    }
}
