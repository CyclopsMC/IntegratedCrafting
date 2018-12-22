package org.cyclops.integratedcrafting.client.gui;

import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.client.gui.component.input.GuiArrowedListField;
import org.cyclops.cyclopscore.client.gui.component.input.GuiNumberField;
import org.cyclops.cyclopscore.client.gui.component.input.IInputListener;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.client.gui.GuiTextFieldDropdown;
import org.cyclops.integrateddynamics.core.client.gui.container.GuiPartSettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rubensworks
 */
public class GuiPartInterfaceCraftingSettings extends GuiPartSettings implements IInputListener {

    private GuiArrowedListField<IngredientComponent<?, ?>> ingredientComponentSideSelector = null;
    private GuiTextFieldDropdown<EnumFacing> dropdownFieldSide = null;
    private List<SideDropdownEntry> dropdownEntries;
    private IngredientComponent<?, ?> selectedIngredientComponent = null;
    private GuiNumberField numberFieldChannelInterfaceCrafting = null;

    public GuiPartInterfaceCraftingSettings(EntityPlayer player, PartTarget target, IPartContainer partContainer, IPartType partType) {
        super(new ContainerPartInterfaceCraftingSettings(player, target, partContainer, partType), player, target, partContainer, partType);
    }

    protected ResourceLocation constructResourceLocation() {
        return new ResourceLocation(Reference.MOD_ID, getGuiTexture());
    }

    @Override
    public String getGuiTexture() {
        return IntegratedCrafting._instance.getReferenceValue(ModBase.REFKEY_TEXTURE_PATH_GUI)
                + "part_interface_settings.png";
    }

    @Override
    protected boolean isFieldSideEnabled() {
        return false;
    }

    protected int getFieldUpdateIntervalY() {
        return 59;
    }

    protected int getFieldPriorityY() {
        return 84;
    }

    protected int getFieldChannelY() {
        return 109;
    }

    @Override
    protected void onSave() {
        super.onSave();
        try {
            EnumFacing selectedSide = dropdownFieldSide.getSelectedDropdownPossibility() == null ? null : dropdownFieldSide.getSelectedDropdownPossibility().getValue();
            int side = selectedSide != null && selectedSide != getDefaultSide() ? selectedSide.ordinal() : -1;
            ValueNotifierHelpers.setValue(getContainer(), ((ContainerPartInterfaceCraftingSettings) getContainer()).getTargetSideOverrideValueId(selectedIngredientComponent), side);

            int channelInterface = numberFieldChannelInterfaceCrafting.getInt();
            ValueNotifierHelpers.setValue(getContainer(), ((ContainerPartInterfaceCraftingSettings) getContainer()).getLastChannelInterfaceCraftingValueId(), channelInterface);
        } catch (NumberFormatException e) {
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        ingredientComponentSideSelector = new GuiArrowedListField<IngredientComponent<?, ?>>(0, Minecraft.getMinecraft().fontRenderer,
                guiLeft + 106, guiTop + 9, 68, 15, true, true, IngredientComponent.REGISTRY.getValues()) {
            @Override
            protected String activeElementToString(IngredientComponent<?, ?> element) {
                return L10NHelpers.localize(element.getUnlocalizedName());
            }
        };
        ingredientComponentSideSelector.setListener(this);
        selectedIngredientComponent = ingredientComponentSideSelector.getActiveElement();

        dropdownEntries = Arrays.stream(EnumFacing.VALUES).map(SideDropdownEntry::new).collect(Collectors.toList());
        dropdownFieldSide = new GuiTextFieldDropdown(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 106, guiTop + 34,
                68, 14, true, Sets.newHashSet(dropdownEntries));
        setSideInDropdownField(selectedIngredientComponent, ((ContainerPartInterfaceCraftingSettings) container).getTargetSideOverrideValue(selectedIngredientComponent));
        dropdownFieldSide.setMaxStringLength(15);
        dropdownFieldSide.setVisible(true);
        dropdownFieldSide.setTextColor(16777215);
        dropdownFieldSide.setCanLoseFocus(true);

        numberFieldChannelInterfaceCrafting = new GuiNumberField(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 106, guiTop + 134, 70, 14, true, true);
        numberFieldChannelInterfaceCrafting.setPositiveOnly(false);
        numberFieldChannelInterfaceCrafting.setMaxStringLength(15);
        numberFieldChannelInterfaceCrafting.setVisible(true);
        numberFieldChannelInterfaceCrafting.setTextColor(16777215);
        numberFieldChannelInterfaceCrafting.setCanLoseFocus(true);

        this.refreshValues();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!this.checkHotbarKeys(keyCode)) {
            if (!this.numberFieldChannelInterfaceCrafting.textboxKeyTyped(typedChar, keyCode)
                    && !this.dropdownFieldSide.textboxKeyTyped(typedChar, keyCode)) {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.ingredientComponentSideSelector.mouseClicked(mouseX, mouseY, mouseButton);
        this.dropdownFieldSide.mouseClicked(mouseX, mouseY, mouseButton);
        this.numberFieldChannelInterfaceCrafting.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);

        fontRenderer.drawString(L10NHelpers.localize("gui.integrateddynamics.partsettings.side"), guiLeft + 8, guiTop + 12, Helpers.RGBToInt(0, 0, 0));
        GlStateManager.color(1, 1, 1);
        ingredientComponentSideSelector.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
        dropdownFieldSide.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);

        fontRenderer.drawString(L10NHelpers.localize("gui.integratedcrafting.partsettings.channel.interface"),
                guiLeft + 8, guiTop + 137, 0);
        numberFieldChannelInterfaceCrafting.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
    }

    @Override
    protected int getBaseYSize() {
        return 236;
    }

    protected void setSideInDropdownField(IngredientComponent<?, ?> ingredientComponent, EnumFacing side) {
        if (selectedIngredientComponent == ingredientComponent) {
            dropdownFieldSide.selectPossibility(dropdownEntries.get(side.ordinal()));
        }
    }

    @Override
    public void onUpdate(int valueId, NBTTagCompound value) {
        super.onUpdate(valueId, value);
        for (IngredientComponent<?, ?> ingredientComponent : IngredientComponent.REGISTRY.getValuesCollection()) {
            if (valueId == ((ContainerPartInterfaceCraftingSettings) getContainer()).getTargetSideOverrideValueId(ingredientComponent)) {
                int side = ((ContainerPartInterfaceCraftingSettings) getContainer()).getTargetSideOverrideValue(ingredientComponent).ordinal();
                setSideInDropdownField(ingredientComponent, side == -1 ? getDefaultSide() : EnumFacing.VALUES[side]);
            }
        }
        if (valueId == ((ContainerPartInterfaceCraftingSettings) getContainer()).getLastChannelInterfaceCraftingValueId()) {
            numberFieldChannelInterfaceCrafting.setText(Integer.toString(((ContainerPartInterfaceCraftingSettings) getContainer()).getLastChannelInterfaceValue()));
        }
    }

    @Override
    public void onChanged() {
        this.onSave();
        selectedIngredientComponent = ingredientComponentSideSelector.getActiveElement();
        setSideInDropdownField(selectedIngredientComponent, ((ContainerPartInterfaceCraftingSettings) container).getTargetSideOverrideValue(selectedIngredientComponent));
    }
}
