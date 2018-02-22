package org.cyclops.integratedcrafting.api.recipe;

import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;

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

    /**
     * @return A new set in which recipes will be sorted by output, followed by priority.
     */
    public static TreeSet<PrioritizedRecipe> newOutputSortedSet() {
        return Sets.newTreeSet(PrioritizedRecipeOutputComparator.getInstance());
    }
}
