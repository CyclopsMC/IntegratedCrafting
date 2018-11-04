package org.cyclops.integratedcrafting.api.recipe;

import java.util.Comparator;

/**
 * A comparator for comparing recipes by output and priority.
 * @author rubensworks
 */
public class PrioritizedRecipeOutputComparator implements Comparator<PrioritizedRecipe> {

    private static final PrioritizedRecipeOutputComparator INSTANCE = new PrioritizedRecipeOutputComparator();

    private PrioritizedRecipeOutputComparator() {

    }

    public static PrioritizedRecipeOutputComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(PrioritizedRecipe o1, PrioritizedRecipe o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1.getRecipe().getOutput().equals(o2.getRecipe().getOutput())) {
            int[] p1 = o1.getPriorities();
            int[] p2 = o2.getPriorities();
            int minLength = Math.min(p1.length, p2.length);
            for (int i = 0; i < minLength; i++) {
                int comp = Integer.compare(p1[i], p2[i]);
                if (comp != 0) {
                    return comp;
                }
            }
        }
        return o1.getRecipe().compareTo(o2.getRecipe());
    }
}
