package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.cyclops.cyclopscore.inventory.IGuiContainerProvider;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.inventory.container.InventoryContainer;
import org.cyclops.cyclopscore.inventory.container.button.IButtonActionServer;
import org.cyclops.integratedcrafting.client.gui.GuiPartInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.item.IVariableFacade;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.client.gui.ExtendedGuiHandler;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueHelpers;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipart;
import org.cyclops.integrateddynamics.core.inventory.container.slot.SlotVariable;
import org.cyclops.integrateddynamics.item.ItemVariable;

/**
 * Container for the crafting interface.
 * @author rubensworks
 */
public class ContainerPartInterfaceCrafting extends ContainerMultipart<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    /**
     * Make a new instance.
     * @param target        The target.
     * @param player        The player.
     * @param partContainer The part container.
     * @param partType      The part type.
     */
    public ContainerPartInterfaceCrafting(EntityPlayer player, PartTarget target, IPartContainer partContainer, PartTypeInterfaceCrafting partType) {
        super(player, target, partContainer, partType);

        putButtonAction(GuiPartInterfaceCrafting.BUTTON_SETTINGS, new IButtonActionServer<InventoryContainer>() {
            @Override
            public void onAction(int buttonId, InventoryContainer container) {
                if (!player.world.isRemote) {
                    IGuiContainerProvider gui = getPartType().getSettingsGuiProvider();
                    IntegratedDynamics._instance.getGuiHandler().setTemporaryData(ExtendedGuiHandler.PART, getTarget().getCenter().getSide()); // Pass the side as extra data to the gui
                    BlockPos cPos = getTarget().getCenter().getPos().getBlockPos();
                    ContainerPartInterfaceCrafting.this.player.openGui(gui.getModGui(), gui.getGuiID(),
                            player.world, cPos.getX(), cPos.getY(), cPos.getZ());
                }
            }
        });

        SimpleInventory inventory = getPartState().getInventoryVariables();
        addInventory(inventory, 0, 8, 22, 1, inventory.getSizeInventory());
        addPlayerInventory(player.inventory, 8, 52);
    }

    @Override
    protected Slot createNewSlot(IInventory inventory, int index, int x, int y) {
        if (inventory instanceof SimpleInventory) {
            return new SlotVariable(inventory, index, x, y) {
                @Override
                public boolean isItemValid(ItemStack itemStack) {
                    IVariableFacade variableFacade = ItemVariable.getInstance().getVariableFacade(itemStack);
                    return variableFacade != null
                            && ValueHelpers.correspondsTo(variableFacade.getOutputType(), ValueTypes.OBJECT_RECIPE)
                            && super.isItemValid(itemStack);
                }
            };
        }
        return super.createNewSlot(inventory, index, x, y);
    }

    @Override
    protected int getSizeInventory() {
        return getPartState().getInventoryVariables().getSizeInventory();
    }

    @Override
    public void onDirty() {

    }
}
