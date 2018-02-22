package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Maps;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentIndex;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Map;
import java.util.Set;

/**
 * A recipe component index for {@link IngredientComponent#ITEMSTACK}.
 * @author rubensworks
 */
public class IngredientComponentIndexItemStack implements IIngredientComponentIndex<ItemStack, Integer> {

    private final Map<Item, Set<PrioritizedRecipe>> prototypedItemRecipes;
    private final Set<PrioritizedRecipe> plainItemRecipes;

    public IngredientComponentIndexItemStack() {
        this.prototypedItemRecipes = Maps.newIdentityHashMap();
        this.plainItemRecipes = PrioritizedRecipe.newOutputSortedSet();
    }

    @Override
    public void addRecipe(PrioritizedRecipe prioritizedRecipe) {
        for (ItemStack ingredient : prioritizedRecipe.getRecipe().getOutput().getInstances(IngredientComponent.ITEMSTACK)) {
            Item item = ingredient.getItem();
            Set<PrioritizedRecipe> recipes = this.prototypedItemRecipes.computeIfAbsent(item, i -> PrioritizedRecipe.newOutputSortedSet());
            recipes.add(prioritizedRecipe);
        }
    }

    @Override
    public void removeRecipe(PrioritizedRecipe prioritizedRecipe) {
        for (ItemStack ingredient : prioritizedRecipe.getRecipe().getOutput().getInstances(IngredientComponent.ITEMSTACK)) {
            Item item = ingredient.getItem();
            Set<PrioritizedRecipe> recipes = this.prototypedItemRecipes.get(item);
            if (recipes != null) {
                recipes.remove(prioritizedRecipe);
            }
        }
    }

    @Override
    public Set<PrioritizedRecipe> getRecipes(ItemStack output, Integer matchCondition, int limit) {
        Set<PrioritizedRecipe> recipes = PrioritizedRecipe.newOutputSortedSet();

        // Check the prototyped recipes
        for (PrioritizedRecipe prioritizedRecipe : this.prototypedItemRecipes.get(output.getItem())) {
            for (ItemStack ingredient : prioritizedRecipe.getRecipe().getOutput().getInstances(IngredientComponent.ITEMSTACK)) {
                if (ItemMatch.areItemStacksEqual(ingredient, output, matchCondition)) {
                    recipes.add(prioritizedRecipe);
                    if (recipes.size() >= limit) {
                        return recipes;
                    }
                    break;
                }
            }
        }

        return recipes;
    }
}
