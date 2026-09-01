package org.cyclops.integratedcrafting.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonText;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenScrolling;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.helper.GuiHelpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.client.gui.tooltip.RecipeInputs;
import org.cyclops.integratedcrafting.client.gui.tooltip.RecipeInputsTooltip;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedRecipes;

import java.awt.Rectangle;
import java.util.List;

/**
 * Gui that shows all recipes of an attuned crafting interface in a grid.
 * @author rubensworks
 */
public class ContainerScreenPartInterfaceCraftingAttunedRecipes extends ContainerScreenScrolling<ContainerPartInterfaceCraftingAttunedRecipes> {

    private static final int GRID_X = 9;
    private static final int GRID_Y = 18;
    private static final int BUTTONS_Y = 132;
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 14;

    /**
     * The white overlay that highlights the cell under the mouse.
     */
    private static final int COLOR_HOVER = 0x80FFFFFF;
    /**
     * The overlay that greys out a disabled recipe.
     */
    private static final int COLOR_DISABLED = 0x80303030;
    private static final int COLOR_BORDER_ENABLED = 0xFF44BB44;
    private static final int COLOR_BORDER_DISABLED = 0xFFBB4444;

    public ContainerScreenPartInterfaceCraftingAttunedRecipes(ContainerPartInterfaceCraftingAttunedRecipes container,
                                                              Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected ResourceLocation constructGuiTexture() {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/part_interface_crafting_attuned_recipes.png");
    }

    @Override
    protected int getBaseXSize() {
        return 195;
    }

    @Override
    protected int getBaseYSize() {
        return 231;
    }

    protected int getGridWidth() {
        return getMenu().getColumns() * GuiHelpers.SLOT_SIZE;
    }

    protected int getGridHeight() {
        return getMenu().getPageSize() * GuiHelpers.SLOT_SIZE;
    }

    @Override
    protected int getScrollHeight() {
        return getGridHeight() + 4;
    }

    @Override
    protected Rectangle getScrollRegion() {
        return new Rectangle(this.leftPos + GRID_X, this.topPos + GRID_Y, getGridWidth(), getGridHeight());
    }

    @Override
    public void init() {
        clearWidgets();
        super.init();

        addRenderableWidget(new ButtonImage(this.leftPos - 20, this.topPos, 18, 18,
                Component.translatable("gui.integrateddynamics.part_settings"),
                createServerPressable(ContainerPartInterfaceCraftingAttunedRecipes.BUTTON_SETTINGS, (button) -> {}),
                new IImage[]{
                        org.cyclops.integrateddynamics.client.gui.image.Images.BUTTON_BACKGROUND_INACTIVE,
                        org.cyclops.integrateddynamics.client.gui.image.Images.BUTTON_MIDDLE_SETTINGS
                },
                false, 0, 0));
        if (getMenu().getPartType().supportsOffsets()) {
            addRenderableWidget(new ButtonImage(this.leftPos - 20, this.topPos + 20, 18, 18,
                    Component.translatable("gui.integrateddynamics.part_offsets"),
                    createServerPressable(ContainerPartInterfaceCraftingAttunedRecipes.BUTTON_OFFSETS, (button) -> {}),
                    new IImage[]{
                            org.cyclops.integrateddynamics.client.gui.image.Images.BUTTON_BACKGROUND_INACTIVE,
                            org.cyclops.integrateddynamics.client.gui.image.Images.BUTTON_MIDDLE_OFFSET
                    },
                    false, 0, 0));
        }

        // The bulk actions apply to the recipes that match the current search filter,
        // as toggling thousands of recipes one by one is not workable.
        addBulkActionButton(0, "enableall", ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_ENABLE);
        addBulkActionButton(1, "disableall", ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_DISABLE);
        addBulkActionButton(2, "invert", ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_INVERT);

        getScrollbar().setTotalRows(getTotalGridRows());
    }

    protected void addBulkActionButton(int index, String name, int action) {
        Component label = Component.translatable("gui.integratedcrafting.partinterface.recipes." + name);
        addRenderableWidget(new ButtonText(this.leftPos + GRID_X + index * (BUTTON_WIDTH + 2), this.topPos + BUTTONS_Y,
                BUTTON_WIDTH, BUTTON_HEIGHT, label, label,
                (button) -> getMenu().applyBulkAction(action), true));
    }

    /**
     * The scrollbar rows are rounded up here,
     * as a last row that is only partially filled must still be reachable.
     * {@link ContainerScreenScrolling} rounds down, which would hide it.
     */
    protected int getTotalGridRows() {
        return Mth.ceil((double) getMenu().getFilteredItemCount() / getMenu().getColumns());
    }

    @Override
    protected void updateSearch(String searchString) {
        super.updateSearch(searchString);
        getScrollbar().setTotalRows(getTotalGridRows());
        getScrollbar().scrollTo(0);
    }

    /**
     * @param mouseX The absolute mouse x position.
     * @param mouseY The absolute mouse y position.
     * @return The index of the grid cell under the mouse, or -1 if the mouse is not over a cell.
     */
    protected int getCellIndexAt(double mouseX, double mouseY) {
        int relativeX = (int) (mouseX - this.leftPos - this.offsetX - GRID_X);
        int relativeY = (int) (mouseY - this.topPos - this.offsetY - GRID_Y);
        if (relativeX < 0 || relativeY < 0 || relativeX >= getGridWidth() || relativeY >= getGridHeight()) {
            return -1;
        }
        // Ignore the border between two cells
        if (relativeX % GuiHelpers.SLOT_SIZE >= GuiHelpers.SLOT_SIZE_INNER
                || relativeY % GuiHelpers.SLOT_SIZE >= GuiHelpers.SLOT_SIZE_INNER) {
            return -1;
        }
        return relativeX / GuiHelpers.SLOT_SIZE
                + (relativeY / GuiHelpers.SLOT_SIZE) * getMenu().getColumns();
    }

    protected int getCellX(int index) {
        return this.leftPos + this.offsetX + GRID_X + (index % getMenu().getColumns()) * GuiHelpers.SLOT_SIZE;
    }

    protected int getCellY(int index) {
        return this.topPos + this.offsetY + GRID_Y + (index / getMenu().getColumns()) * GuiHelpers.SLOT_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            int index = getCellIndexAt(mouseX, mouseY);
            if (index >= 0) {
                IRecipeDefinition recipe = getMenu().getVisibleElement(index);
                if (recipe != null) {
                    getMenu().setRecipeEnabled(recipe, !getMenu().isRecipeEnabled(recipe));
                    Minecraft.getInstance().getSoundManager()
                            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);

        RenderHelpers.drawScaledCenteredString(guiGraphics.pose(), guiGraphics.bufferSource(), font,
                this.title.getString(), this.leftPos + this.offsetX + 6, this.topPos + this.offsetY + 10, 70,
                4210752, false, Font.DisplayMode.NORMAL);

        ContainerPartInterfaceCraftingAttunedRecipes container = getMenu();
        int cells = container.getPageSize() * container.getColumns();
        for (int i = 0; i < cells; i++) {
            IRecipeDefinition recipe = container.getVisibleElement(i);
            if (recipe == null) {
                continue;
            }
            int x = getCellX(i);
            int y = getCellY(i);

            if (RenderHelpers.isPointInRegion(x, y, GuiHelpers.SLOT_SIZE_INNER, GuiHelpers.SLOT_SIZE_INNER,
                    mouseX, mouseY)) {
                guiGraphics.fill(x, y, x + GuiHelpers.SLOT_SIZE_INNER, y + GuiHelpers.SLOT_SIZE_INNER, COLOR_HOVER);
            }

            ItemStack outputItem = ContainerPartInterfaceCraftingAttunedRecipes.getOutputItem(recipe);
            if (!outputItem.isEmpty()) {
                guiGraphics.renderItem(outputItem, x, y);
                guiGraphics.renderItemDecorations(font, outputItem, x, y);
            }

            // Draw in front of the output, which is rendered as a 3D item
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            boolean enabled = container.isRecipeEnabled(recipe);
            if (!enabled) {
                guiGraphics.fill(x, y, x + GuiHelpers.SLOT_SIZE_INNER, y + GuiHelpers.SLOT_SIZE_INNER, COLOR_DISABLED);
            }
            guiGraphics.renderOutline(x, y, GuiHelpers.SLOT_SIZE_INNER, GuiHelpers.SLOT_SIZE_INNER,
                    enabled ? COLOR_BORDER_ENABLED : COLOR_BORDER_DISABLED);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int index = getCellIndexAt(mouseX, mouseY);
        if (index >= 0) {
            IRecipeDefinition recipe = getMenu().getVisibleElement(index);
            if (recipe != null) {
                renderRecipeTooltip(guiGraphics, recipe, mouseX, mouseY);
            }
        }

        if (isHovering(-20, 0, 18, 18, mouseX, mouseY)) {
            drawTooltip(Lists.newArrayList(Component.translatable("gui.integrateddynamics.part_settings")),
                    guiGraphics.pose(), mouseX - this.leftPos, mouseY - this.topPos);
        }
        if (getMenu().getPartType().supportsOffsets() && isHovering(-20, 20, 18, 18, mouseX, mouseY)) {
            drawTooltip(Lists.newArrayList(Component.translatable("gui.integrateddynamics.part_offsets")),
                    guiGraphics.pose(), mouseX - this.leftPos, mouseY - this.topPos);
        }
    }

    protected void renderRecipeTooltip(GuiGraphics guiGraphics, IRecipeDefinition recipe, int mouseX, int mouseY) {
        ContainerPartInterfaceCraftingAttunedRecipes.RecipeEntry entry = getMenu().getEntry(recipe);
        Minecraft minecraft = Minecraft.getInstance();

        List<Either<FormattedText, TooltipComponent>> elements = Lists.newArrayList();

        ItemStack outputItem = ContainerPartInterfaceCraftingAttunedRecipes.getOutputItem(recipe);
        if (outputItem.isEmpty()) {
            elements.add(Either.left(entry.displayName()));
        } else {
            for (Component line : outputItem.getTooltipLines(
                    Item.TooltipContext.of(minecraft.level.registryAccess()), minecraft.player,
                    minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)) {
                elements.add(Either.left(line));
            }
        }
        elements.add(Either.left(Component.literal(entry.identifier()).withStyle(ChatFormatting.DARK_GRAY)));

        // The inputs are only determined for the recipe that is actually hovered,
        // as doing this for every shown recipe on every frame would be far too slow.
        List<List<IPrototypedIngredient<?, ?>>> inputs = RecipeInputs.getGroupedInputs(recipe);
        if (!inputs.isEmpty()) {
            elements.add(Either.left(Component.translatable("gui.integratedcrafting.partinterface.recipes.inputs")
                    .withStyle(ChatFormatting.YELLOW)));
            elements.add(Either.right(new RecipeInputsTooltip(inputs)));
        }

        elements.add(Either.left(Component.translatable(getMenu().isRecipeEnabled(recipe)
                        ? "gui.integratedcrafting.partinterface.recipes.click_disable"
                        : "gui.integratedcrafting.partinterface.recipes.click_enable")
                .withStyle(ChatFormatting.AQUA)));

        // Don't write to the depth buffer, so that anything drawn after this tooltip is not occluded by it.
        RenderSystem.disableDepthTest();
        // Tooltips are positioned in screen space, while this layer is translated to the position of the gui.
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(-this.leftPos, -this.topPos, 0);
        guiGraphics.renderComponentTooltipFromElements(font, elements, mouseX, mouseY, ItemStack.EMPTY);
        guiGraphics.pose().popPose();
        RenderSystem.enableDepthTest();
    }

}
