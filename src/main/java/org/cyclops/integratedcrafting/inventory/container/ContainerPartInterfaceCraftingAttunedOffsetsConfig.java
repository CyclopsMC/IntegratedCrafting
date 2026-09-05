package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.world.flag.FeatureFlags;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigCommon;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfigScreenFactoryProvider;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;

/**
 * Config for {@link ContainerPartInterfaceCraftingAttunedOffsets}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedOffsetsConfig extends GuiConfigCommon<ContainerPartInterfaceCraftingAttunedOffsets, IModBase> {

    public ContainerPartInterfaceCraftingAttunedOffsetsConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting_attuned_offsets",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCraftingAttunedOffsets::new, FeatureFlags.VANILLA_SET));
    }

    @Override
    public GuiConfigScreenFactoryProvider<ContainerPartInterfaceCraftingAttunedOffsets> getScreenFactoryProvider() {
        return new ContainerPartInterfaceCraftingAttunedOffsetsConfigScreenFactoryProvider();
    }
}
