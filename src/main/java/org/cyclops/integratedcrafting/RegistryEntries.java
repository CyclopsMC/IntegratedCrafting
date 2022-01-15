package org.cyclops.integratedcrafting;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ObjectHolder;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.item.ItemVariable;

/**
 * Referenced registry entries.
 * @author rubensworks
 */
public class RegistryEntries {

    @ObjectHolder("integratedcrafting:part_interface_crafting")
    public static final Item ITEM_PART_INTERFACE_CRAFTING = null;
    @ObjectHolder("integrateddynamics:variable")
    public static final ItemVariable ITEM_VARIABLE = null;

    @ObjectHolder("integratedcrafting:part_interface_crafting")
    public static final MenuType<ContainerPartInterfaceCrafting> CONTAINER_INTERFACE_CRAFTING = null;
    @ObjectHolder("integratedcrafting:part_interface_crafting_settings")
    public static final MenuType<ContainerPartInterfaceCraftingSettings> CONTAINER_INTERFACE_CRAFTING_SETTINGS = null;

}
