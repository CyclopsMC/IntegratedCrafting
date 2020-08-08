package org.cyclops.integratedcrafting.capability.network;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import org.cyclops.commoncapabilities.CommonCapabilities;
import org.cyclops.cyclopscore.config.extendedconfig.CapabilityConfig;
import org.cyclops.cyclopscore.modcompat.capabilities.DefaultCapabilityStorage;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;

/**
 * Config for the crafting interface capability.
 * @author rubensworks
 *
 */
public class CraftingInterfaceConfig extends CapabilityConfig<ICraftingInterface> {

    @CapabilityInject(ICraftingInterface.class)
    public static Capability<ICraftingInterface> CAPABILITY = null;

    public CraftingInterfaceConfig() {
        super(
                CommonCapabilities._instance,
                "craftingInterface",
                ICraftingInterface.class,
                new DefaultCapabilityStorage<ICraftingInterface>(),
                PartTypeInterfaceCrafting.State::new
        );
    }

}
