package org.cyclops.integratedcrafting.core;

import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverrideRegistry;
import org.cyclops.integratedcrafting.core.crafting.processoverride.CraftingProcessOverrideBrewingStand;
import org.cyclops.integratedcrafting.core.crafting.processoverride.CraftingProcessOverrideCraftingTable;
import org.cyclops.integratedcrafting.core.crafting.processoverride.CraftingProcessOverrideSmithingTable;
import org.cyclops.integratedcrafting.core.crafting.processoverride.CraftingProcessOverrideStonecutter;

/**
 * @author rubensworks
 */
public class CraftingProcessOverrides {

    public static ICraftingProcessOverrideRegistry REGISTRY = IntegratedCrafting._instance.getRegistryManager()
            .getRegistry(ICraftingProcessOverrideRegistry.class);

    public static final CraftingProcessOverrideSmithingTable SMITHING_TABLE = REGISTRY.register(new CraftingProcessOverrideSmithingTable()); // Must be before crafting table, as smithing table would also be applicable to CraftingProcessOverrideCraftingTable
    public static final CraftingProcessOverrideCraftingTable CRAFTING_TABLE = REGISTRY.register(new CraftingProcessOverrideCraftingTable());
    public static final CraftingProcessOverrideBrewingStand BREWING_STAND = REGISTRY.register(new CraftingProcessOverrideBrewingStand());
    public static final CraftingProcessOverrideStonecutter STONE_CUTTER = REGISTRY.register(new CraftingProcessOverrideStonecutter());

    public static void load() {}

}
