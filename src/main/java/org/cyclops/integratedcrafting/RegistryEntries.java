package org.cyclops.integratedcrafting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.item.ItemVariable;

/**
 * Referenced registry entries.
 * @author rubensworks
 */
public class RegistryEntries {

    public static final DeferredHolder<Item, Item> ITEM_PART_INTERFACE_CRAFTING = DeferredHolder.create(Registries.ITEM, new ResourceLocation("integratedcrafting:part_interface_crafting"));
    public static final DeferredHolder<Item, ItemVariable> ITEM_VARIABLE = DeferredHolder.create(Registries.ITEM, new ResourceLocation("integrateddynamics:variable"));

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerPartInterfaceCrafting>> CONTAINER_INTERFACE_CRAFTING = DeferredHolder.create(Registries.MENU, new ResourceLocation("integratedcrafting:part_interface_crafting"));
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerPartInterfaceCraftingSettings>> CONTAINER_INTERFACE_CRAFTING_SETTINGS = DeferredHolder.create(Registries.MENU, new ResourceLocation("integratedcrafting:part_interface_crafting_settings"));

}
