package org.cyclops.integratedcrafting.core;

import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverrideRegistry;
import org.cyclops.integratedcrafting.core.crafting.processoverride.CraftingProcessOverrideCraftingTable;

/**
 * @author rubensworks
 */
public class CraftingProcessOverrides {

    public static ICraftingProcessOverrideRegistry REGISTRY = IntegratedCrafting._instance.getRegistryManager()
            .getRegistry(ICraftingProcessOverrideRegistry.class);

    public static final CraftingProcessOverrideCraftingTable CRAFTING_TABLE = REGISTRY.register(new CraftingProcessOverrideCraftingTable());

    public static void load() {}

}
