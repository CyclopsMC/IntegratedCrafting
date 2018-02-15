package org.cyclops.integratedcrafting.part;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.inventory.IGuiContainerProvider;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.integratedcrafting.client.gui.GuiPartInterfaceCrafting;
import org.cyclops.integratedcrafting.core.part.PartTypeCraftingBase;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integrateddynamics.core.client.gui.ExtendedGuiHandler;
import org.cyclops.integrateddynamics.core.part.PartStateBase;
import org.cyclops.integrateddynamics.core.part.PartTypeConfigurable;

/**
 * Interface for item handlers.
 * @author rubensworks
 */
public class PartTypeInterfaceCrafting extends PartTypeCraftingBase<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    private final IGuiContainerProvider settingsGuiProvider;

    public PartTypeInterfaceCrafting(String name) {
        super(name);
        getModGui().getGuiHandler().registerGUI((settingsGuiProvider = new PartTypeConfigurable.GuiProviderSettings(
                Helpers.getNewId(getModGui(), Helpers.IDType.GUI), getModGui())), ExtendedGuiHandler.PART);
    }

    public IGuiContainerProvider getSettingsGuiProvider() {
        return settingsGuiProvider;
    }

    @Override
    protected PartTypeInterfaceCrafting.State constructDefaultState() {
        return new PartTypeInterfaceCrafting.State();
    }

    @Override
    public Class<? super PartTypeInterfaceCrafting> getPartTypeClass() {
        return PartTypeInterfaceCrafting.class;
    }

    @Override
    public Class<? extends GuiScreen> getGui() {
        return GuiPartInterfaceCrafting.class;
    }

    @Override
    public Class<? extends Container> getContainer() {
        return ContainerPartInterfaceCrafting.class;
    }

    public static class State extends PartStateBase<PartTypeInterfaceCrafting> {

        private SimpleInventory inventory;

        public State() {
            this.inventory = new SimpleInventory(9, "variables", 1);
            this.inventory.addDirtyMarkListener(this);
        }

        /**
         * @return The inner inventory
         */
        public SimpleInventory getInventory() {
            return this.inventory;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            inventory.writeToNBT(tag);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            inventory.readFromNBT(tag);
        }

    }
}
