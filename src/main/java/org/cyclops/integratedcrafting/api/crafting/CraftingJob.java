package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
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

    public static CompoundTag serialize(CraftingJob craftingJob) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", craftingJob.id);
        tag.putInt("channel", craftingJob.channel);
        tag.put("recipe", IRecipeDefinition.serialize(craftingJob.recipe));
        tag.put("dependencies", new IntArrayTag(craftingJob.getDependencyCraftingJobs()));
        tag.put("dependents", new IntArrayTag(craftingJob.getDependentCraftingJobs()));
        tag.putInt("amount", craftingJob.amount);
        tag.put("ingredientsStorage", IMixedIngredients.serialize(craftingJob.ingredientsStorage));
        tag.put("lastMissingIngredients", MissingIngredients.serialize(craftingJob.lastMissingIngredients));
        tag.putLong("startTick", craftingJob.startTick);
        tag.putBoolean("invalidInputs", craftingJob.invalidInputs);
        if (craftingJob.initiatorUuid != null) {
            tag.putString("initiatorUuid", craftingJob.initiatorUuid);
        }
        return tag;
    }

    public static CraftingJob deserialize(CompoundTag tag) {
        if (!tag.contains("id", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Could not find an id entry in the given tag");
        }
        if (!tag.contains("channel", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Could not find a channel entry in the given tag");
        }
        if (!tag.contains("recipe", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a recipe entry in the given tag");
        }
        if (!tag.contains("dependencies", Tag.TAG_INT_ARRAY)) {
            throw new IllegalArgumentException("Could not find a dependencies entry in the given tag");
        }
        if (!tag.contains("dependents", Tag.TAG_INT_ARRAY)) {
            throw new IllegalArgumentException("Could not find a dependents entry in the given tag");
        }
        if (!tag.contains("amount", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Could not find a amount entry in the given tag");
        }
        if (!tag.contains("ingredientsStorage", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a ingredientsStorage entry in the given tag");
        }
        if (!tag.contains("lastMissingIngredients", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a lastMissingIngredients entry in the given tag");
        }
        if (!tag.contains("startTick", Tag.TAG_LONG)) {
            throw new IllegalArgumentException("Could not find a startTick entry in the given tag");
        }
        if (!tag.contains("invalidInputs", Tag.TAG_BYTE)) {
            throw new IllegalArgumentException("Could not find an invalidInputs entry in the given tag");
        }
        int id = tag.getInt("id");
        int channel = tag.getInt("channel");
        IRecipeDefinition recipe = IRecipeDefinition.deserialize(tag.getCompound("recipe"));
        int amount = tag.getInt("amount");
        IMixedIngredients ingredientsStorage = IMixedIngredients.deserialize(tag.getCompound("ingredientsStorage"));
        CraftingJob craftingJob = new CraftingJob(id, channel, recipe, amount, ingredientsStorage);
        for (int dependency : tag.getIntArray("dependencies")) {
            craftingJob.dependencyCraftingJobs.add(dependency);
        }
        for (int dependent : tag.getIntArray("dependents")) {
            craftingJob.dependentCraftingJobs.add(dependent);
        }
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients = MissingIngredients
                .deserialize(tag.getCompound("lastMissingIngredients"));
        craftingJob.setLastMissingIngredients(lastMissingIngredients);
        craftingJob.setStartTick(tag.getLong("startTick"));
        craftingJob.setInvalidInputs(tag.getBoolean("invalidInputs"));
        if (tag.contains("initiatorUuid", Tag.TAG_STRING)) {
            craftingJob.setInitiatorUuid(tag.getString("initiatorUuid"));
        }
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
