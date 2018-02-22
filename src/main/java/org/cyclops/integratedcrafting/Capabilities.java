package org.cyclops.integratedcrafting;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeHandler;

/**
 * Used capabilities for this mod.
 * @author rubensworks
 */
public class Capabilities {
    @CapabilityInject(IRecipeHandler.class)
    public static Capability<IRecipeHandler> RECIPE_HANDLER = null;
}
