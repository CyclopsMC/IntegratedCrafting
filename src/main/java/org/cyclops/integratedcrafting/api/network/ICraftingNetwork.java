package org.cyclops.integratedcrafting.api.network;

import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

import java.util.Map;
import java.util.Set;

/**
 * A network capability for crafting.
 * @author rubensworks
 */
public interface ICraftingNetwork {

    /**
     * @return The channels that have at least one active position.
     */
    public int[] getChannels();

    /**
     * Get all crafting interfaces for the given channel.
     * @param channel The crafting channel.
     * @return Crafting interfaces.
     */
    public Set<ICraftingInterface> getCraftingInterfaces(int channel);

    /**
     * Get the recipe to interface mapping for the given channel.
     * @param channel The crafting channel.
     * @return The recipe to interface mapping.
     */
    public Map<PrioritizedRecipe, ICraftingInterface> getRecipeCraftingInterfaces(int channel);

    /**
     * Get the recipe index on the given channel.
     * @param channel The crafting channel.
     * @return The index.
     */
    public IRecipeIndex getRecipeIndex(int channel);

    /**
     * Add a crafting interface to the network.
     * @param channel The channel of the interface.
     * @param craftingInterface A crafting interface.
     * @return If the crafting interface did not exist before in the network.
     */
    public boolean addCraftingInterface(int channel, ICraftingInterface craftingInterface);

    /**
     * Remove a crafting interface from the network.
     * @param channel The channel of the interface.
     * @param craftingInterface A crafting interface.
     * @return If the crafting interface existed.
     */
    public boolean removeCraftingInterface(int channel, ICraftingInterface craftingInterface);

}
