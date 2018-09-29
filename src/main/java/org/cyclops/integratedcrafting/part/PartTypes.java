package org.cyclops.integratedcrafting.part;

import com.google.common.collect.Lists;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.part.IPartTypeRegistry;
import org.cyclops.integrateddynamics.part.aspect.Aspects;

/**
 * @author rubensworks
 */
public class PartTypes {

    public static final IPartTypeRegistry REGISTRY = IntegratedDynamics._instance.getRegistryManager().getRegistry(IPartTypeRegistry.class);

    public static void load() {
        Aspects.REGISTRY.register(org.cyclops.integrateddynamics.core.part.PartTypes.NETWORK_READER, Lists.newArrayList(
                CraftingAspects.Read.Network.RECIPES,
                CraftingAspects.Read.Network.CRAFTING_JOBS,
                CraftingAspects.Read.Network.CRAFTING_INGREDIENTS
        ));
    }

    public static final PartTypeInterfaceCrafting INTERFACE_CRAFTING = REGISTRY.register(new PartTypeInterfaceCrafting("interface_crafting"));
    public static final PartTypeCraftingWriter CRAFTING_WRITER = REGISTRY.register(new PartTypeCraftingWriter("crafting_writer"));

}
