package org.cyclops.integratedcrafting.capability.network;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.cyclops.commoncapabilities.CommonCapabilities;
import org.cyclops.cyclopscore.config.extendedconfig.CapabilityConfig;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;

/**
 * Config for the crafting interface capability.
 * @author rubensworks
 *
 */
public class CraftingInterfaceConfig extends CapabilityConfig<ICraftingInterface> {

    public static Capability<ICraftingInterface> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    public CraftingInterfaceConfig() {
        super(
                CommonCapabilities._instance,
                "craftingInterface",
                ICraftingInterface.class
        );
    }

}
