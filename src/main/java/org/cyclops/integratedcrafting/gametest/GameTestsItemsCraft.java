package org.cyclops.integratedcrafting.gametest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesList;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspectWriteBuilders;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.write.IPartStateWriter;
import org.cyclops.integrateddynamics.core.block.IgnoredBlockStatus;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import java.util.List;
import java.util.Map;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.*;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.createVariableForValue;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemsCraft {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftChestOne(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check crafting writer state
            IPartStateWriter partStateWriter = (IPartStateWriter) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)).getState();
            helper.assertFalse(partStateWriter.isDeactivated(), "Importer is deactivated");
            helper.assertValueEqual(
                    PartTypes.CRAFTING_WRITER.getBlockState(PartHelpers.getPartContainerChecked(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)), Direction.NORTH).getValue(IgnoredBlockStatus.STATUS),
                    IgnoredBlockStatus.Status.ACTIVE,
                    "Block status is incorrect"
            );
            helper.assertValueEqual(partStateWriter.getActiveAspect(), CraftingAspects.Write.ITEMSTACK_CRAFT, "Active aspect is incorrect");
            helper.assertTrue(partStateWriter.getErrors(CraftingAspects.Write.ITEMSTACK_CRAFT).isEmpty(), "Active aspect has errors");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.OAK_PLANKS, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 56, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CHEST, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftChestAll(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST));

        // Set aspect to ignore storage contents
        setWriterAspectProperty(positions.writer(), CraftingAspects.Write.ITEMSTACK_CRAFT, CraftingAspectWriteBuilders.PROP_IGNORE_STORAGE, ValueTypeBoolean.ValueBoolean.of(true));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check crafting writer state
            IPartStateWriter partStateWriter = (IPartStateWriter) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)).getState();
            helper.assertFalse(partStateWriter.isDeactivated(), "Importer is deactivated");
            helper.assertValueEqual(
                    PartTypes.CRAFTING_WRITER.getBlockState(PartHelpers.getPartContainerChecked(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)), Direction.NORTH).getValue(IgnoredBlockStatus.STATUS),
                    IgnoredBlockStatus.Status.ACTIVE,
                    "Block status is incorrect"
            );
            helper.assertValueEqual(partStateWriter.getActiveAspect(), CraftingAspects.Write.ITEMSTACK_CRAFT, "Active aspect is incorrect");
            helper.assertTrue(partStateWriter.getErrors(CraftingAspects.Write.ITEMSTACK_CRAFT).isEmpty(), "Active aspect has errors");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.CHEST, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CHEST, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 7, "Slot 1 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftIronIngot(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.RAW_IRON, 1));

        // Add iron ingot recipe to furnace
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_INGOT));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 0, "Slot 1 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftIronIngotRecipeWithEmptySpaces(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.RAW_IRON, 1));

        // Add iron ingot recipe with spaces to furnace
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> recipeIn = Maps.newIdentityHashMap();
        List<IPrototypedIngredientAlternatives<?, ?>> alternatives = Lists.newArrayList();
        Map<IngredientComponent<?, ?>, List<?>> recipeOut = Maps.newIdentityHashMap();
        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
        )));
        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
        )));
        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
        )));
        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
        )));
        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(Items.RAW_IRON), ItemMatch.ITEM | ItemMatch.DATA)
        )));
        recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
        recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(new ItemStack(Items.IRON_INGOT)));
        ItemStack variableRecipe = createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_RECIPE, ValueObjectTypeRecipe.ValueRecipe.of(new RecipeDefinition(recipeIn, new MixedIngredients(recipeOut))));
        positions.interfaceStates().get(0).getInventoryVariables().setItem(0, variableRecipe);

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_INGOT));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 0, "Slot 1 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftChestFromLogs(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_LOG, 2));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(1, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe 0 in crafting interface is not valid");
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(1), "Recipe 1 in crafting interface is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.CHEST, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 1 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 0, "Slot 1 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftIronIngotsParallel(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.RAW_IRON, 5));

        // Add iron ingot recipe to furnaces
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(1).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(2).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(3).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(4).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_INGOT, 5));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(1).isRecipeSlotValid(0), "Recipe in crafting interface 1 is not valid");
            helper.assertTrue(positions.interfaceStates().get(2).isRecipeSlotValid(0), "Recipe in crafting interface 2 is not valid");
            helper.assertTrue(positions.interfaceStates().get(3).isRecipeSlotValid(0), "Recipe in crafting interface 3 is not valid");
            helper.assertTrue(positions.interfaceStates().get(4).isRecipeSlotValid(0), "Recipe in crafting interface 4 is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 5, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 0, "Slot 1 amount is incorrect");

            helper.assertBlockProperty(POS.west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().south().west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().south().south().west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().south().south().south().west(), AbstractFurnaceBlock.LIT, true);
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftIronIngotsParallelMultipleNonBlocking(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.RAW_IRON, 10));

        // Add iron ingot recipe to furnaces
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(1).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(2).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(3).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(4).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_INGOT, 10));

        // Disable blocking mode
        setCraftingInterfaceBlockingMode(positions.interfaces().get(0), false);
        setCraftingInterfaceBlockingMode(positions.interfaces().get(1), false);
        setCraftingInterfaceBlockingMode(positions.interfaces().get(2), false);
        setCraftingInterfaceBlockingMode(positions.interfaces().get(3), false);
        setCraftingInterfaceBlockingMode(positions.interfaces().get(4), false);

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(1).isRecipeSlotValid(0), "Recipe in crafting interface 1 is not valid");
            helper.assertTrue(positions.interfaceStates().get(2).isRecipeSlotValid(0), "Recipe in crafting interface 2 is not valid");
            helper.assertTrue(positions.interfaceStates().get(3).isRecipeSlotValid(0), "Recipe in crafting interface 3 is not valid");
            helper.assertTrue(positions.interfaceStates().get(4).isRecipeSlotValid(0), "Recipe in crafting interface 4 is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 10, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 0, "Slot 1 amount is incorrect");

            helper.assertBlockProperty(POS.west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().south().west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().south().south().west(), AbstractFurnaceBlock.LIT, true);
            helper.assertBlockProperty(POS.south().south().south().south().west(), AbstractFurnaceBlock.LIT, true);
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftCrafterComplex(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.NetworkPositions positions = createBasicNetwork(helper, POS, Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_LOG, 1));
        chestIn.setItem(1, new ItemStack(Items.RAW_IRON, 5));
        chestIn.setItem(2, new ItemStack(Items.REDSTONE_BLOCK, 2));
        chestIn.setItem(3, new ItemStack(Items.COBBLESTONE, 7));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(1, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(2, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "redstone")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(3, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "crafter")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(4, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "dropper")));
        positions.interfaceRecipeAdders().get(1).accept(Triple.of(0, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(2).accept(Triple.of(1, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(3).accept(Triple.of(2, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(4).accept(Triple.of(3, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        positions.interfaceRecipeAdders().get(5).accept(Triple.of(4, RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CRAFTER));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe 0 in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(1), "Recipe 1 in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(2), "Recipe 2 in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(3), "Recipe 3 in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(4), "Recipe 4 in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(1).isRecipeSlotValid(0), "Recipe 0 in crafting interface 1 is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.CRAFTER, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
        });
    }

}
