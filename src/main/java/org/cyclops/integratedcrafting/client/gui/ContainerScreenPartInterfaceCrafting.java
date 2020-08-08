package org.cyclops.integratedcrafting.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.client.gui.image.Images;
import org.cyclops.cyclopscore.helper.GuiHelpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipartAspects;

import java.util.Collections;
import java.util.Optional;

/**
 * Gui for the crafting interface.
 * @author rubensworks
 */
public class ContainerScreenPartInterfaceCrafting extends ContainerScreenExtended<ContainerPartInterfaceCrafting> {

    public ContainerScreenPartInterfaceCrafting(ContainerPartInterfaceCrafting container, PlayerInventory inventory, ITextComponent title) {
        super(container, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addButton(new ButtonImage(this.guiLeft + 155, this.guiTop + 4, 15, 15,
                L10NHelpers.localize("gui.integrateddynamics.part_settings"),
                createServerPressable(ContainerMultipartAspects.BUTTON_SETTINGS, b -> {}), true,
                Images.CONFIG_BOARD, -2, -3));
    }

    @Override
    protected ResourceLocation constructGuiTexture() {
        return new ResourceLocation(Reference.MOD_ID, "textures/gui/part_interface_crafting.png");
    }

    @Override
    protected int getBaseXSize() {
        return 176;
    }

    @Override
    protected int getBaseYSize() {
        return 141;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

        GlStateManager.color4f(1, 1, 1, 1);
        int y = guiTop + 42;
        for (int i = 0; i < getContainer().getContainerInventory().getSizeInventory(); i++) {
            int x = guiLeft + 10 + i * GuiHelpers.SLOT_SIZE;
            if (!getContainer().getContainerInventory().getStackInSlot(i).isEmpty()) {
                IImage image = container.isRecipeSlotValid(i) ? Images.OK : Images.ERROR;
                image.draw(this, x, y);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        int y = 42;
        for (int i = 0; i < getContainer().getContainerInventory().getSizeInventory(); i++) {
            int x = 10 + i * GuiHelpers.SLOT_SIZE;
            int slot = i;
            GuiHelpers.renderTooltipOptional(this, x, y, 14, 13, mouseX, mouseY,
                    () -> {
                        if (!getContainer().getInventory().get(slot).isEmpty()) {
                            ITextComponent unlocalizedMessage = container.getRecipeSlotUnlocalizedMessage(slot);
                            if (unlocalizedMessage != null) {
                                return Optional.of(Collections.singletonList(unlocalizedMessage));
                            }
                        }
                        return Optional.empty();
                    });
        }
    }
}
