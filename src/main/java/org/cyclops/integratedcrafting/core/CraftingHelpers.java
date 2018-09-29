package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IngredientComponentStorageEmpty;
import org.cyclops.cyclopscore.helper.TileHelpers;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkConfig;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Helpers related to handling crafting jobs.
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
     * Get the storage network of the given type in the given network.
     * @param network A network.
     * @param ingredientComponent The ingredient component type of the network.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The storage network or null.
     */
    @Nullable
    public static <T, M> IPositionedAddonsNetworkIngredients<T, M> getIngredientsNetwork(INetwork network,
                                                                                         IngredientComponent<T, M> ingredientComponent) {
        return ingredientComponent
                .getCapability(Capabilities.POSITIONED_ADDONS_NETWORK_INGREDIENTS_HANDLER)
                .getStorage(network);
    }

    /**
     * Get the storage of the given ingredient component type from the network.
     * @param network The network.
     * @param channel A network channel.
     * @param ingredientComponent The ingredient component type of the network.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The storage.
     */
    public static <T, M> IIngredientComponentStorage<T, M> getNetworkStorage(INetwork network, int channel,
                                                                             IngredientComponent<T, M> ingredientComponent,
                                                                             boolean scheduleObservation) {
        IPositionedAddonsNetworkIngredients<T, M> ingredientsNetwork = getIngredientsNetwork(network, ingredientComponent);
        if (ingredientsNetwork != null) {
            if (scheduleObservation) {
                ingredientsNetwork.scheduleObservation();
            }
            return ingredientsNetwork.getChannel(channel);
        }
        return new IngredientComponentStorageEmpty<>(ingredientComponent);
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
        return !ingredientComponent.getMatcher().isEmpty(
                getNetworkStorage(network, channel, ingredientComponent, true)
                        .extract(instance, matchCondition, true));
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
     * Get all required recipe input ingredients from the network for the given ingredient component.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * Note: Make sure that you first call in simulation-mode
     * to see if the ingredients are available.
     * If you immediately call this non-simulated,
     * then there might be a chance that ingredients are lost
     * from the network.
     *
     * @param storage The target storage.
     * @param ingredientComponent The ingredient component to get the ingredients for.
     * @param recipe The recipe to get the inputs from.
     * @param simulate If true, then the ingredients will effectively be removed from the network, not when false.
     * @return A list of slot-based ingredients, or null if no valid inputs could be found.
     */
    @Nullable
    public static <T, M> List<T> getIngredientRecipeInputs(IIngredientComponentStorage<T, M> storage,
                                                           IngredientComponent<T, M> ingredientComponent,
                                                           IRecipeDefinition recipe, boolean simulate) {
        // Quickly return if the storage is empty
        if (storage.getMaxQuantity() == 0) {
            return null;
        }

        // Iterate over all input slots
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        List<List<IPrototypedIngredient<T, M>>> inputAlternativePrototypes = recipe.getInputs(ingredientComponent);
        List<T> inputInstances = Lists.newArrayList();
        for (List<IPrototypedIngredient<T, M>> inputPrototypes : inputAlternativePrototypes) {
            T inputInstance = null;
            boolean hasInputInstance = false;

            // Iterate over all alternatives for this input slot, and take the first matching ingredient.
            for (IPrototypedIngredient<T, M> inputPrototype : inputPrototypes) {
                // If the prototype is empty, we can skip network extraction
                if (matcher.isEmpty(inputPrototype.getPrototype())) {
                    inputInstance = inputPrototype.getPrototype();
                    hasInputInstance = true;
                    break;
                }

                T extracted = storage.extract(inputPrototype.getPrototype(), inputPrototype.getCondition(), simulate);
                if (!matcher.isEmpty(extracted)) {
                    inputInstance = extracted;
                    hasInputInstance = true;
                    break;
                }
            }

            // If none of the alternatives were found, fail immediately
            if (!hasInputInstance) {
                // This input failed, return immediately
                // TODO: catch already-extracted items in non-simulation
                return null;
            }

            // Otherwise, append it to the list and carry on.
            inputInstances.add(inputInstance);
        }

        return inputInstances;
    }

    /**
     * Get all required recipe input ingredients from the network.
     *
     * If multiple alternative inputs are possible,
     * then only the first possible match will be taken.
     *
     * Note: Make sure that you first call in simulation-mode
     * to see if the ingredients are available.
     * If you immediately call this non-simulated,
     * then there might be a chance that ingredients are lost
     * from the network.
     *
     * @param network The target network.
     * @param channel The target channel.
     * @param recipe The recipe to get the inputs from.
     * @param simulate If true, then the ingredients will effectively be removed from the network, not when false.
     * @return The found ingredients or null.
     */
    @Nullable
    public static IMixedIngredients getRecipeInputs(INetwork network, int channel,
                                                    IRecipeDefinition recipe, boolean simulate) {
        Map<IngredientComponent<?, ?>, List<?>> ingredients = Maps.newIdentityHashMap();
        for (IngredientComponent<?, ?> ingredientComponent : recipe.getInputComponents()) {
            IIngredientComponentStorage storage = getNetworkStorage(network, channel,
                    ingredientComponent, false);
            List<?> ingredientInputs = getIngredientRecipeInputs(storage, ingredientComponent, recipe, simulate);
            if (ingredientInputs == null) {
                return null;
            }
            ingredients.put(ingredientComponent, ingredientInputs);
        }
        return new MixedIngredients(ingredients);
    }

    /**
     * Create a list of prototyped ingredients from the instances
     * of the given ingredient component type in the given mixed ingredients.
     *
     * Equal prototypes will be stacked.
     *
     * @param ingredientComponent The ingredient component type.
     * @param mixedIngredients The mixed ingredients.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return A list of prototypes.
     */
    public static <T, M> List<IPrototypedIngredient<T, M>> getCompressedIngredients(IngredientComponent<T, M> ingredientComponent,
                                                                                    IMixedIngredients mixedIngredients) {
        List<IPrototypedIngredient<T, M>> outputs = Lists.newArrayList();

        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        for (T instance : mixedIngredients.getInstances(ingredientComponent)) {
            // Try to stack this instance with an existing prototype
            boolean stacked = false;
            ListIterator<IPrototypedIngredient<T, M>> existingIt = outputs.listIterator();
            while(existingIt.hasNext()) {
                IPrototypedIngredient<T, M> prototypedIngredient = existingIt.next();
                if (matcher.matches(instance, prototypedIngredient.getPrototype(),
                        prototypedIngredient.getCondition())) {
                    T stackedInstance = matcher.withQuantity(prototypedIngredient.getPrototype(),
                            matcher.getQuantity(prototypedIngredient.getPrototype())
                                    + matcher.getQuantity(instance));
                    existingIt.set(new PrototypedIngredient<>(ingredientComponent, stackedInstance,
                            prototypedIngredient.getCondition()));
                    stacked = true;
                    break;
                }
            }

            // If not possible, just append it to the list
            if (!stacked) {
                outputs.add(new PrototypedIngredient<>(ingredientComponent, instance,
                        matcher.getExactMatchNoQuantityCondition()));
            }
        }

        return outputs;
    }

    /**
     * Create a collection of prototypes from the given recipe's outputs.
     *
     * Equal prototypes will be stacked.
     *
     * @param recipe A recipe.
     * @return A map from ingredient component types to their list of prototypes.
     */
    public static Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> getRecipeOutputs(IRecipeDefinition recipe) {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> outputs = Maps.newHashMap();

        IMixedIngredients mixedIngredients = recipe.getOutput();
        for (IngredientComponent ingredientComponent : mixedIngredients.getComponents()) {
            outputs.put(ingredientComponent, getCompressedIngredients(ingredientComponent, mixedIngredients));
        }

        return outputs;
    }

    /**
     * Insert the ingredients of the given ingredient component type into the target to make it start crafting.
     * @param ingredientComponent The ingredient component type.
     * @param capabilityProvider The target capability provider.
     * @param side The target side.
     * @param ingredients The ingredients to insert.
     * @param simulate If insertion should be simulated.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return If all instances could be inserted.
     */
    public static <T, M> boolean insertIngredientCrafting(IngredientComponent<T, M> ingredientComponent,
                                                          ICapabilityProvider capabilityProvider,
                                                          @Nullable EnumFacing side,
                                                          IMixedIngredients ingredients, boolean simulate) {
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        IIngredientComponentStorage<T, M> storage = ingredientComponent.getStorage(capabilityProvider, side);
        List<T> instances = ingredients.getInstances(ingredientComponent);
        for (T instance : instances) {
            T remaining = storage.insert(instance, simulate);
            if (!matcher.isEmpty(remaining)) {
                // TODO: handle cases when not simulating?
                return false;
            }
        }
        return true;
    }

    /**
     * Insert the ingredients of all applicable ingredient component types into the target to make it start crafting.
     * @param target The target position.
     * @param ingredients The ingredients to insert.
     * @param simulate If insertion should be simulated.
     * @return If all instances could be inserted.
     */
    public static boolean insertCrafting(PartPos target, IMixedIngredients ingredients, boolean simulate) {
        // TODO: handle custom I/O handler based on Block type

        EnumFacing side = target.getSide();
        TileEntity tile = TileHelpers.getSafeTile(target.getPos(), TileEntity.class);
        if (tile != null) {
            for (IngredientComponent<?, ?> ingredientComponent : ingredients.getComponents()) {
                if (!insertIngredientCrafting(ingredientComponent, tile, side, ingredients, simulate)) {
                    // TODO: handle cases when not simulating?
                    return false;
                }
            }
        }
        return true;
    }

}
