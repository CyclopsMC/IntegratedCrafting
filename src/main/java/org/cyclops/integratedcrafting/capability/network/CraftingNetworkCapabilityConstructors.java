package org.cyclops.integratedcrafting.capability.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.cyclops.cyclopscore.modcompat.capabilities.DefaultCapabilityProvider;
import org.cyclops.integratedcrafting.core.network.CraftingNetwork;
import org.cyclops.integrateddynamics.Reference;
import org.cyclops.integrateddynamics.api.network.AttachCapabilitiesEventNetwork;

/**
 * Constructor event for network capabilities.
 * @author rubensworks
 */
public class CraftingNetworkCapabilityConstructors {

    @SubscribeEvent
    public void onNetworkLoad(AttachCapabilitiesEventNetwork event) {
        event.addCapability(new ResourceLocation(Reference.MOD_ID, "crafting_network"),
                new DefaultCapabilityProvider<>(() -> CraftingNetworkConfig.CAPABILITY, new CraftingNetwork()));
    }

}
