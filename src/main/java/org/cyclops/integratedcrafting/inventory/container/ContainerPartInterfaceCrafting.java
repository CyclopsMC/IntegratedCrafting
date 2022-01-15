package org.cyclops.integratedcrafting.inventory.container;

import com.google.common.collect.Lists;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.integratedcrafting.RegistryEntries;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.api.item.IVariableFacade;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueHelpers;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipart;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipartAspects;
import org.cyclops.integrateddynamics.core.inventory.container.slot.SlotVariable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Container for the crafting interface.
 * @author rubensworks
 */
public class ContainerPartInterfaceCrafting extends ContainerMultipart<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    private final List<Integer> readSlotValidIds;
    private final List<Integer> readSlotErrorIds;

    public ContainerPartInterfaceCrafting(int id, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
        this(id, playerInventory, new SimpleInventory(packetBuffer.readInt(), 1),
                Optional.empty(), Optional.empty(), PartHelpers.readPart(packetBuffer));
    }

    public ContainerPartInterfaceCrafting(int id, Inventory playerInventory, Container inventory,
                                          Optional<PartTarget> target, Optional<IPartContainer> partContainer, PartTypeInterfaceCrafting partType) {
        super(RegistryEntries.CONTAINER_INTERFACE_CRAFTING, id, playerInventory, inventory, target, partContainer, partType);

        addInventory(inventory, 0, 8, 22, 1, inventory.getContainerSize());
        addPlayerInventory(player.getInventory(), 8, 59);

        getPartState().ifPresent(p -> p.setLastPlayer(player));

        this.readSlotValidIds = Lists.newArrayList();
        this.readSlotErrorIds = Lists.newArrayList();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            this.readSlotValidIds.add(getNextValueId());
            this.readSlotErrorIds.add(getNextValueId());
        }

        if (!player.getCommandSenderWorld().isClientSide()) {
            putButtonAction(ContainerMultipartAspects.BUTTON_SETTINGS, (s, containerExtended) -> {
                PartHelpers.openContainerPartSettings((ServerPlayer) player, target.get().getCenter(), partType);
            });
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        getPartState().ifPresent(partState -> {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ValueNotifierHelpers.setValue(this, this.readSlotValidIds.get(i), partState.isRecipeSlotValid(i));
                ValueNotifierHelpers.setValue(this, this.readSlotErrorIds.get(i), partState.getRecipeSlotUnlocalizedMessage(i));
            }
        });
    }

    public boolean isRecipeSlotValid(int slot) {
        return ValueNotifierHelpers.getValueBoolean(this, this.readSlotValidIds.get(slot));
    }

    @Nullable
    public Component getRecipeSlotUnlocalizedMessage(int slot) {
        return ValueNotifierHelpers.getValueTextComponent(this, this.readSlotErrorIds.get(slot));
    }

    @Override
    protected Slot createNewSlot(Container inventory, int index, int x, int y) {
        if (inventory instanceof SimpleInventory) {
            return new SlotVariable(inventory, index, x, y) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    IVariableFacade variableFacade = RegistryEntries.ITEM_VARIABLE.getVariableFacade(itemStack);
                    return variableFacade != null
                            && ValueHelpers.correspondsTo(variableFacade.getOutputType(), ValueTypes.OBJECT_RECIPE)
                            && super.mayPlace(itemStack);
                }
            };
        }
        return super.createNewSlot(inventory, index, x, y);
    }

    @Override
    public void onDirty() {

    }
}
