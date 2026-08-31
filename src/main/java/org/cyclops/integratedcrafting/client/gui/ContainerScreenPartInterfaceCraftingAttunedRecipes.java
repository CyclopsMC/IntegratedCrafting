package org.cyclops.integratedcrafting.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonImage;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonText;
import org.cyclops.cyclopscore.client.gui.container.ContainerScreenScrolling;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.client.gui.image.Images;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedRecipes;

import java.awt.Rectangle;
import java.util.List;

/**
 * Gui that lists all recipes of an attuned crafting interface.
 * @author rubensworks
 */
public class ContainerScreenPartInterfaceCraftingAttunedRecipes extends ContainerScreenScrolling<ContainerPartInterfaceCraftingAttunedRecipes> {

    private static final int LIST_X = 9;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 160;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTONS_Y = 114;
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 14;
    /**
     * The maximum number of recipe inputs that are shown in a tooltip.
     */
    private static final int MAX_TOOLTIP_INPUTS = 9;

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
        return 213;
    }

    @Override
    protected int getScrollHeight() {
        return ROW_HEIGHT * ContainerPartInterfaceCraftingAttunedRecipes.PAGE_SIZE + 4;
    }

    @Override
    protected Rectangle getScrollRegion() {
        return new Rectangle(this.leftPos + LIST_X, this.topPos + LIST_Y,
                LIST_WIDTH, ROW_HEIGHT * ContainerPartInterfaceCraftingAttunedRecipes.PAGE_SIZE);
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
    }

    protected void addBulkActionButton(int index, String name, int action) {
        Component label = Component.translatable("gui.integratedcrafting.partinterface.recipes." + name);
        addRenderableWidget(new ButtonText(this.leftPos + LIST_X + index * (BUTTON_WIDTH + 2), this.topPos + BUTTONS_Y,
                BUTTON_WIDTH, BUTTON_HEIGHT, label, label,
                (button) -> getMenu().applyBulkAction(action), true));
    }

    protected int getRowY(int row) {
        return LIST_Y + ROW_HEIGHT * row;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            for (int i = 0; i < getMenu().getPageSize(); i++) {
                if (getMenu().isElementVisible(i)
                        && isHovering(LIST_X, getRowY(i), LIST_WIDTH, ROW_HEIGHT - 1, mouseX, mouseY)) {
                    IRecipeDefinition recipe = getMenu().getVisibleElement(i);
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
                this.title.getString(), this.leftPos + offsetX + 6, this.topPos + offsetY + 10, 70,
                4210752, false, Font.DisplayMode.NORMAL);

        ContainerPartInterfaceCraftingAttunedRecipes container = getMenu();
        for (int i = 0; i < container.getPageSize(); i++) {
            if (!container.isElementVisible(i)) {
                continue;
            }
            IRecipeDefinition recipe = container.getVisibleElement(i);
            ContainerPartInterfaceCraftingAttunedRecipes.RecipeEntry entry = container.getEntry(recipe);
            boolean enabled = container.isRecipeEnabled(recipe);
            int x = this.leftPos + offsetX + LIST_X;
            int y = this.topPos + offsetY + getRowY(i);

            // Row background
            if (enabled) {
                RenderSystem.setShaderColor(0.84F, 1F, 0.84F, 1F);
            } else {
                RenderSystem.setShaderColor(0.62F, 0.62F, 0.62F, 1F);
            }
            guiGraphics.blit(this.texture, x, y, 0, getBaseYSize(), LIST_WIDTH, ROW_HEIGHT - 1);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            // Output icon
            ItemStack outputItem = ContainerPartInterfaceCraftingAttunedRecipes.getOutputItem(recipe);
            if (!outputItem.isEmpty()) {
                guiGraphics.renderItem(outputItem, x + 1, y);
                guiGraphics.renderItemDecorations(font, outputItem, x + 1, y);
            }

            // Output name
            RenderHelpers.drawScaledCenteredString(guiGraphics.pose(), guiGraphics.bufferSource(), font,
                    entry.displayName().getString(), x + 22, y + 8, 108,
                    enabled ? Helpers.RGBToInt(40, 40, 40) : Helpers.RGBToInt(105, 105, 105),
                    false, Font.DisplayMode.NORMAL);

            // Enabled indicator
            (enabled ? Images.OK : Images.ERROR).draw(guiGraphics, x + 143, y + 2);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ContainerPartInterfaceCraftingAttunedRecipes container = getMenu();
        for (int i = 0; i < container.getPageSize(); i++) {
            if (container.isElementVisible(i)
                    && isHovering(LIST_X, getRowY(i), LIST_WIDTH, ROW_HEIGHT - 1, mouseX, mouseY)) {
                drawTooltip(getRecipeTooltip(container, container.getVisibleElement(i)),
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

    protected List<Component> getRecipeTooltip(ContainerPartInterfaceCraftingAttunedRecipes container,
                                               IRecipeDefinition recipe) {
        ContainerPartInterfaceCraftingAttunedRecipes.RecipeEntry entry = container.getEntry(recipe);
        boolean enabled = container.isRecipeEnabled(recipe);

        List<Component> lines = Lists.newArrayList();
        lines.add(entry.displayName().copy().withStyle(ChatFormatting.WHITE));
        lines.add(Component.literal(entry.identifier()).withStyle(ChatFormatting.DARK_GRAY));

        lines.add(Component.translatable("gui.integratedcrafting.partinterface.recipes.inputs")
                .withStyle(ChatFormatting.GRAY));
        int shownInputs = 0;
        for (IngredientComponent<?, ?> component : recipe.getInputComponents()) {
            for (IPrototypedIngredientAlternatives<?, ?> alternatives : getInputs(recipe, component)) {
                if (shownInputs >= MAX_TOOLTIP_INPUTS) {
                    lines.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
                    shownInputs++;
                    break;
                }
                Component inputName = getInputName(alternatives);
                if (inputName != null) {
                    lines.add(Component.literal("- ").withStyle(ChatFormatting.YELLOW).append(inputName));
                    shownInputs++;
                }
            }
            if (shownInputs > MAX_TOOLTIP_INPUTS) {
                break;
            }
        }

        lines.add(Component.translatable(enabled
                        ? "gui.integratedcrafting.partinterface.recipes.click_disable"
                        : "gui.integratedcrafting.partinterface.recipes.click_enable")
                .withStyle(ChatFormatting.AQUA));
        return lines;
    }

    protected static <T, M> List<IPrototypedIngredientAlternatives<T, M>> getInputs(IRecipeDefinition recipe,
                                                                                    IngredientComponent<T, M> component) {
        return recipe.getInputs(component);
    }

    protected Component getInputName(IPrototypedIngredientAlternatives<?, ?> alternatives) {
        for (IPrototypedIngredient<?, ?> alternative : alternatives.getAlternatives()) {
            Object prototype = alternative.getPrototype();
            if (prototype instanceof ItemStack itemStack) {
                if (itemStack.isEmpty()) {
                    continue;
                }
                return Component.literal(itemStack.getCount() + "x ").append(itemStack.getHoverName());
            }
            return Component.literal(String.valueOf(prototype));
        }
        return null;
    }

}
