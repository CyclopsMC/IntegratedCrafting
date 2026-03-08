package org.cyclops.integratedcrafting.gametest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.write.IPartStateWriter;
import org.cyclops.integrateddynamics.api.part.write.IPartTypeWriter;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueHelpers;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.evaluate.variable.Variable;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.part.event.PartVariableDrivenVariableContentsUpdatedEvent;
import org.cyclops.integrateddynamics.core.test.TestHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;

/**
 * Game tests for all advancements in the mod.
 * @author rubensworks
 */
public class GameTestsAdvancements {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
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
                    .get(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "root"));
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
                    .get(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_setup/craft_crafting_interface"));
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
                    .get(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_setup/craft_crafting_interface_attuned"));
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
                    .get(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_trigger/craft_crafting_writer"));
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
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Deserialize the recipe value from the exact same SNBT that the advancement JSON expects.
        // This guarantees ValuePredicate.test() will find equal values via ValueHelpers.areValuesEqual().
        CompoundTag recipeTag;
        try {
            recipeTag =
                    TagParser.parseCompoundFully(
                            "{output:{\"minecraft:itemstack\":[{id:\"minecraft:oak_planks\",Count:4}]},"
                                    + "input:{\"minecraft:itemstack\":[{val:[{condition:5,prototype:{id:\"minecraft:oak_log\",Count:1}}],type:0b}]}}");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        IValue recipeValue =
                TestHelpers.deserialize(
                        recipeTag,
                        valueInput -> ValueHelpers.deserializeRaw(valueInput, ValueTypes.OBJECT_RECIPE));
        IVariable<?> variable = new Variable<>(recipeValue);

        // Fire the event directly, mirroring what PartTypeInterfaceCrafting.State.reloadRecipe() does
        // when a player inserts a recipe variable into the crafting interface
        NeoForge.EVENT_BUS.post(new PartVariableDrivenVariableContentsUpdatedEvent<>(
                null, null, null, PartTypes.INTERFACE_CRAFTING, null, player, variable, null));

        helper.succeedWhen(() -> {
            AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                    .get(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_setup/insert_recipe_planks"));
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
                    .get(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "autocrafting_trigger/craft_planks"));
            helper.assertTrue(advancement != null, "craft_planks advancement not found");
            helper.assertTrue(
                    player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "craft_planks advancement not granted"
            );
        });
    }

    private static void assertAdvancementNotDone(GameTestHelper helper, ServerPlayer player, String id) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(Reference.MOD_ID, id));
        if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            throw new GameTestAssertException(Component.literal("Advancement should NOT have been obtained: " + Reference.MOD_ID + ":" + id), (int) helper.getTick());
        }
    }

    /**
     * Negative test for the root advancement.
     * Trigger: minecraft:inventory_changed
     * Condition: player has integrateddynamics:variable in inventory
     * Here we add a stick instead – advancement must NOT be granted.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementRootNegative(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Add a stick (not a variable) to the player's inventory
        player.getInventory().setItem(0, new ItemStack(Items.STICK));

        helper.succeedWhen(() -> assertAdvancementNotDone(helper, player, "root"));
    }

    /**
     * Negative test for the craft_crafting_interface advancement.
     * Trigger: cyclopscore:item_crafted
     * Condition: player crafts integratedcrafting:part_interface_crafting
     * Here we craft a stick instead – advancement must NOT be granted.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftCraftingInterfaceNegative(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(Items.STICK),
                new SimpleContainer(9)
        ));

        helper.succeedWhen(() -> assertAdvancementNotDone(helper, player, "autocrafting_setup/craft_crafting_interface"));
    }

    /**
     * Negative test for the craft_crafting_interface_attuned advancement.
     * Trigger: cyclopscore:item_crafted
     * Condition: player crafts integratedcrafting:part_interface_crafting_attuned
     * Here we craft the non-attuned interface instead – advancement must NOT be granted.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftCraftingInterfaceAttunedNegative(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(PartTypes.INTERFACE_CRAFTING.getItem()),
                new SimpleContainer(9)
        ));

        helper.succeedWhen(() -> assertAdvancementNotDone(helper, player, "autocrafting_setup/craft_crafting_interface_attuned"));
    }

    /**
     * Negative test for the craft_crafting_writer advancement.
     * Trigger: cyclopscore:item_crafted
     * Condition: player crafts integratedcrafting:part_crafting_writer
     * Here we craft a stick instead – advancement must NOT be granted.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftCraftingWriterNegative(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(Items.STICK),
                new SimpleContainer(9)
        ));

        helper.succeedWhen(() -> assertAdvancementNotDone(helper, player, "autocrafting_trigger/craft_crafting_writer"));
    }

    /**
     * Negative test for the insert_recipe_planks advancement.
     * Trigger: integrateddynamics:part_variable_driven
     * Condition: crafting interface has an oak planks recipe variable
     * Here we fire the event with a wrong part type (crafting_writer) – advancement must NOT be granted.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementInsertRecipePlanksNegative(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        CompoundTag recipeTag;
        try {
            recipeTag =
                    TagParser.parseCompoundFully(
                            "{output:{\"minecraft:itemstack\":[{id:\"minecraft:oak_planks\",Count:4}]},"
                                    + "input:{\"minecraft:itemstack\":[{val:[{condition:5,prototype:{id:\"minecraft:oak_log\",Count:1}}],type:0b}]}}");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        IValue recipeValue =
                TestHelpers.deserialize(
                        recipeTag,
                        valueInput -> ValueHelpers.deserializeRaw(valueInput, ValueTypes.OBJECT_RECIPE));
        IVariable<?> variable = new Variable<>(recipeValue);

        // Fire with wrong part type (crafting_writer instead of interface_crafting)
        NeoForge.EVENT_BUS.post(new PartVariableDrivenVariableContentsUpdatedEvent<>(
                null, null, null, PartTypes.CRAFTING_WRITER, null, player, variable, null));

        helper.succeedWhen(() -> assertAdvancementNotDone(helper, player, "autocrafting_setup/insert_recipe_planks"));
    }

    /**
     * Negative test for the craft_planks advancement.
     * Trigger: integrateddynamics:part_writer_aspect
     * Condition: crafting writer writes an oak_planks itemstack as the ITEMSTACK_CRAFT aspect
     * Here we place a stick variable in the writer instead – advancement must NOT be granted.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementCraftPlanksNegative(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS);

        // Place stick variable in the crafting writer instead of oak planks
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.STICK));

        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        PartHelpers.PartStateHolder writerStateHolder = PartHelpers.getPart(positions.writer());
        IPartTypeWriter<?, ?> partTypeWriter = (IPartTypeWriter<?, ?>) writerStateHolder.getPart();
        IPartStateWriter<?> writerState = (IPartStateWriter<?>) writerStateHolder.getState();
        callUpdateActivationWithPlayer(partTypeWriter, writerState, positions.writer(), player);

        helper.succeedWhen(() -> assertAdvancementNotDone(helper, player, "autocrafting_trigger/craft_planks"));
    }

    @SuppressWarnings("unchecked")
    private static <P extends IPartTypeWriter<P, S>, S extends IPartStateWriter<P>> void callUpdateActivationWithPlayer(
            IPartTypeWriter<?, ?> partType, IPartStateWriter<?> partState, PartPos writerPos, ServerPlayer player) {
        ((P) partType).updateActivation(PartTarget.fromCenter(writerPos), (S) partState, player, false);
    }

}
