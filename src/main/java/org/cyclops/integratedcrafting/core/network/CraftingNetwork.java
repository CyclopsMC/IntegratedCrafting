package org.cyclops.integratedcrafting.core.network;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.network.ICraftingNetworkChannel;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndexModifiable;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.core.RecipeIndexDefault;

import java.util.Map;
import java.util.Set;

/**
 * A crafting handler network with multiple channels.
 * @author rubensworks
 */
public class CraftingNetwork implements ICraftingNetwork {

    private final TIntObjectMap<Set<ICraftingInterface>> craftingInterfaces = new TIntObjectHashMap<>();
    private final TIntObjectMap<IRecipeIndexModifiable> recipeIndexes = new TIntObjectHashMap<>();
    private final TIntObjectMap<Map<PrioritizedRecipe, ICraftingInterface>> recipeCraftingInterfaces = new TIntObjectHashMap<>();

    @Override
    public ICraftingNetworkChannel getChannel(int channel) {
        return new CraftingNetworkChannel(this, channel);
    }

    public Set<ICraftingInterface> getCraftingInterfaces(int channel) {
        Set<ICraftingInterface> craftingInterfaces = this.craftingInterfaces.get(channel);
        if (craftingInterfaces == null) {
            craftingInterfaces = Sets.newHashSet();
            this.craftingInterfaces.put(channel, craftingInterfaces);
        }
        return craftingInterfaces;
    }

    public IRecipeIndexModifiable getRecipeIndex(int channel) {
        IRecipeIndexModifiable recipeIndex = this.recipeIndexes.get(channel);
        if (recipeIndex == null) {
            recipeIndex = new RecipeIndexDefault();
            this.recipeIndexes.put(channel, recipeIndex);
        }
        return recipeIndex;
    }

    public Map<PrioritizedRecipe, ICraftingInterface> getRecipeCraftingInterfaces(int channel) {
        Map<PrioritizedRecipe, ICraftingInterface> recipeCraftingInterfaces = this.recipeCraftingInterfaces.get(channel);
        if (recipeCraftingInterfaces == null) {
            recipeCraftingInterfaces = Maps.newIdentityHashMap();
            this.recipeCraftingInterfaces.put(channel, recipeCraftingInterfaces);
        }
        return recipeCraftingInterfaces;
    }

    public void cleanupChannel(int channel) {
        Set<ICraftingInterface> craftingInterfaces = this.craftingInterfaces.get(channel);
        if (craftingInterfaces != null && craftingInterfaces.isEmpty()) {
            this.craftingInterfaces.remove(channel);
            this.recipeIndexes.remove(channel);
            this.recipeCraftingInterfaces.remove(channel);
        }
    }
}
