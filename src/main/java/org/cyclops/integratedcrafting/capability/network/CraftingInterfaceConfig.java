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

    /**
     * The unique instance.
     */
    public static CraftingInterfaceConfig _instance;

    @CapabilityInject(ICraftingInterface.class)
    public static Capability<ICraftingInterface> CAPABILITY = null;

    /**
     * Make a new instance.
     */
    public CraftingInterfaceConfig() {
        super(
                CommonCapabilities._instance,
                true,
                "craftingInterface",
                "A capability for crafting interfaces.",
                ICraftingInterface.class,
                new DefaultCapabilityStorage<ICraftingInterface>(),
                PartTypeInterfaceCrafting.State.class
        );
    }

    @Override
    public boolean isDisableable() {
        return false;
    }

}
