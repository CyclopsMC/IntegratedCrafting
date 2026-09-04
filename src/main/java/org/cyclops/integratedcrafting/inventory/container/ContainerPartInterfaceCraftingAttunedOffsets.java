package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.integratedcrafting.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerPartOffset;

import java.util.Optional;

/**
 * Offsets container for the attuned crafting interface.
 *
 * This only exists so that its screen can send players back
 * to the attuned crafting interface gui when they close it.
 *
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedOffsets extends ContainerPartOffset {

    public ContainerPartInterfaceCraftingAttunedOffsets(int id, Inventory playerInventory, RegistryFriendlyByteBuf packetBuffer) {
        this(id, playerInventory, new SimpleContainer(0),
                PartHelpers.readPartTarget(packetBuffer), Optional.empty(), PartHelpers.readPart(packetBuffer));
    }

    public ContainerPartInterfaceCraftingAttunedOffsets(int id, Inventory playerInventory, Container inventory,
                                                        PartTarget target, Optional<IPartContainer> partContainer,
                                                        IPartType partType) {
        super(RegistryEntries.CONTAINER_INTERFACE_CRAFTING_ATTUNED_OFFSETS.get(), id, playerInventory, inventory,
                target, partContainer, partType);
    }

}
