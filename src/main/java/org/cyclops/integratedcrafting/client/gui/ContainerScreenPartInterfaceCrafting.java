package org.cyclops.integratedcrafting.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenExtended;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.client.gui.image.Images;
import org.cyclops.cyclopscore.helper.GuiHelpers;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingVariableBase;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipartAspects;

import java.util.Collections;
import java.util.Optional;

/**
 * Gui for the crafting interface.
 * @author rubensworks
 */
public class ContainerScreenPartInterfaceCrafting extends ContainerScreenExtended<ContainerPartInterfaceCrafting> {

    private static final int BUTTON_SETTINGS_X = 155;

    public ContainerScreenPartInterfaceCrafting(ContainerPartInterfaceCrafting container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(new ButtonImage(this.leftPos + BUTTON_SETTINGS_X, this.topPos + 4, 15, 15,
                Component.translatable("gui.integrateddynamics.part_settings"),
                createServerPressable(ContainerMultipartAspects.BUTTON_SETTINGS, b -> {}), true,
                Images.CONFIG_BOARD, -2, -3));
    }

    @Override
    protected ResourceLocation constructGuiTexture() {
        return ((PartTypeInterfaceCraftingVariableBase<?, ?>) getMenu().getPartType()).getGuiTexture();
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        int y = topPos + 42;
        int slotsX = ContainerPartInterfaceCrafting.getVariableSlotsX(getMenu().getContainerInventory().getContainerSize());
        for (int i = 0; i < getMenu().getContainerInventory().getContainerSize(); i++) {
            int x = leftPos + slotsX + 2 + i * GuiHelpers.SLOT_SIZE;
            if (!getMenu().getContainerInventory().getItem(i).isEmpty()) {
                IImage image = container.isRecipeSlotValid(i) ? Images.OK : Images.ERROR;
                image.draw(guiGraphics, x, y);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Shrink the title if it would otherwise run into the settings button,
        // as part names vary in length across part types and translations.
        int titleMaxWidth = BUTTON_SETTINGS_X - this.titleLabelX - 2;
        float titleScale = Math.min(1F, (float) titleMaxWidth / this.font.width(this.title));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) this.titleLabelX, (float) this.titleLabelY, 0F);
        guiGraphics.pose().scale(titleScale, titleScale, 1F);
        this.font.drawInBatch(this.title, 0F, 0F, 4210752, false,
                guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);
        guiGraphics.pose().popPose();

        int y = 42;
        int slotsX = ContainerPartInterfaceCrafting.getVariableSlotsX(getMenu().getContainerInventory().getContainerSize());
        for (int i = 0; i < getMenu().getContainerInventory().getContainerSize(); i++) {
            int x = slotsX + 2 + i * GuiHelpers.SLOT_SIZE;
            int slot = i;
            GuiHelpers.renderTooltipOptional(this, guiGraphics.pose(), x, y, 14, 13, mouseX, mouseY,
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
