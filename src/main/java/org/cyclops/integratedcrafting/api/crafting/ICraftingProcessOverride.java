package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integrateddynamics.api.part.PartPos;

import java.util.function.Function;

/**
 * A certain override for performing a crafting process.
 * @author rubensworks
 */
public interface ICraftingProcessOverride {

    /**
     * Check if this override applies to the given target.
     * @param target A target position.
     * @return If this override is applicable.
     */
    public boolean isApplicable(PartPos target);

    /**
     * Start a crafting process with the given ingredients.
     * @param targetGetter A function to get the target position.
     * @param ingredients The ingredients to insert.
     * @param resultsSink A sink where the ingredients can optionally be inserted into.
     *                    This should only be used if the processor does not have an internal storage.
     * @param simulate If insertion should be simulated.
     * @return If all instances could be inserted.
     */
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter, IMixedIngredients ingredients,
                         ICraftingResultsSink resultsSink, boolean simulate);

}
