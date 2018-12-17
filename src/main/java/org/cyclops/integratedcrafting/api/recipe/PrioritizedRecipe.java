package org.cyclops.integratedcrafting.api.recipe;

import com.google.common.collect.Sets;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;

import java.util.Arrays;
import java.util.TreeSet;

/**
 * A recipe with a chain of priorities.
 * The priorities that come first have a higher 'priority' during comparison than those later in the array.
 * @author rubensworks
 */
public class PrioritizedRecipe {

    private final IRecipeDefinition recipe;
    private final int[] priorities;

    public PrioritizedRecipe(IRecipeDefinition recipe, int... priorities) {
        this.recipe = recipe;
        this.priorities = priorities;
    }

    public IRecipeDefinition getRecipe() {
        return recipe;
    }

    public int[] getPriorities() {
        return priorities;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PrioritizedRecipe
                && ((PrioritizedRecipe) obj).getRecipe().equals(this.getRecipe())
                && Arrays.equals(((PrioritizedRecipe) obj).getPriorities(), this.getPriorities());
    }

    @Override
    public String toString() {
        return String.format("[PrioritizedRecipe %s %s]", getRecipe(), getPriorities());
    }

    @Override
    public int hashCode() {
        return getRecipe().hashCode() | Arrays.hashCode(getPriorities());
    }

    /**
     * @return A new set in which recipes will be sorted by output, followed by priority.
     */
    public static TreeSet<PrioritizedRecipe> newOutputSortedSet() {
        return Sets.newTreeSet(PrioritizedRecipeOutputComparator.getInstance());
    }

    public static NBTTagCompound serialize(PrioritizedRecipe prioritizedRecipe) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("priorities", prioritizedRecipe.getPriorities());
        tag.setTag("recipeDefinition", IRecipeDefinition.serialize(prioritizedRecipe.getRecipe()));
        return tag;
    }

    public static PrioritizedRecipe deserialize(NBTTagCompound tag) {
        if (!tag.hasKey("priorities", Constants.NBT.TAG_INT_ARRAY)) {
            throw new IllegalArgumentException("Could not find a priorities entry in the given tag");
        }
        if (!tag.hasKey("recipeDefinition", Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a recipeDefinition entry in the given tag");
        }
        int[] priorities = tag.getIntArray("priorities");
        IRecipeDefinition recipeDefinition = IRecipeDefinition.deserialize(tag.getCompoundTag("recipeDefinition"));
        return new PrioritizedRecipe(recipeDefinition, priorities);
    }
}
