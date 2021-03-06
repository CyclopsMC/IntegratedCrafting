package org.cyclops.integratedcrafting.client.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
import org.cyclops.integrateddynamics.core.helper.L10NValues;
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

    public ContainerScreenPartInterfaceCraftingSettings(ContainerPartInterfaceCraftingSettings container, PlayerInventory inventory, ITextComponent title) {
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
            ValueNotifierHelpers.setValue(getContainer(), getContainer().getTargetSideOverrideValueId(selectedIngredientComponent), side);

            int channelInterface = numberFieldChannelInterfaceCrafting.getInt();
            ValueNotifierHelpers.setValue(getContainer(), getContainer().getLastChannelInterfaceCraftingValueId(), channelInterface);
            getContainer().setLastDisableCraftingCheckValue(checkboxFieldDisabledCraftingCheck.isChecked());
        } catch (NumberFormatException e) {
        }
    }

    @Override
    public void init() {
        super.init();

        ingredientComponentSideSelector = new WidgetArrowedListField<IngredientComponent<?, ?>>(font,
                guiLeft + 106, guiTop + 9, 68, 15, true,
                new TranslationTextComponent("gui.integratedcrafting.partsettings.ingredient"),
                true, Lists.newArrayList(IngredientComponent.REGISTRY.getValues())) {
            @Override
            protected String activeElementToString(IngredientComponent<?, ?> element) {
                return L10NHelpers.localize(element.getTranslationKey());
            }
        };
        ingredientComponentSideSelector.setListener(this);
        selectedIngredientComponent = ingredientComponentSideSelector.getActiveElement();

        dropdownEntries = Arrays.stream(Direction.values()).map(SideDropdownEntry::new).collect(Collectors.toList());
        dropdownFieldSide = new WidgetTextFieldDropdown(font, guiLeft + 106, guiTop + 34,
                68, 14, new TranslationTextComponent("gui.integrateddynamics.partsettings.side"),
                true, Sets.newHashSet(dropdownEntries));
        setSideInDropdownField(selectedIngredientComponent, ((ContainerPartInterfaceCraftingSettings) container).getTargetSideOverrideValue(selectedIngredientComponent));
        dropdownFieldSide.setMaxStringLength(15);
        dropdownFieldSide.setVisible(true);
        dropdownFieldSide.setTextColor(16777215);
        dropdownFieldSide.setCanLoseFocus(true);

        numberFieldChannelInterfaceCrafting = new WidgetNumberField(font, guiLeft + 106, guiTop + 134, 70, 14,
                true, new TranslationTextComponent("gui.integratedcrafting.partsettings.channel.interface"), true);
        numberFieldChannelInterfaceCrafting.setPositiveOnly(false);
        numberFieldChannelInterfaceCrafting.setMaxStringLength(15);
        numberFieldChannelInterfaceCrafting.setVisible(true);
        numberFieldChannelInterfaceCrafting.setTextColor(16777215);
        numberFieldChannelInterfaceCrafting.setCanLoseFocus(true);

        checkboxFieldDisabledCraftingCheck = new ButtonCheckbox(guiLeft + 110, guiTop + 159, 110, 10,
                new TranslationTextComponent("gui.integratedcrafting.partsettings.craftingcheckdisabled"), (entry) ->  {});

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
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);

        font.drawString(matrixStack, L10NHelpers.localize("gui.integrateddynamics.partsettings.side"), guiLeft + 8, guiTop + 12, Helpers.RGBToInt(0, 0, 0));
        GlStateManager.color4f(1, 1, 1, 1);
        ingredientComponentSideSelector.render(matrixStack, mouseX, mouseY, partialTicks);
        dropdownFieldSide.render(matrixStack, mouseX, mouseY, partialTicks);

        font.drawString(matrixStack, L10NHelpers.localize("gui.integratedcrafting.partsettings.channel.interface"),
                guiLeft + 8, guiTop + 137, 0);
        numberFieldChannelInterfaceCrafting.render(matrixStack, mouseX, mouseY, partialTicks);

        font.drawString(matrixStack, L10NHelpers.localize("gui.integratedcrafting.partsettings.craftingcheckdisabled"),
                guiLeft + 8, guiTop + 162, 0);
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
    public void onUpdate(int valueId, CompoundNBT value) {
        super.onUpdate(valueId, value);
        for (IngredientComponent<?, ?> ingredientComponent : IngredientComponent.REGISTRY.getValues()) {
            if (valueId == getContainer().getTargetSideOverrideValueId(ingredientComponent)) {
                int side = getContainer().getTargetSideOverrideValue(ingredientComponent).ordinal();
                setSideInDropdownField(ingredientComponent, side == -1 ? getDefaultSide() : Direction.values()[side]);
            }
        }
        if (valueId == getContainer().getLastChannelInterfaceCraftingValueId()) {
            numberFieldChannelInterfaceCrafting.setText(Integer.toString(getContainer().getLastChannelInterfaceValue()));
        }
        if (valueId == getContainer().getLastDisableCraftingCheckValueId()) {
            checkboxFieldDisabledCraftingCheck.setChecked(getContainer().getLastDisableCraftingCheckValue());
        }
    }

    @Override
    public void onChanged() {
        this.onSave();
        selectedIngredientComponent = ingredientComponentSideSelector.getActiveElement();
        setSideInDropdownField(selectedIngredientComponent, getContainer().getTargetSideOverrideValue(selectedIngredientComponent));
    }
}
