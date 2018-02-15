package org.cyclops.integratedcrafting.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.cyclops.cyclopscore.client.gui.component.button.GuiButtonImage;
import org.cyclops.cyclopscore.client.gui.image.Images;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.client.gui.ExtendedGuiHandler;
import org.cyclops.integrateddynamics.core.client.gui.container.GuiMultipart;


/**
 * Gui for the crafting interface.
 * @author rubensworks
 */
public class GuiPartInterfaceCrafting extends GuiMultipart<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    public static final int BUTTON_SETTINGS = 1;

    /**
     * Make a new instance.
     * @param partTarget The target.
     * @param player The player.
     * @param partContainer The part container.
     * @param partType The targeted part type.
     */
    public GuiPartInterfaceCrafting(EntityPlayer player, PartTarget partTarget, IPartContainer partContainer, PartTypeInterfaceCrafting partType) {
        super(new ContainerPartInterfaceCrafting(player, partTarget, partContainer, partType));

        putButtonAction(BUTTON_SETTINGS, (buttonId, gui, container) -> {
            IntegratedDynamics._instance.getGuiHandler().setTemporaryData(ExtendedGuiHandler.PART, getTarget().getCenter().getSide()); // Pass the side as extra data to the gui
        });
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButtonImage(BUTTON_SETTINGS, this.guiLeft + 155, this.guiTop + 4, 15, 15, Images.CONFIG_BOARD, -2, -3, true));
    }

    @Override
    protected ResourceLocation constructResourceLocation() {
        return new ResourceLocation(Reference.MOD_ID, getGuiTexture());
    }

    @Override
    public String getGuiTexture() {
        return IntegratedCrafting._instance.getReferenceValue(ModBase.REFKEY_TEXTURE_PATH_GUI) + getNameId() + ".png";
    }

    @Override
    protected String getNameId() {
        return "part_interface_crafting";
    }

    @Override
    protected int getBaseXSize() {
        return 176;
    }

    @Override
    protected int getBaseYSize() {
        return 134;
    }
}
