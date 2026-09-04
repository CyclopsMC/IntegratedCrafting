package org.cyclops.integratedcrafting.client.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
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

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.List;
import java.util.Set;

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
     * The bulk action buttons are spread out over the full width of the grid.
     */
    private static final int BUTTON_OFFSET = 55;
    private static final String[] BULK_ACTION_NAMES = {"enableall", "disableall", "invert"};

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

    /**
     * The minimum time in milliseconds between two clicking sounds while dragging,
     * as dragging over a full row would otherwise produce a burst of clicks.
     */
    private static final long TOGGLE_SOUND_INTERVAL = 100;
    /**
     * The maximum distance in pixels between two positions that a drag is applied at.
     */
    private static final double DRAG_STEP = 4;
    private static final int MAX_DRAG_STEPS = 100;

    /**
     * The recipes that the drag that is in progress has passed over,
     * or null if no drag is in progress.
     */
    private Set<IRecipeDefinition> draggedRecipes = null;
    /**
     * The state that the drag that is in progress applies to the recipes it passes over.
     */
    private boolean draggedEnabled = false;
    private long lastToggleSoundTime = 0;

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
        addBulkActionButton(0, ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_ENABLE);
        addBulkActionButton(1, ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_DISABLE);
        addBulkActionButton(2, ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_INVERT);
    }

    protected void addBulkActionButton(int index, int action) {
        Component label = Component.translatable(getBulkActionKey(index));
        addRenderableWidget(new ButtonText(this.leftPos + getBulkActionX(index), this.topPos + BUTTONS_Y,
                BUTTON_WIDTH, BUTTON_HEIGHT, label, label,
                (button) -> getMenu().applyBulkAction(action), true));
    }

    protected static String getBulkActionKey(int index) {
        return "gui.integratedcrafting.partinterface.recipes." + BULK_ACTION_NAMES[index];
    }

    protected int getBulkActionX(int index) {
        return GRID_X + index * BUTTON_OFFSET;
    }

    /**
     * @param mouseX The absolute mouse x position.
     * @param mouseY The absolute mouse y position.
     * @return The index of the grid cell under the mouse, or -1 if the mouse is not over a cell.
     */
    protected int getCellIndexAt(double mouseX, double mouseY) {
        return getCellIndexAt(mouseX, mouseY, true);
    }

    /**
     * @param mouseX The absolute mouse x position.
     * @param mouseY The absolute mouse y position.
     * @param skipCellBorders If the border between two cells should not count as a cell.
     * @return The index of the grid cell under the mouse, or -1 if the mouse is not over a cell.
     */
    protected int getCellIndexAt(double mouseX, double mouseY, boolean skipCellBorders) {
        int relativeX = (int) (mouseX - this.leftPos - this.offsetX - GRID_X);
        int relativeY = (int) (mouseY - this.topPos - this.offsetY - GRID_Y);
        if (relativeX < 0 || relativeY < 0 || relativeX >= getGridWidth() || relativeY >= getGridHeight()) {
            return -1;
        }
        // Ignore the border between two cells
        if (skipCellBorders && (relativeX % GuiHelpers.SLOT_SIZE >= GuiHelpers.SLOT_SIZE_INNER
                || relativeY % GuiHelpers.SLOT_SIZE >= GuiHelpers.SLOT_SIZE_INNER)) {
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
            IRecipeDefinition recipe = getRecipeAt(mouseX, mouseY);
            if (recipe != null) {
                // The recipe that is clicked first determines the state that the whole drag applies,
                // so that dragging can both enable and disable recipes.
                this.draggedRecipes = Sets.newIdentityHashSet();
                this.draggedEnabled = !getMenu().isRecipeEnabled(recipe);
                applyDragTo(recipe);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        if (this.draggedRecipes != null && mouseButton == 0) {
            // The mouse can move over several cells between two events,
            // so the whole path since the previous position is walked over.
            int steps = Mth.clamp((int) Math.ceil(Math.max(Math.abs(dragX), Math.abs(dragY)) / DRAG_STEP),
                    1, MAX_DRAG_STEPS);
            for (int step = 1; step <= steps; step++) {
                applyDragAt(mouseX - dragX * (steps - step) / steps, mouseY - dragY * (steps - step) / steps);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (this.draggedRecipes != null && mouseButton == 0) {
            this.draggedRecipes = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    /**
     * @param mouseX The absolute mouse x position.
     * @param mouseY The absolute mouse y position.
     * @return The recipe in the grid cell under the mouse, or null if there is none.
     */
    @Nullable
    protected IRecipeDefinition getRecipeAt(double mouseX, double mouseY) {
        int index = getCellIndexAt(mouseX, mouseY);
        return index >= 0 ? getMenu().getVisibleElement(index) : null;
    }

    /**
     * Apply the state of the drag that is in progress to the recipe at the given position, if any.
     *
     * @param mouseX The absolute mouse x position.
     * @param mouseY The absolute mouse y position.
     */
    protected void applyDragAt(double mouseX, double mouseY) {
        // The borders between cells are dragged over as well,
        // so that a drag is not interrupted by the single pixel between two cells.
        int index = getCellIndexAt(mouseX, mouseY, false);
        IRecipeDefinition recipe = index >= 0 ? getMenu().getVisibleElement(index) : null;
        if (recipe != null) {
            applyDragTo(recipe);
        }
    }

    /**
     * Apply the state of the drag that is in progress to the given recipe.
     *
     * Recipes that the drag already passed over are skipped,
     * so that moving back and forth over a recipe does not toggle it repeatedly.
     *
     * @param recipe One of the recipes in this container.
     */
    protected void applyDragTo(IRecipeDefinition recipe) {
        if (!this.draggedRecipes.add(recipe) || getMenu().isRecipeEnabled(recipe) == this.draggedEnabled) {
            return;
        }
        getMenu().setRecipeEnabled(recipe, this.draggedEnabled);

        long time = Util.getMillis();
        if (time - this.lastToggleSoundTime >= TOGGLE_SOUND_INTERVAL) {
            this.lastToggleSoundTime = time;
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
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

            boolean enabled = container.isRecipeEnabled(recipe);

            // The state border is inset by a pixel, so that the slot's own border stays visible,
            // and is drawn before the output icon and its count so that it does not cut through them.
            guiGraphics.renderOutline(x, y, GuiHelpers.SLOT_SIZE_INNER, GuiHelpers.SLOT_SIZE_INNER,
                    enabled ? COLOR_BORDER_ENABLED : COLOR_BORDER_DISABLED);
            if (RenderHelpers.isPointInRegion(x, y, GuiHelpers.SLOT_SIZE_INNER, GuiHelpers.SLOT_SIZE_INNER,
                    mouseX, mouseY)) {
                guiGraphics.fill(x, y, x + GuiHelpers.SLOT_SIZE_INNER, y + GuiHelpers.SLOT_SIZE_INNER, COLOR_HOVER);
            }

            ItemStack outputItem = ContainerPartInterfaceCraftingAttunedRecipes.getOutputItem(recipe);
            if (!outputItem.isEmpty()) {
                guiGraphics.renderItem(outputItem, x, y);
                guiGraphics.renderItemDecorations(font, outputItem, x, y);
            }

            if (!enabled) {
                // Draw in front of the output, which is rendered as a 3D item
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 300);
                guiGraphics.fill(x, y, x + GuiHelpers.SLOT_SIZE_INNER, y + GuiHelpers.SLOT_SIZE_INNER, COLOR_DISABLED);
                guiGraphics.pose().popPose();
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // The tooltip is hidden while dragging, as it would cover the cells that are being dragged over
        if (this.draggedRecipes == null) {
            IRecipeDefinition recipe = getRecipeAt(mouseX, mouseY);
            if (recipe != null) {
                renderRecipeTooltip(guiGraphics, recipe, mouseX, mouseY);
            }
        }

        // The button labels are kept short enough to fit, so what they act on is in their tooltip
        for (int i = 0; i < BULK_ACTION_NAMES.length; i++) {
            if (isHovering(getBulkActionX(i), BUTTONS_Y, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
                drawTooltip(Lists.newArrayList(Component.translatable(getBulkActionKey(i) + ".info")),
                        guiGraphics.pose(), mouseX - this.leftPos, mouseY - this.topPos);
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
        elements.add(Either.left(Component.translatable("gui.integratedcrafting.partinterface.recipes.click_drag")
                .withStyle(ChatFormatting.DARK_AQUA)));

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
