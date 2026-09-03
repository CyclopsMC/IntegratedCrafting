package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfig;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.client.gui.ContainerScreenPartInterfaceCraftingAttunedOffsets;

/**
 * Config for {@link ContainerPartInterfaceCraftingAttunedOffsets}.
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedOffsetsConfig extends GuiConfig<ContainerPartInterfaceCraftingAttunedOffsets> {

    public ContainerPartInterfaceCraftingAttunedOffsetsConfig() {
        super(IntegratedCrafting._instance,
                "part_interface_crafting_attuned_offsets",
                eConfig -> new ContainerTypeData<>(ContainerPartInterfaceCraftingAttunedOffsets::new, FeatureFlags.VANILLA_SET));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <U extends Screen & MenuAccess<ContainerPartInterfaceCraftingAttunedOffsets>> MenuScreens.ScreenConstructor<ContainerPartInterfaceCraftingAttunedOffsets, U> getScreenFactory() {
        return new ScreenFactorySafe<>(ContainerScreenPartInterfaceCraftingAttunedOffsets::new);
    }

}
