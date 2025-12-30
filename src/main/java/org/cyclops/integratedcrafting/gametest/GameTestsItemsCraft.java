package org.cyclops.integratedcrafting.gametest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
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
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesItemStackTag;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesList;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingAttuned;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspectWriteBuilders;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.write.IPartStateWriter;
import org.cyclops.integrateddynamics.core.block.IgnoredBlockStatus;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integratedtunnels.part.aspect.TunnelAspects;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.*;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.createVariableForValue;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.placeVariableInWriter;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemsCraft {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftChestOne(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

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
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

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
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.FURNACE);

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
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.FURNACE);

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
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

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
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE);

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
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE);

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
    public void testItemsCraftDropper(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.CRAFTING_TABLE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(1, new ItemStack(Items.REDSTONE_BLOCK, 2));
        chestIn.setItem(2, new ItemStack(Items.COBBLESTONE, 7));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "redstone")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(1, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "dropper")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.DROPPER));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe 0 in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(1), "Recipe 1 in crafting interface 0 is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.DROPPER, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftCrafterComplex(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE, Blocks.FURNACE);

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
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.REDSTONE, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 15, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CRAFTER, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftPlanksAndExtractFromStorage(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_LOG, 64));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.OAK_PLANKS));

        // Set aspect to ignore storage contents
        setWriterAspectProperty(positions.writer(), CraftingAspects.Write.ITEMSTACK_CRAFT, CraftingAspectWriteBuilders.PROP_IGNORE_STORAGE, ValueTypeBoolean.ValueBoolean.of(true));

        // Extract all items from storage chest
        helper.setBlock(positions.chest().south(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(positions.chest().south().south(), Blocks.CHEST);
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(positions.chest().south()), Direction.SOUTH, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM.getItem()));
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(positions.chest().south()), Direction.NORTH, org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM.getItem()));
        placeVariableInWriter(helper.getLevel(), PartPos.of(helper.getLevel(), helper.absolutePos(positions.chest().south()), Direction.NORTH), TunnelAspects.Write.Item.ITEMSTACK_IMPORT, createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_ITEMSTACK, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.OAK_PLANKS))));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check if items have been crafted
            helper.assertTrue(chestIn.getItem(0).isEmpty(), "Slot 0 item is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftDeadBushTag(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.SHEARS, 1));
        chestIn.setItem(1, new ItemStack(Items.SPRUCE_SAPLING, 10));

        // Add dead bush to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("integratedcrafting", "special/minecraft_dead_bush")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.DEAD_BUSH));

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
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.DEAD_BUSH, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 10, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.SHEARS, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftDeadBushTagReusable(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.SHEARS, 1));
        chestIn.setItem(1, new ItemStack(Items.SPRUCE_SAPLING, 10));

        // Add dead bush recipe with reusable shears to crafting interface
        createDeadBushTagReusableRecipe(helper, positions);

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.DEAD_BUSH, 10));

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
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.DEAD_BUSH, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 10, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.SHEARS, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
        });
    }

    protected static void createDeadBushTagReusableRecipe(GameTestHelper helper, GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions) {
        // Add dead bush recipe with reusable shears to crafting interface
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> recipeIn = Maps.newIdentityHashMap();
        List<IPrototypedIngredientAlternatives<?, ?>> alternatives = Lists.newArrayList();
        Map<IngredientComponent<?, ?>, List<?>> recipeOut = Maps.newIdentityHashMap();
        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(Items.SPRUCE_SAPLING), ItemMatch.ITEM | ItemMatch.DATA)
        )));
        alternatives.add(new PrototypedIngredientAlternativesItemStackTag(Lists.newArrayList("c:tools/shear"), ItemMatch.ITEM, 1));
        recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
        Map<IngredientComponent<?, ?>, List<Boolean>> inputsReusable = Maps.newIdentityHashMap();
        inputsReusable.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(false, true));
        recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(new ItemStack(Items.DEAD_BUSH)));
        ItemStack variableRecipe = createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_RECIPE, ValueObjectTypeRecipe.ValueRecipe.of(new RecipeDefinition(recipeIn, inputsReusable, new MixedIngredients(recipeOut))));
        positions.interfaceStates().get(0).getInventoryVariables().setItem(0, variableRecipe);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftDeadBushTagReusableAsDependency(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.CRAFTING_TABLE, Blocks.CRAFTING_TABLE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.SHEARS, 1));
        chestIn.setItem(1, new ItemStack(Items.SHEARS, 1));
        chestIn.setItem(2, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(3, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(4, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(5, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(6, new ItemStack(Items.SPRUCE_SAPLING, 64));

        // Add dead bush recipe with reusable shears to crafting interface
        createDeadBushTagReusableRecipe(helper, positions);

        // Add dead bush to gold recipe
        positions.interfaceRecipeAdders().get(1).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("integratedcrafting", "special/dead_bush_to_gold")));

        // Speed up crafting interfaces, to craft once every tick
        GameTestHelpersIntegratedCrafting.setCraftingInterfaceUpdateInterval(positions.interfaces().get(0), 1);
        GameTestHelpersIntegratedCrafting.setCraftingInterfaceUpdateInterval(positions.interfaces().get(1), 1);

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.GOLD_INGOT, 64 * 5));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");
            helper.assertTrue(positions.interfaceStates().get(1).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

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
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.GOLD_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 64, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.GOLD_INGOT, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 64, "Slot 1 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getItem(), Items.GOLD_INGOT, "Slot 2 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getCount(), 64, "Slot 2 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getItem(), Items.GOLD_INGOT, "Slot 3 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getCount(), 64, "Slot 3 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(4).getItem(), Items.GOLD_INGOT, "Slot 4 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(4).getCount(), 64, "Slot 4 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(5).getItem(), Items.SHEARS, "Slot 5 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(5).getCount(), 1, "Slot 5 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftDeadBushTagReusableAsDependencyCraft(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.CRAFTING_TABLE, Blocks.CRAFTING_TABLE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.SHEARS, 1));
        chestIn.setItem(1, new ItemStack(Items.IRON_INGOT, 2));
        chestIn.setItem(2, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(3, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(4, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(5, new ItemStack(Items.SPRUCE_SAPLING, 64));
        chestIn.setItem(6, new ItemStack(Items.SPRUCE_SAPLING, 64));

        // Add dead bush recipe with reusable shears to crafting interface
        createDeadBushTagReusableRecipe(helper, positions);

        // Add dead bush to gold recipe
        positions.interfaceRecipeAdders().get(1).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("integratedcrafting", "special/dead_bush_to_gold")));
        positions.interfaceRecipeAdders().get(1).accept(Triple.of(1, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "shears")));

        // Speed up crafting interfaces, to craft once every tick
        GameTestHelpersIntegratedCrafting.setCraftingInterfaceUpdateInterval(positions.interfaces().get(0), 1);
        GameTestHelpersIntegratedCrafting.setCraftingInterfaceUpdateInterval(positions.interfaces().get(1), 1);

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.GOLD_INGOT, 64 * 5));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");
            helper.assertTrue(positions.interfaceStates().get(1).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

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
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.GOLD_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 64, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.GOLD_INGOT, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 64, "Slot 1 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getItem(), Items.GOLD_INGOT, "Slot 2 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getCount(), 64, "Slot 2 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getItem(), Items.GOLD_INGOT, "Slot 3 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getCount(), 64, "Slot 3 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(4).getItem(), Items.GOLD_INGOT, "Slot 4 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(4).getCount(), 64, "Slot 4 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(5).getItem(), Items.SHEARS, "Slot 5 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(5).getCount(), 1, "Slot 5 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftCraftingTablesWithExistingPlank(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.CRAFTING_TABLE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_LOG, 2));
        chestIn.setItem(1, new ItemStack(Items.OAK_PLANKS, 1));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "oak_planks")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(1, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CRAFTING_TABLE, 2));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe 0 in crafting interface 0 is not valid");
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(1), "Recipe 1 in crafting interface 0 is not valid");

            // Check if items have been crafted
            // Try-catch block checks for two acceptable variants
            try {
                helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.OAK_PLANKS, "Slot 0 item is incorrect");
                helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
                helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CRAFTING_TABLE, "Slot 1 item is incorrect");
                helper.assertValueEqual(chestIn.getItem(1).getCount(), 2, "Slot 1 amount is incorrect");
            } catch (GameTestAssertException e) {
                helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.CRAFTING_TABLE, "Slot 0 item is incorrect");
                helper.assertValueEqual(chestIn.getItem(0).getCount(), 2, "Slot 0 amount is incorrect");
                helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.OAK_PLANKS, "Slot 1 item is incorrect");
                helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
            }
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftAttunedIronShovel(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions = createBasicNetwork(helper, POS, true, Blocks.CRAFTING_TABLE, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.IRON_ORE, 64));
        chestIn.setItem(1, new ItemStack(Items.OAK_LOG, 64));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_SHOVEL));

        helper.succeedWhen(() -> {
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
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_ORE, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 63, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.OAK_LOG, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 63, "Slot 1 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getItem(), Items.OAK_PLANKS, "Slot 2 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getCount(), 2, "Slot 2 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getItem(), Items.IRON_SHOVEL, "Slot 3 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getCount(), 1, "Slot 3 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(4).getItem(), Items.STICK, "Slot 4 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(4).getCount(), 2, "Slot 4 amount is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftAttunedIronShovelCancel(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions = createBasicNetwork(helper, POS, true, Blocks.CRAFTING_TABLE, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.IRON_ORE, 64));
        chestIn.setItem(1, new ItemStack(Items.OAK_LOG, 64));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_SHOVEL));

        helper.runAfterDelay(10, () -> {
            CraftingHelpers.getCraftingNetwork(NetworkHelpers.getNetworkChecked(positions.writer()))
                    .ifPresent(network -> {
                        PartHelpers.removePart(helper.getLevel(), positions.writer().getPos().getBlockPos(), positions.writer().getSide(), null, false, false, false);

                        Iterator<CraftingJob> it = network.getCraftingJobs(0);
                        while (it.hasNext()) {
                            network.cancelCraftingJob(0, it.next().getId());
                        }
                    });
        });

        helper.succeedWhen(() -> {
            // We don't check the writer state, as it has been removed

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_ORE, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 63, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.OAK_LOG, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 63, "Slot 1 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getItem(), Items.OAK_PLANKS, "Slot 2 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(2).getCount(), 4, "Slot 2 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getItem(), Items.IRON_INGOT, "Slot 3 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(3).getCount(), 1, "Slot 3 amount is incorrect");
        });
    }

}
