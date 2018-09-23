package org.cyclops.integratedcrafting.part;

import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.part.IPartTypeRegistry;

/**
 * @author rubensworks
 */
public class PartTypes {

    public static final IPartTypeRegistry REGISTRY = IntegratedDynamics._instance.getRegistryManager().getRegistry(IPartTypeRegistry.class);

    public static void load() {}

    public static final PartTypeInterfaceCrafting INTERFACE_CRAFTING = REGISTRY.register(new PartTypeInterfaceCrafting("interface_crafting"));
    public static final PartTypeCraftingWriter CRAFTING_WRITER = REGISTRY.register(new PartTypeCraftingWriter("crafting_writer"));

}
