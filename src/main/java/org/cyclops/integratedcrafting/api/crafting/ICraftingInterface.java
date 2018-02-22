package org.cyclops.integratedcrafting.api.crafting;

import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Collection;

/**
 * A handler for invoking crafting recipes.
 * @author rubensworks
 */
public interface ICraftingInterface {

    /**
     * @return The collection of recipes that is exposed by this crafting interface.
     */
    public Collection<PrioritizedRecipe> getRecipes();

}
