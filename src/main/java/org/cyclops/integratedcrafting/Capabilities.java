package org.cyclops.integratedcrafting;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeHandler;
import org.cyclops.integrateddynamics.api.ingredient.capability.IPositionedAddonsNetworkIngredientsHandler;

/**
 * Used capabilities for this mod.
 * @author rubensworks
 */
public class Capabilities {
    public static Capability<IRecipeHandler> RECIPE_HANDLER = CapabilityManager.get(new CapabilityToken<>(){});
    public static Capability<IPositionedAddonsNetworkIngredientsHandler> POSITIONED_ADDONS_NETWORK_INGREDIENTS_HANDLER = CapabilityManager.get(new CapabilityToken<>(){});
}
