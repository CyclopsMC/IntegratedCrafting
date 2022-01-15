package org.cyclops.integratedcrafting.capability.network;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.cyclops.commoncapabilities.CommonCapabilities;
import org.cyclops.cyclopscore.config.extendedconfig.CapabilityConfig;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;

/**
 * Config for the crafting network capability.
 * @author rubensworks
 *
 */
public class CraftingNetworkConfig extends CapabilityConfig<ICraftingNetwork> {

    public static Capability<ICraftingNetwork> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    public CraftingNetworkConfig() {
        super(
                CommonCapabilities._instance,
                "craftingNetwork",
                ICraftingNetwork.class
        );
    }

}
