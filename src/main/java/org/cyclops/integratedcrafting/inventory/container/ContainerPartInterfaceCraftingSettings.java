package org.cyclops.integratedcrafting.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerPartSettings;

/**
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingSettings extends ContainerPartSettings {

    private final int lastChannelInterfaceCraftingValueId;

    public ContainerPartInterfaceCraftingSettings(EntityPlayer player, PartTarget target, IPartContainer partContainer, IPartType partType) {
        super(player, target, partContainer, partType);
        lastChannelInterfaceCraftingValueId = getNextValueId();
    }

    @Override
    protected int getPlayerInventoryOffsetY() {
        return 134;
    }

    @Override
    protected void initializeValues() {
        super.initializeValues();
        ValueNotifierHelpers.setValue(this, lastChannelInterfaceCraftingValueId, ((PartTypeInterfaceCrafting.State) getPartState()).getChannelCrafting());
    }

    public int getLastChannelInterfaceCraftingValueId() {
        return lastChannelInterfaceCraftingValueId;
    }

    public int getLastChannelInterfaceValue() {
        return ValueNotifierHelpers.getValueInt(this, lastChannelInterfaceCraftingValueId);
    }

    @Override
    protected void updatePartSettings() {
        super.updatePartSettings();
        ((PartTypeInterfaceCrafting.State) getPartState()).setChannelCrafting(getLastChannelInterfaceValue());
    }
}
