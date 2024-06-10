package org.cyclops.integratedcrafting;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integrateddynamics.api.network.NetworkCapability;
import org.cyclops.integrateddynamics.api.part.PartCapability;

/**
 * Used capabilities for this mod.
 * @author rubensworks
 */
public class Capabilities {
    public static final class CraftingNetwork {
        public static final NetworkCapability<ICraftingNetwork> NETWORK = NetworkCapability.create(new ResourceLocation(Reference.MOD_ID, "crafting_network"), ICraftingNetwork.class);
    }
    public static final class CraftingInterface {
        public static final PartCapability<ICraftingInterface> PART = PartCapability.create(new ResourceLocation(Reference.MOD_ID, "crafting_interface"), ICraftingInterface.class);
    }
}
