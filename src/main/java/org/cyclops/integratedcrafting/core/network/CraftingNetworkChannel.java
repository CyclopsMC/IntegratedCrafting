package org.cyclops.integratedcrafting.core.network;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.network.ICraftingNetworkChannel;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndexModifiable;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Map;
import java.util.Set;

/**
 * A single channel of the crafting handler network.
 * @author rubensworks
 */
public class CraftingNetworkChannel implements ICraftingNetworkChannel {

    private final CraftingNetwork craftingNetwork;
    private final int channel;

    public CraftingNetworkChannel(CraftingNetwork craftingNetwork, int channel) {
        this.craftingNetwork = craftingNetwork;
        this.channel = channel;
    }

    protected Set<ICraftingInterface> getCraftingInterfaces() {
        return craftingNetwork.getCraftingInterfaces(channel);
    }

    protected IRecipeIndexModifiable getRecipeIndex() {
        return craftingNetwork.getRecipeIndex(channel);
    }

    protected Map<PrioritizedRecipe, ICraftingInterface> getRecipeCraftingInterfaces() {
        return craftingNetwork.getRecipeCraftingInterfaces(channel);
    }

    @Override
    public Set<PrioritizedRecipe> getRecipes() {
        return getRecipeIndex().getRecipes();
    }

    @Override
    public <T, R, M> Set<PrioritizedRecipe> getRecipes(IngredientComponent<T, R, M> outputType, T output, M matchCondition, int limit) {
        return getRecipeIndex().getRecipes(outputType, output, matchCondition, limit);
    }

    @Override
    public int getChannel() {
        return this.channel;
    }

    @Override
    public boolean addCraftingInterface(ICraftingInterface craftingInterface) {
        // Only process deeper indexes if the interface was not yet present
        if (getCraftingInterfaces().add(craftingInterface)) {
            IRecipeIndexModifiable recipeIndex = getRecipeIndex();
            Map<PrioritizedRecipe, ICraftingInterface> recipeCraftingInterfaces = getRecipeCraftingInterfaces();
            for (PrioritizedRecipe recipe : craftingInterface.getRecipes()) {
                // Save the recipes in the index
                recipeIndex.addRecipe(recipe);
                // Save a mapping from each of the recipes to this crafting interface
                recipeCraftingInterfaces.put(recipe, craftingInterface);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removeCraftingInterface(ICraftingInterface craftingInterface) {
        // Only process deeper indexes if the interface was present
        if (getCraftingInterfaces().remove(craftingInterface)) {
            IRecipeIndexModifiable recipeIndex = getRecipeIndex();
            Map<PrioritizedRecipe, ICraftingInterface> recipeCraftingInterfaces = getRecipeCraftingInterfaces();
            for (PrioritizedRecipe recipe : craftingInterface.getRecipes()) {
                // Remove the recipes from the index
                recipeIndex.removeRecipe(recipe);
                // Remove the mappings from each of the recipes to this crafting interface
                recipeCraftingInterfaces.remove(recipe, craftingInterface);
            }

            // Try cleaning up the channel
            craftingNetwork.cleanupChannel(channel);
            return true;
        }
        return false;
    }
}
