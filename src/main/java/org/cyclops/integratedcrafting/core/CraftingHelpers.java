package org.cyclops.integratedcrafting.core;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkConfig;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * Helpers related to the crafting network.
 * @author rubensworks
 */
public class CraftingHelpers {

    /**
     * Get the crafting network in the given network.
     * @param network A network.
     * @return The crafting network or null.
     */
    @Nullable
    public static ICraftingNetwork getCraftingNetwork(@Nullable INetwork network) {
        if (network != null) {
            return network.getCapability(CraftingNetworkConfig.CAPABILITY);
        }
        return null;
    }

    /**
     * Schedule a crafting job for the given recipe in the given network.
     * @param craftingNetwork The target crafting network.
     * @param channel The target channel.
     * @param recipeDefinition The recipe to create a job for.
     * @return The scheduled crafting job.
     */
    public static CraftingJob scheduleCraftingJob(ICraftingNetwork craftingNetwork, int channel,
                                           PrioritizedRecipe recipeDefinition) {
        CraftingJob craftingJob = new CraftingJob(channel, recipeDefinition);
        craftingNetwork.scheduleCraftingJob(craftingJob);
        return craftingJob;
    }

    /**
     * Schedule a crafting job for the given instance in the given network.
     * @param craftingNetwork The target crafting network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to craft.
     * @param matchCondition The match condition of the instance.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The scheduled crafting job, or null if no recipe was found.
     */
    @Nullable
    public static <T, M> CraftingJob scheduleCraftingJob(ICraftingNetwork craftingNetwork, int channel,
                                                               IngredientComponent<T, M> ingredientComponent,
                                                               T instance, M matchCondition) {
        IRecipeIndex recipeIndex = craftingNetwork.getRecipeIndex(channel);
        Iterator<PrioritizedRecipe> recipes = recipeIndex.getRecipes(ingredientComponent, instance, matchCondition);
        if (recipes.hasNext()) {
            return scheduleCraftingJob(craftingNetwork, channel, recipes.next());
        }
        return null;
    }

    /**
     * Check if there is a scheduled crafting job for the given instance.
     * @param craftingNetwork The target crafting network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to check.
     * @param matchCondition The match condition of the instance.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return If the instance has a crafting job.
     */
    public static <T, M> boolean isCrafting(ICraftingNetwork craftingNetwork, int channel,
                                            IngredientComponent<T, M> ingredientComponent,
                                            T instance, M matchCondition) {
        Iterator<CraftingJob> craftingJobs = craftingNetwork.getCraftingJobs(channel, ingredientComponent,
                instance, matchCondition);
        return craftingJobs.hasNext();
    }

    /**
     * Check if the given network contains the given instance in any of its storages.
     * @param network The target network.
     * @param channel The target channel.
     * @param ingredientComponent The ingredient component type of the instance.
     * @param instance The instance to check.
     * @param matchCondition The match condition of the instance.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return If the instance is present in the network.
     */
    public static <T, M> boolean hasStorageInstance(INetwork network, int channel,
                                                    IngredientComponent<T, M> ingredientComponent,
                                                    T instance, M matchCondition) {
        IPositionedAddonsNetworkIngredients<T, M> storage = ingredientComponent
                .getCapability(Capabilities.POSITIONED_ADDONS_NETWORK_INGREDIENTS_HANDLER)
                .getStorage(network);
        if (storage != null) {
            return storage.getChannel(channel).iterator(instance, matchCondition).hasNext();
        }
        return false;
    }
}
