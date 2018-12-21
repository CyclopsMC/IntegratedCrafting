package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraftforge.common.util.Constants;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.core.MissingIngredients;

import java.util.Map;

/**
 * @author rubensworks
 */
public class CraftingJob {

    private final int id;
    private final int channel;
    private final PrioritizedRecipe recipe;
    private final IntList dependencyCraftingJobs;
    private final IntList dependentCraftingJobs;
    private int amount;
    private IMixedIngredients ingredientsStorage;
    private Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients;
    private long startTick;
    private boolean invalidInputs;

    public CraftingJob(int id, int channel, PrioritizedRecipe recipe, int amount, IMixedIngredients ingredientsStorage) {
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

    public PrioritizedRecipe getRecipe() {
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

    public static NBTTagCompound serialize(CraftingJob craftingJob) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("id", craftingJob.id);
        tag.setInteger("channel", craftingJob.channel);
        tag.setTag("recipe", PrioritizedRecipe.serialize(craftingJob.recipe));
        tag.setTag("dependencies", new NBTTagIntArray(craftingJob.getDependencyCraftingJobs()));
        tag.setTag("dependents", new NBTTagIntArray(craftingJob.getDependentCraftingJobs()));
        tag.setInteger("amount", craftingJob.amount);
        tag.setTag("ingredientsStorage", IMixedIngredients.serialize(craftingJob.ingredientsStorage));
        tag.setTag("lastMissingIngredients", MissingIngredients.serialize(craftingJob.lastMissingIngredients));
        tag.setLong("startTick", craftingJob.startTick);
        tag.setBoolean("invalidInputs", craftingJob.invalidInputs);
        return tag;
    }

    public static CraftingJob deserialize(NBTTagCompound tag) {
        if (!tag.hasKey("id", Constants.NBT.TAG_INT)) {
            throw new IllegalArgumentException("Could not find an id entry in the given tag");
        }
        if (!tag.hasKey("channel", Constants.NBT.TAG_INT)) {
            throw new IllegalArgumentException("Could not find a channel entry in the given tag");
        }
        if (!tag.hasKey("recipe", Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a recipe entry in the given tag");
        }
        if (!tag.hasKey("dependencies", Constants.NBT.TAG_INT_ARRAY)) {
            throw new IllegalArgumentException("Could not find a dependencies entry in the given tag");
        }
        if (!tag.hasKey("dependents", Constants.NBT.TAG_INT_ARRAY)) {
            throw new IllegalArgumentException("Could not find a dependents entry in the given tag");
        }
        if (!tag.hasKey("amount", Constants.NBT.TAG_INT)) {
            throw new IllegalArgumentException("Could not find a amount entry in the given tag");
        }
        if (!tag.hasKey("ingredientsStorage", Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a ingredientsStorage entry in the given tag");
        }
        if (!tag.hasKey("lastMissingIngredients", Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a lastMissingIngredients entry in the given tag");
        }
        if (!tag.hasKey("startTick", Constants.NBT.TAG_LONG)) {
            throw new IllegalArgumentException("Could not find a startTick entry in the given tag");
        }
        if (!tag.hasKey("invalidInputs", Constants.NBT.TAG_BYTE)) {
            throw new IllegalArgumentException("Could not find an invalidInputs entry in the given tag");
        }
        int id = tag.getInteger("id");
        int channel = tag.getInteger("channel");
        PrioritizedRecipe prioritizedRecipe = PrioritizedRecipe.deserialize(tag.getCompoundTag("recipe"));
        int amount = tag.getInteger("amount");
        IMixedIngredients ingredientsStorage = IMixedIngredients.deserialize(tag.getCompoundTag("ingredientsStorage"));
        CraftingJob craftingJob = new CraftingJob(id, channel, prioritizedRecipe, amount, ingredientsStorage);
        for (int dependency : tag.getIntArray("dependencies")) {
            craftingJob.dependencyCraftingJobs.add(dependency);
        }
        for (int dependent : tag.getIntArray("dependents")) {
            craftingJob.dependentCraftingJobs.add(dependent);
        }
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients = MissingIngredients
                .deserialize(tag.getCompoundTag("lastMissingIngredients"));
        craftingJob.setLastMissingIngredients(lastMissingIngredients);
        craftingJob.setStartTick(tag.getLong("startTick"));
        craftingJob.setInvalidInputs(tag.getBoolean("invalidInputs"));
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
                && this.getRecipe().equals(that.getRecipe())
                && this.getDependencyCraftingJobs().equals(that.getDependencyCraftingJobs())
                && this.getDependentCraftingJobs().equals(that.getDependentCraftingJobs())
                && this.getAmount() == that.getAmount()
                && this.getIngredientsStorage().equals(that.getIngredientsStorage());
    }
}
