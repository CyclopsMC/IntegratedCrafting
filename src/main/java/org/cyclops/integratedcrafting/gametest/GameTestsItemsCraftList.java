package org.cyclops.integratedcrafting.gametest;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingList;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeListProxyPositionedRecipes;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.part.aspect.Aspects;

import java.util.List;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createVariableForRecipeList;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.createVariableForValue;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.createVariableFromReader;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemsCraftList {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * A single list variable exposes multiple recipes, of which the first one is crafted.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftListChest(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingList.State> positions =
                createBasicNetwork(helper, POS, PartTypes.INTERFACE_CRAFTING_LIST, Blocks.CRAFTING_TABLE);

        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        positions.interfaceStates().get(0).getInventoryVariables().setItem(0,
                createVariableForRecipeList(helper.getLevel(), List.of(
                        Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")),
                        Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"))
                )));

        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST));

        helper.succeedWhen(() -> {
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe list in crafting interface is not valid");
            helper.assertValueEqual(positions.interfaceStates().get(0).getRecipes().size(), 2, "Recipe count is incorrect");

            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.OAK_PLANKS, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 56, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CHEST, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
        });
    }

    /**
     * A recipe at the end of the list is craftable as well.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftListCraftingTable(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingList.State> positions =
                createBasicNetwork(helper, POS, PartTypes.INTERFACE_CRAFTING_LIST, Blocks.CRAFTING_TABLE);

        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        positions.interfaceStates().get(0).getInventoryVariables().setItem(0,
                createVariableForRecipeList(helper.getLevel(), List.of(
                        Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")),
                        Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"))
                )));

        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CRAFTING_TABLE));

        helper.succeedWhen(() -> {
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe list in crafting interface is not valid");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CRAFTING_TABLE, "Slot 1 item is incorrect");
        });
    }

    /**
     * Replacing the list variable re-indexes the interface with the new recipes.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftListReplaceVariable(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingList.State> positions =
                createBasicNetwork(helper, POS, PartTypes.INTERFACE_CRAFTING_LIST, Blocks.CRAFTING_TABLE);
        PartTypeInterfaceCraftingList.State state = positions.interfaceStates().get(0);

        state.getInventoryVariables().setItem(0, createVariableForRecipeList(helper.getLevel(), List.of(
                Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")),
                Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"))
        )));

        boolean[] replaced = {false};
        helper.succeedWhen(() -> {
            if (!replaced[0]) {
                helper.assertValueEqual(state.getRecipes().size(), 2, "Initial recipe count is incorrect");
                state.getInventoryVariables().setItem(0, createVariableForRecipeList(helper.getLevel(), List.of(
                        Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table"))
                )));
                replaced[0] = true;
                throw new GameTestAssertException("Waiting for the replaced variable to be picked up");
            }
            helper.assertValueEqual(state.getRecipes().size(), 1, "Recipe count after replacement is incorrect");
        });
    }

    /**
     * Duplicate recipes inside a list are only exposed once,
     * as the crafting network drops a recipe as soon as one removal is requested for it.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftListDuplicates(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingList.State> positions =
                createBasicNetwork(helper, POS, PartTypes.INTERFACE_CRAFTING_LIST, Blocks.CRAFTING_TABLE);

        positions.interfaceStates().get(0).getInventoryVariables().setItem(0,
                createVariableForRecipeList(helper.getLevel(), List.of(
                        Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")),
                        Pair.of(RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest"))
                )));

        helper.succeedWhen(() -> helper.assertValueEqual(positions.interfaceStates().get(0).getRecipes().size(), 1,
                "Duplicate recipes were not removed"));
    }

    /**
     * A list that does not hold recipes is rejected.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftListWrongElementType(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingList.State> positions =
                createBasicNetwork(helper, POS, PartTypes.INTERFACE_CRAFTING_LIST, Blocks.CRAFTING_TABLE);

        positions.interfaceStates().get(0).getInventoryVariables().setItem(0, createVariableForValue(helper.getLevel(),
                ValueTypes.LIST, ValueTypeList.ValueList.ofList(ValueTypes.INTEGER,
                        Lists.newArrayList(ValueTypeInteger.ValueInteger.of(1)))));

        helper.succeedWhen(() -> {
            helper.assertFalse(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe list in crafting interface is valid");
            helper.assertTrue(positions.interfaceStates().get(0).getRecipes().isEmpty(), "Recipes were exposed");
        });
    }

    /**
     * A lazy list from a machine reader is read into the interface in full,
     * and re-read whenever the reader's variable is invalidated.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftListMachineReader(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingList.State> positions =
                createBasicNetwork(helper, POS, PartTypes.INTERFACE_CRAFTING_LIST, Blocks.CRAFTING_TABLE);
        PartTypeInterfaceCraftingList.State state = positions.interfaceStates().get(0);

        // Validating every recipe of a whole machine against the target is not what is under test here
        state.setDisableCraftingCheck(true);

        // Extend the network towards a second crafting table, and read its recipes
        helper.setBlock(POS.south(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(POS.south().west(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(POS.south().west().west(), Blocks.CRAFTING_TABLE);
        PartPos readerPos = PartPos.of(helper.getLevel(), helper.absolutePos(POS.south().west()), Direction.WEST);
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS.south().west()), Direction.WEST,
                org.cyclops.integrateddynamics.core.part.PartTypes.MACHINE_READER,
                new ItemStack(org.cyclops.integrateddynamics.core.part.PartTypes.MACHINE_READER.getItem()));

        state.getInventoryVariables().setItem(0, createVariableFromReader(helper.getLevel(), readerPos,
                Aspects.Read.Machine.LIST_GETRECIPES));

        boolean[] targetRemoved = {false};
        helper.succeedWhen(() -> {
            if (!targetRemoved[0]) {
                helper.assertTrue(state.isRecipeSlotValid(0),
                        "Recipe list from the machine reader is not valid: " + state.getRecipeSlotUnlocalizedMessage(0));
                // The interface exposes exactly the recipes that its reader's target holds
                int targetRecipes = new ValueTypeListProxyPositionedRecipes(
                        DimPos.of(helper.getLevel(), helper.absolutePos(POS.south().west().west())), Direction.UP).getLength();
                helper.assertTrue(targetRecipes > 0, "The reader's target exposes no recipes at all");
                helper.assertValueEqual(state.getRecipes().size(), targetRecipes,
                        "Recipe count from the machine reader is incorrect");

                // Remove the reader's target, so that only the list variable changes
                helper.setBlock(POS.south().west().west(), Blocks.AIR);
                targetRemoved[0] = true;
                throw new GameTestAssertException("Waiting for the invalidated variable to be picked up");
            }
            helper.assertTrue(state.getRecipes().isEmpty(), "Recipes were not cleared after the reader target was removed");
        });
    }
}
