package org.cyclops.integratedcrafting.client.gui.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.cyclopscore.client.gui.GuiGraphicsExtended;
import org.cyclops.cyclopscore.helper.GuiHelpers;

import java.util.List;

/**
 * Draws the inputs of a recipe as a grid of slots inside a tooltip.
 *
 * Inputs that accept multiple alternatives, such as tag-based inputs,
 * cycle over their alternatives.
 *
 * @author rubensworks
 */
@OnlyIn(Dist.CLIENT)
public class ClientRecipeInputsTooltip implements ClientTooltipComponent {

    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/slot");
    private static final int SLOT_WIDTH = 18;
    private static final int SLOT_HEIGHT = 20;
    private static final int MAX_COLUMNS = 9;
    private static final int MARGIN_Y = 2;

    /**
     * The number of ticks an alternative is shown before cycling to the next one.
     */
    private static final int TICK_DELAY = 30;

    private final List<List<IPrototypedIngredient<?, ?>>> inputs;
    private final int columns;
    private final int rows;

    public ClientRecipeInputsTooltip(RecipeInputsTooltip tooltip) {
        this.inputs = tooltip.inputs();
        // Spread the inputs evenly over as few rows as possible
        this.rows = Math.max(1, (int) Math.ceil((double) this.inputs.size() / MAX_COLUMNS));
        this.columns = Math.max(1, (int) Math.ceil((double) this.inputs.size() / this.rows));
    }

    @Override
    public int getHeight() {
        return this.rows * SLOT_HEIGHT + MARGIN_Y;
    }

    @Override
    public int getWidth(Font font) {
        return this.columns * SLOT_WIDTH;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        int tick = getTick();
        for (int i = 0; i < this.inputs.size(); i++) {
            List<IPrototypedIngredient<?, ?>> alternatives = this.inputs.get(i);
            int slotX = x + (i % this.columns) * SLOT_WIDTH;
            int slotY = y + (i / this.columns) * SLOT_HEIGHT;
            guiGraphics.blitSprite(SLOT_SPRITE, slotX, slotY, SLOT_WIDTH, SLOT_HEIGHT);
            // Cycle over the alternatives of this input
            drawIngredient(guiGraphics, font, alternatives.get(tick % alternatives.size()), slotX + 1, slotY + 1);
        }
    }

    protected static <T, M> void drawIngredient(GuiGraphics guiGraphics, Font font,
                                                IPrototypedIngredient<T, M> ingredient, int x, int y) {
        T prototype = ingredient.getPrototype();
        if (prototype instanceof ItemStack itemStack) {
            guiGraphics.renderItem(itemStack, x, y);
            guiGraphics.renderItemDecorations(font, itemStack, x, y);
        } else {
            // Other ingredient types have no icon here, so only their quantity is shown
            long quantity = ingredient.getComponent().getMatcher().getQuantity(prototype);
            new GuiGraphicsExtended(guiGraphics).drawSlotText(font,
                    GuiHelpers.quantityToScaledString(quantity), x, y);
        }
    }

    protected static int getTick() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0 : (int) (minecraft.level.getGameTime() / TICK_DELAY);
    }

}
