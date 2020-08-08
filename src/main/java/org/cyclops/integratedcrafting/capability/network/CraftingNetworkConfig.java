package org.cyclops.integratedcrafting.capability.network;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import org.cyclops.commoncapabilities.CommonCapabilities;
import org.cyclops.cyclopscore.config.extendedconfig.CapabilityConfig;
import org.cyclops.cyclopscore.modcompat.capabilities.DefaultCapabilityStorage;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.network.CraftingNetwork;

/**
 * Config for the crafting network capability.
 * @author rubensworks
 *
 */
public class CraftingNetworkConfig extends CapabilityConfig<ICraftingNetwork> {

    @CapabilityInject(ICraftingNetwork.class)
    public static Capability<ICraftingNetwork> CAPABILITY = null;
    public CraftingNetworkConfig() {
        super(
                CommonCapabilities._instance,
                "craftingNetwork",
                ICraftingNetwork.class,
                new DefaultCapabilityStorage<ICraftingNetwork>(),
                CraftingNetwork::new
        );
    }

}
