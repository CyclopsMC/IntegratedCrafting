package org.cyclops.integratedcrafting.client.gui;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedOffsets;
import org.cyclops.integrateddynamics.core.client.gui.container.ContainerScreenPartOffset;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerPartOffset;
import org.lwjgl.glfw.GLFW;

/**
 * Offsets gui for the attuned crafting interface.
 *
 * Unlike the regular offsets gui, closing this one brings the player
 * back to the gui of the attuned crafting interface it was opened from.
 *
 * @author rubensworks
 */
public class ContainerScreenPartInterfaceCraftingAttunedOffsets extends ContainerScreenPartOffset<ContainerPartInterfaceCraftingAttunedOffsets> {

    public ContainerScreenPartInterfaceCraftingAttunedOffsets(ContainerPartInterfaceCraftingAttunedOffsets container,
                                                              Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
            // Don't close all guis, but go back to the gui of the part.
            exitToPartGui();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    /**
     * Save the current offsets and go back to the gui of the part.
     */
    protected void exitToPartGui() {
        createServerPressable(ContainerPartOffset.BUTTON_SAVE, button -> onSave()).onPress(null);
    }

}
