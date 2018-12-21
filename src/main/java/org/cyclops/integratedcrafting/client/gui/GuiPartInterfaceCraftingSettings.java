package org.cyclops.integratedcrafting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.cyclops.cyclopscore.client.gui.component.input.GuiNumberField;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.client.gui.container.GuiPartSettings;

import java.io.IOException;

/**
 * @author rubensworks
 */
public class GuiPartInterfaceCraftingSettings extends GuiPartSettings {

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
    protected void onSave() {
        super.onSave();
        try {
            int channelInterface = numberFieldChannelInterfaceCrafting.getInt();
            ValueNotifierHelpers.setValue(getContainer(), ((ContainerPartInterfaceCraftingSettings) getContainer()).getLastChannelInterfaceCraftingValueId(), channelInterface);
        } catch (NumberFormatException e) {
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        numberFieldChannelInterfaceCrafting = new GuiNumberField(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 106, guiTop + 109, 70, 14, true, true);
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
            if (!this.numberFieldChannelInterfaceCrafting.textboxKeyTyped(typedChar, keyCode)) {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.numberFieldChannelInterfaceCrafting.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        numberFieldChannelInterfaceCrafting.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
        fontRenderer.drawString(L10NHelpers.localize("gui.integratedcrafting.partsettings.channel.interface"),
                guiLeft + 8, guiTop + 112, 0);
    }

    @Override
    protected int getBaseYSize() {
        return 216;
    }

    @Override
    public void onUpdate(int valueId, NBTTagCompound value) {
        super.onUpdate(valueId, value);
        if (valueId == ((ContainerPartInterfaceCraftingSettings) getContainer()).getLastChannelInterfaceCraftingValueId()) {
            numberFieldChannelInterfaceCrafting.setText(Integer.toString(((ContainerPartInterfaceCraftingSettings) getContainer()).getLastChannelInterfaceValue()));
        }
    }
}
