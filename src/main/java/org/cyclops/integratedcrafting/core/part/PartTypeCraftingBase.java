package org.cyclops.integratedcrafting.core.part;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.part.IPartState;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartRenderPosition;
import org.cyclops.integrateddynamics.core.client.gui.container.GuiPartSettings;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerPartSettings;
import org.cyclops.integrateddynamics.core.part.PartTypeBase;

/**
 * Base part for a crafting part.
 * @author rubensworks
 */
public abstract class PartTypeCraftingBase<P extends IPartType<P, S>, S extends IPartState<P>> extends PartTypeBase<P, S> {

    public PartTypeCraftingBase(String name) {
        super(name, new PartRenderPosition(0.1875F, 0.1875F, 0.625F, 0.625F));
    }

    @Override
    public ModBase getMod() {
        return IntegratedCrafting._instance;
    }

    @Override
    public ModBase getModGui() {
        return IntegratedDynamics._instance;
    }

    @Override
    public Class<? super P> getPartTypeClass() {
        return IPartType.class;
    }

    @Override
    protected boolean hasGui() {
        return true;
    }

    @Override
    public Class<? extends Container> getContainer() {
        return ContainerPartSettings.class;
    }

    @Override
    public Class<? extends GuiScreen> getGui() {
        return GuiPartSettings.class;
    }
}
