package org.cyclops.integratedcrafting.api.crafting;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

/**
 * @author rubensworks
 */
public class CraftingJob {

    private final int channel;
    private final PrioritizedRecipe recipe;

    public CraftingJob(int channel, PrioritizedRecipe recipe) {
        this.channel = channel;
        this.recipe = recipe;
    }

    public int getChannel() {
        return this.channel;
    }

    public PrioritizedRecipe getRecipe() {
        return this.recipe;
    }

    public static NBTTagCompound serialize(CraftingJob craftingJob) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("channel", craftingJob.channel);
        tag.setIntArray("priorities", craftingJob.recipe.getPriorities());
        tag.setTag("recipeDefinition", IRecipeDefinition.serialize(craftingJob.recipe.getRecipe()));
        return tag;
    }

    public static CraftingJob deserialize(NBTTagCompound tag) {
        if (!tag.hasKey("channel", Constants.NBT.TAG_INT)) {
            throw new IllegalArgumentException("Could not find a channel entry in the given tag");
        }
        if (!tag.hasKey("priorities", Constants.NBT.TAG_INT_ARRAY)) {
            throw new IllegalArgumentException("Could not find a priorities entry in the given tag");
        }
        if (!tag.hasKey("recipeDefinition", Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a recipeDefinition entry in the given tag");
        }
        int channel = tag.getInteger("channel");
        int[] priorities = tag.getIntArray("priorities");
        IRecipeDefinition recipeDefinition = IRecipeDefinition.deserialize(tag.getCompoundTag("recipeDefinition"));
        return new CraftingJob(channel, new PrioritizedRecipe(recipeDefinition, priorities));
    }
}
