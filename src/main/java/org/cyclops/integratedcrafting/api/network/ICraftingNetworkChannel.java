package org.cyclops.integratedcrafting.api.network;

import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;

/**
 * A single crafting network channel.
 * @author rubensworks
 */
public interface ICraftingNetworkChannel extends IRecipeIndex {

    /**
     * @return The channel ID.
     */
    public int getChannel();

    /**
     * Add a crafting interface to the network.
     * @param craftingInterface A crafting interface.
     * @return If the crafting interface did not exist before in the network.
     */
    public boolean addCraftingInterface(ICraftingInterface craftingInterface);

    /**
     * Remove a crafting interface from the network.
     * @param craftingInterface A crafting interface.
     * @return If the crafting interface existed.
     */
    public boolean removeCraftingInterface(ICraftingInterface craftingInterface);

}
