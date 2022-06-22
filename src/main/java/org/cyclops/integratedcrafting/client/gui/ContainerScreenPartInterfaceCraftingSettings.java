package org.cyclops.integratedcrafting.client.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.client.gui.component.button.ButtonCheckbox;
import org.cyclops.cyclopscore.client.gui.component.input.IInputListener;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetArrowedListField;
import org.cyclops.cyclopscore.client.gui.component.input.WidgetNumberField;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.core.client.gui.WidgetTextFieldDropdown;
import org.cyclops.integrateddynamics.core.client.gui.container.ContainerScreenPartSettings;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rubensworks
 */
public class ContainerScreenPartInterfaceCraftingSettings extends ContainerScreenPartSettings<ContainerPartInterfaceCraftingSettings> implements IInputListener {

    private WidgetArrowedListField<IngredientComponent<?, ?>> ingredientComponentSideSelector = null;
    private WidgetTextFieldDropdown<Direction> dropdownFieldSide = null;
    private List<SideDropdownEntry> dropdownEntries;
    private IngredientComponent<?, ?> selectedIngredientComponent = null;
    private WidgetNumberField numberFieldChannelInterfaceCrafting = null;
    private ButtonCheckbox checkboxFieldDisabledCraftingCheck = null;

    public ContainerScreenPartInterfaceCraftingSettings(ContainerPartInterfaceCraftingSettings container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected ResourceLocation constructGuiTexture() {
        return new ResourceLocation(Reference.MOD_ID, "textures/gui/part_interface_settings.png");
    }

    @Override
    protected boolean isFieldSideEnabled() {
        return false;
    }

    @Override
    protected int getFieldSideY() {
        return 34;
    }

    @Override
    protected int getFieldUpdateIntervalY() {
        return 59;
    }

    @Override
    protected int getFieldPriorityY() {
        return 84;
    }

    @Override
    protected int getFieldChannelY() {
        return 109;
    }

    @Override
    protected void onSave() {
        super.onSave();
        try {
            Direction selectedSide = dropdownFieldSide.getSelectedDropdownPossibility() == null ? null : dropdownFieldSide.getSelectedDropdownPossibility().getValue();
            int side = selectedSide != null && selectedSide != getDefaultSide() ? selectedSide.ordinal() : -1;
            ValueNotifierHelpers.setValue(getMenu(), getMenu().getTargetSideOverrideValueId(selectedIngredientComponent), side);

            int channelInterface = numberFieldChannelInterfaceCrafting.getInt();
            ValueNotifierHelpers.setValue(getMenu(), getMenu().getLastChannelInterfaceCraftingValueId(), channelInterface);
            getMenu().setLastDisableCraftingCheckValue(checkboxFieldDisabledCraftingCheck.isChecked());
        } catch (NumberFormatException e) {
        }
    }

    @Override
    public void init() {
        super.init();

        ingredientComponentSideSelector = new WidgetArrowedListField<IngredientComponent<?, ?>>(font,
                leftPos + 106, topPos + 9, 68, 15, true,
                Component.translatable("gui.integratedcrafting.partsettings.ingredient"),
                true, Lists.newArrayList(IngredientComponent.REGISTRY.getValues())) {
            @Override
            protected String activeElementToString(IngredientComponent<?, ?> element) {
                return L10NHelpers.localize(element.getTranslationKey());
            }
        };
        ingredientComponentSideSelector.setListener(this);
        selectedIngredientComponent = ingredientComponentSideSelector.getActiveElement();

        dropdownEntries = Arrays.stream(Direction.values()).map(SideDropdownEntry::new).collect(Collectors.toList());
        dropdownFieldSide = new WidgetTextFieldDropdown(font, leftPos + 106, topPos + 34,
                68, 14, Component.translatable("gui.integrateddynamics.partsettings.side"),
                true, Sets.newHashSet(dropdownEntries));
        setSideInDropdownField(selectedIngredientComponent, ((ContainerPartInterfaceCraftingSettings) container).getTargetSideOverrideValue(selectedIngredientComponent));
        dropdownFieldSide.setMaxLength(15);
        dropdownFieldSide.setVisible(true);
        dropdownFieldSide.setTextColor(16777215);
        dropdownFieldSide.setCanLoseFocus(true);

        numberFieldChannelInterfaceCrafting = new WidgetNumberField(font, leftPos + 106, topPos + 134, 70, 14,
                true, Component.translatable("gui.integratedcrafting.partsettings.channel.interface"), true);
        numberFieldChannelInterfaceCrafting.setPositiveOnly(false);
        numberFieldChannelInterfaceCrafting.setMaxLength(15);
        numberFieldChannelInterfaceCrafting.setVisible(true);
        numberFieldChannelInterfaceCrafting.setTextColor(16777215);
        numberFieldChannelInterfaceCrafting.setCanLoseFocus(true);

        checkboxFieldDisabledCraftingCheck = new ButtonCheckbox(leftPos + 110, topPos + 159, 110, 10,
                Component.translatable("gui.integratedcrafting.partsettings.craftingcheckdisabled"), (entry) ->  {});

        this.refreshValues();
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        if (!this.numberFieldChannelInterfaceCrafting.charTyped(typedChar, keyCode)
                && !this.dropdownFieldSide.charTyped(typedChar, keyCode)) {
            return super.charTyped(typedChar, keyCode);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int typedChar, int keyCode, int modifiers) {
        if (typedChar != GLFW.GLFW_KEY_ESCAPE) {
            if (this.numberFieldChannelInterfaceCrafting.keyPressed(typedChar, keyCode, modifiers)) {
                return true;
            }
            if (this.dropdownFieldSide.keyPressed(typedChar, keyCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(typedChar, keyCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.ingredientComponentSideSelector.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (this.dropdownFieldSide.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (this.numberFieldChannelInterfaceCrafting.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (this.checkboxFieldDisabledCraftingCheck.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        font.draw(matrixStack, L10NHelpers.localize("gui.integrateddynamics.partsettings.side"), leftPos + 8, topPos + 12, Helpers.RGBToInt(0, 0, 0));
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ingredientComponentSideSelector.render(matrixStack, mouseX, mouseY, partialTicks);
        dropdownFieldSide.render(matrixStack, mouseX, mouseY, partialTicks);

        font.draw(matrixStack, L10NHelpers.localize("gui.integratedcrafting.partsettings.channel.interface"),
                leftPos + 8, topPos + 137, 0);
        numberFieldChannelInterfaceCrafting.render(matrixStack, mouseX, mouseY, partialTicks);

        font.draw(matrixStack, L10NHelpers.localize("gui.integratedcrafting.partsettings.craftingcheckdisabled"),
                leftPos + 8, topPos + 162, 0);
        checkboxFieldDisabledCraftingCheck.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected int getBaseYSize() {
        return 256;
    }

    protected void setSideInDropdownField(IngredientComponent<?, ?> ingredientComponent, Direction side) {
        if (selectedIngredientComponent == ingredientComponent) {
            dropdownFieldSide.selectPossibility(dropdownEntries.get(side.ordinal()));
        }
    }

    @Override
    public void onUpdate(int valueId, CompoundTag value) {
        super.onUpdate(valueId, value);
        for (IngredientComponent<?, ?> ingredientComponent : IngredientComponent.REGISTRY.getValues()) {
            if (valueId == getMenu().getTargetSideOverrideValueId(ingredientComponent)) {
                int side = getMenu().getTargetSideOverrideValue(ingredientComponent).ordinal();
                setSideInDropdownField(ingredientComponent, side == -1 ? getDefaultSide() : Direction.values()[side]);
            }
        }
        if (valueId == getMenu().getLastChannelInterfaceCraftingValueId()) {
            numberFieldChannelInterfaceCrafting.setValue(Integer.toString(getMenu().getLastChannelInterfaceValue()));
        }
        if (valueId == getMenu().getLastDisableCraftingCheckValueId()) {
            checkboxFieldDisabledCraftingCheck.setChecked(getMenu().getLastDisableCraftingCheckValue());
        }
    }

    @Override
    public void onChanged() {
        this.onSave();
        selectedIngredientComponent = ingredientComponentSideSelector.getActiveElement();
        setSideInDropdownField(selectedIngredientComponent, getMenu().getTargetSideOverrideValue(selectedIngredientComponent));
    }
}
