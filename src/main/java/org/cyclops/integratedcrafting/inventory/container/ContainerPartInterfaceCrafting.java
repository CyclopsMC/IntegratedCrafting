package org.cyclops.integratedcrafting.inventory.container;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
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

    public ContainerPartInterfaceCrafting(int id, PlayerInventory playerInventory, PacketBuffer packetBuffer) {
        this(id, playerInventory, new SimpleInventory(packetBuffer.readInt(), 1),
                Optional.empty(), Optional.empty(), PartHelpers.readPart(packetBuffer));
    }

    public ContainerPartInterfaceCrafting(int id, PlayerInventory playerInventory, IInventory inventory,
                                          Optional<PartTarget> target, Optional<IPartContainer> partContainer, PartTypeInterfaceCrafting partType) {
        super(RegistryEntries.CONTAINER_INTERFACE_CRAFTING, id, playerInventory, inventory, target, partContainer, partType);

        addInventory(inventory, 0, 8, 22, 1, inventory.getSizeInventory());
        addPlayerInventory(player.inventory, 8, 59);

        getPartState().ifPresent(p -> p.setLastPlayer(player));

        this.readSlotValidIds = Lists.newArrayList();
        this.readSlotErrorIds = Lists.newArrayList();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            this.readSlotValidIds.add(getNextValueId());
            this.readSlotErrorIds.add(getNextValueId());
        }

        if (!player.getEntityWorld().isRemote()) {
            putButtonAction(ContainerMultipartAspects.BUTTON_SETTINGS, (s, containerExtended) -> {
                PartHelpers.openContainerPartSettings((ServerPlayerEntity) player, target.get().getCenter(), partType);
            });
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        getPartState().ifPresent(partState -> {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ValueNotifierHelpers.setValue(this, this.readSlotValidIds.get(i), partState.isRecipeSlotValid(i));
                ValueNotifierHelpers.setValue(this, this.readSlotErrorIds.get(i), partState.getRecipeSlotUnlocalizedMessage(i));
            }
        });
    }

    public boolean isRecipeSlotValid(int slot) {
        return ValueNotifierHelpers.getValueBoolean(this, this.readSlotValidIds.get(slot));
    }

    @Nullable
    public ITextComponent getRecipeSlotUnlocalizedMessage(int slot) {
        return ValueNotifierHelpers.getValueTextComponent(this, this.readSlotErrorIds.get(slot));
    }

    @Override
    protected Slot createNewSlot(IInventory inventory, int index, int x, int y) {
        if (inventory instanceof SimpleInventory) {
            return new SlotVariable(inventory, index, x, y) {
                @Override
                public boolean isItemValid(ItemStack itemStack) {
                    IVariableFacade variableFacade = RegistryEntries.ITEM_VARIABLE.getVariableFacade(itemStack);
                    return variableFacade != null
                            && ValueHelpers.correspondsTo(variableFacade.getOutputType(), ValueTypes.OBJECT_RECIPE)
                            && super.isItemValid(itemStack);
                }
            };
        }
        return super.createNewSlot(inventory, index, x, y);
    }

    @Override
    public void onDirty() {

    }
}
