package org.cyclops.integratedcrafting.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.write.IPartStateWriter;
import org.cyclops.integrateddynamics.api.part.write.IPartTypeWriter;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.*;

/**
 * Game tests for all advancements in the mod.
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsAdvancements {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 200;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * Test for the root advancement.
     * Trigger: minecraft:inventory_changed
     * Condition: player has integrateddynamics:variable in inventory
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementRoot(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Add a variable item to the player's inventory, which fires the InventoryChangeTrigger implicitly
        player.getInventory().setItem(0, new ItemStack(RegistryEntries.ITEM_VARIABLE.get()));

        helper.succeedWhen(() -> {
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "root"));
            helper.assertTrue(advancement != null, "Root advancement not found");
            helper.assertTrue(
                    player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "Root advancement not granted"
            );
        });
    }

    /**
     * Test for the craft_crafting_interface advancement.
     * Trigger: cyclopscore:item_crafted
     * Condition: player crafts integratedcrafting:part_interface_crafting
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftCraftingInterface(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Fire the PlayerEvent.ItemCraftedEvent via the NeoForge event bus,
        // which is the same mechanism used when a player actually crafts an item
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(PartTypes.INTERFACE_CRAFTING.getItem()),
                new SimpleContainer(9)
        ));

        helper.succeedWhen(() -> {
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_setup/craft_crafting_interface"));
            helper.assertTrue(advancement != null, "craft_crafting_interface advancement not found");
            helper.assertTrue(
                    player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "craft_crafting_interface advancement not granted"
            );
        });
    }

    /**
     * Test for the craft_crafting_interface_attuned advancement.
     * Trigger: cyclopscore:item_crafted
     * Condition: player crafts integratedcrafting:part_interface_crafting_attuned
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftCraftingInterfaceAttuned(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Fire the PlayerEvent.ItemCraftedEvent via the NeoForge event bus
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(PartTypes.INTERFACE_CRAFTING_ATTUNED.getItem()),
                new SimpleContainer(9)
        ));

        helper.succeedWhen(() -> {
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_setup/craft_crafting_interface_attuned"));
            helper.assertTrue(advancement != null, "craft_crafting_interface_attuned advancement not found");
            helper.assertTrue(
                    player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "craft_crafting_interface_attuned advancement not granted"
            );
        });
    }

    /**
     * Test for the craft_crafting_writer advancement.
     * Trigger: cyclopscore:item_crafted
     * Condition: player crafts integratedcrafting:part_crafting_writer
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftCraftingWriter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Fire the PlayerEvent.ItemCraftedEvent via the NeoForge event bus
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(PartTypes.CRAFTING_WRITER.getItem()),
                new SimpleContainer(9)
        ));

        helper.succeedWhen(() -> {
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_trigger/craft_crafting_writer"));
            helper.assertTrue(advancement != null, "craft_crafting_writer advancement not found");
            helper.assertTrue(
                    player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "craft_crafting_writer advancement not granted"
            );
        });
    }

    /**
     * Test for the insert_recipe_planks advancement.
     * Trigger: integrateddynamics:part_variable_driven
     * Condition: crafting interface has an oak planks recipe variable
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementInsertRecipePlanks(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Set the last player on the crafting interface state so that the PartVariableDrivenVariableContentsUpdatedEvent
        // is fired with a real ServerPlayer when the recipe slot is updated
        positions.interfaceStates().get(0).setLastPlayer(player);

        // Add the oak planks recipe variable to the crafting interface.
        // This triggers onDirty() -> reloadRecipes() -> reloadRecipe() which fires
        // PartVariableDrivenVariableContentsUpdatedEvent with the lastPlayer
        positions.interfaceRecipeAdders().get(0).accept(
                Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks"))
        );

        helper.succeedWhen(() -> {
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_setup/insert_recipe_planks"));
            helper.assertTrue(advancement != null, "insert_recipe_planks advancement not found");
            helper.assertTrue(
                    player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "insert_recipe_planks advancement not granted"
            );
        });
    }

    /**
     * Test for the craft_planks advancement.
     * Trigger: integrateddynamics:part_writer_aspect
     * Condition: crafting writer writes an oak_planks itemstack as the ITEMSTACK_CRAFT aspect
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftPlanks(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS);

        // Place oak planks variable in the crafting writer (sets the variable in the writer's inventory)
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.OAK_PLANKS));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Call updateActivation with a real ServerPlayer to fire PartWriterAspectEvent with the player.
        // This is the same mechanism used when a player actually places a variable via the GUI.
        PartHelpers.PartStateHolder writerStateHolder = PartHelpers.getPart(positions.writer());
        IPartTypeWriter<?, ?> partTypeWriter = (IPartTypeWriter<?, ?>) writerStateHolder.getPart();
        IPartStateWriter<?> writerState = (IPartStateWriter<?>) writerStateHolder.getState();
        callUpdateActivationWithPlayer(partTypeWriter, writerState, positions.writer(), player);

        helper.succeedWhen(() -> {
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_trigger/craft_planks"));
            helper.assertTrue(advancement != null, "craft_planks advancement not found");
            helper.assertTrue(
                    player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "craft_planks advancement not granted"
            );
        });
    }

    @SuppressWarnings("unchecked")
    private static <P extends IPartTypeWriter<P, S>, S extends IPartStateWriter<P>> void callUpdateActivationWithPlayer(
            IPartTypeWriter<?, ?> partType, IPartStateWriter<?> partState, PartPos writerPos, ServerPlayer player) {
        ((P) partType).updateActivation(PartTarget.fromCenter(writerPos), (S) partState, player);
    }

}
