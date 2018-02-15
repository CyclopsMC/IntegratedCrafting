package org.cyclops.integratedcrafting.part;

import org.cyclops.integratedcrafting.core.part.PartTypeCraftingBase;
import org.cyclops.integrateddynamics.core.part.PartStateBase;

/**
 * Interface for item handlers.
 * @author rubensworks
 */
public class PartTypeInterfaceCrafting extends PartTypeCraftingBase<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {
    public PartTypeInterfaceCrafting(String name) {
        super(name);
    }

    @Override
    protected PartTypeInterfaceCrafting.State constructDefaultState() {
        return new PartTypeInterfaceCrafting.State();
    }

    // TODO: implement item, fluid and energy handler cap?
    public static class State extends PartStateBase<PartTypeInterfaceCrafting> {

    }
}
