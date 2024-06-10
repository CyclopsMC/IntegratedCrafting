package org.cyclops.integratedcrafting.capability.network;

import net.neoforged.bus.api.SubscribeEvent;
import org.cyclops.cyclopscore.modcompat.capabilities.DefaultCapabilityProvider;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.core.network.CraftingNetwork;
import org.cyclops.integrateddynamics.api.network.AttachCapabilitiesEventNetwork;

/**
 * Constructor event for network capabilities.
 * @author rubensworks
 */
public class CraftingNetworkCapabilityConstructors {

    @SubscribeEvent
    public void onNetworkLoad(AttachCapabilitiesEventNetwork event) {
        event.register(Capabilities.CraftingNetwork.NETWORK, new DefaultCapabilityProvider<>(new CraftingNetwork()));
    }

}
