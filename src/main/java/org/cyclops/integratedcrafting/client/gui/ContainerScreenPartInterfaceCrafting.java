package org.cyclops.integratedcrafting.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.client.gui.image.Images;
import org.cyclops.cyclopscore.helper.GuiHelpers;
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

    public ContainerScreenPartInterfaceCrafting(ContainerPartInterfaceCrafting container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(new ButtonImage(this.leftPos + 155, this.topPos + 4, 15, 15,
                Component.translatable("gui.integrateddynamics.part_settings"),
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
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        int y = topPos + 42;
        for (int i = 0; i < getMenu().getContainerInventory().getContainerSize(); i++) {
            int x = leftPos + 10 + i * GuiHelpers.SLOT_SIZE;
            if (!getMenu().getContainerInventory().getItem(i).isEmpty()) {
                IImage image = container.isRecipeSlotValid(i) ? Images.OK : Images.ERROR;
                image.draw(this, matrixStack, x, y);
            }
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        // super.drawGuiContainerForegroundLayer(matrixStack, mouseX, mouseY);
        this.font.draw(matrixStack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 4210752);

        int y = 42;
        for (int i = 0; i < getMenu().getContainerInventory().getContainerSize(); i++) {
            int x = 10 + i * GuiHelpers.SLOT_SIZE;
            int slot = i;
            GuiHelpers.renderTooltipOptional(this, matrixStack, x, y, 14, 13, mouseX, mouseY,
                    () -> {
                        if (!getMenu().getItems().get(slot).isEmpty()) {
                            Component unlocalizedMessage = container.getRecipeSlotUnlocalizedMessage(slot);
                            if (unlocalizedMessage != null) {
                                return Optional.of(Collections.singletonList(unlocalizedMessage));
                            }
                        }
                        return Optional.empty();
                    });
        }
    }
}
