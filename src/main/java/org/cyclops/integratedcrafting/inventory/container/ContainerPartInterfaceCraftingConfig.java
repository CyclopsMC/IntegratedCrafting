package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.world.flag.FeatureFlags;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigCommon;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;

/**
 * Config for {@link ContainerPartInterfaceCrafting}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingConfig extends GuiConfigCommon<ContainerPartInterfaceCrafting, IModBase> {

    public ContainerPartInterfaceCraftingConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCrafting::new, FeatureFlags.VANILLA_SET));
    }

    @Override
    public GuiConfigScreenFactoryProvider<ContainerPartInterfaceCrafting> getScreenFactoryProvider() {
        return new ContainerPartInterfaceCraftingConfigScreenFactoryProvider();
    }
}
