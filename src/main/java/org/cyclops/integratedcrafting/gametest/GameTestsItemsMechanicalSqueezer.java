package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingAttuned;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.write.IPartStateWriter;
import org.cyclops.integrateddynamics.core.block.IgnoredBlockStatus;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.*;

public class GameTestsItemsMechanicalSqueezer {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsMechanicalSqueezerBricks(GameTestHelper helper) {
        INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, RegistryEntries.BLOCK_MECHANICAL_SQUEEZER.value());

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east(), ChestBlockEntity.class);
        chestIn.setItem(0, new ItemStack(Items.BRICKS, 1));

        // Add brick recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RegistryEntries.RECIPETYPE_MECHANICAL_SQUEEZER.get(), Identifier.fromNamespaceAndPath("integrateddynamics", "mechanical_squeezer/convenience/minecraft_brick")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.BRICK, 4));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), Component.literal("Recipe in crafting interface is not valid"));

            // Check crafting writer state
            IPartStateWriter partStateWriter = (IPartStateWriter) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)).getState();
            helper.assertFalse(partStateWriter.isDeactivated(), Component.literal("Crafting writer is deactivated"));
            helper.assertValueEqual(
                    PartTypes.CRAFTING_WRITER.getBlockState(PartHelpers.getPartContainerChecked(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)), Direction.NORTH).getValue(IgnoredBlockStatus.STATUS),
                    IgnoredBlockStatus.Status.ACTIVE,
                    Component.literal("Block status is incorrect")
            );
            helper.assertValueEqual(partStateWriter.getActiveAspect(), CraftingAspects.Write.ITEMSTACK_CRAFT, Component.literal("Active aspect is incorrect"));
            helper.assertTrue(partStateWriter.getErrors(CraftingAspects.Write.ITEMSTACK_CRAFT).isEmpty(), Component.literal("Active aspect has errors"));

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.BRICK, Component.literal("Slot 0 item is incorrect"));
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 4, Component.literal("Slot 0 amount is incorrect"));
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsMechanicalSqueezerAttunedBricks(GameTestHelper helper) {
        INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions = createBasicNetwork(helper, POS, true, RegistryEntries.BLOCK_MECHANICAL_SQUEEZER.value());

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east(), ChestBlockEntity.class);
        chestIn.setItem(0, new ItemStack(Items.BRICKS, 1));

        // Enable crafting aspect in crafting writer (attuned interface auto-discovers recipes)
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.BRICK, 4));

        helper.succeedWhen(() -> {
            // Check crafting writer state
            IPartStateWriter partStateWriter = (IPartStateWriter) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)).getState();
            helper.assertFalse(partStateWriter.isDeactivated(), Component.literal("Crafting writer is deactivated"));
            helper.assertValueEqual(
                    PartTypes.CRAFTING_WRITER.getBlockState(PartHelpers.getPartContainerChecked(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)), Direction.NORTH).getValue(IgnoredBlockStatus.STATUS),
                    IgnoredBlockStatus.Status.ACTIVE,
                    Component.literal("Block status is incorrect")
            );
            helper.assertValueEqual(partStateWriter.getActiveAspect(), CraftingAspects.Write.ITEMSTACK_CRAFT, Component.literal("Active aspect is incorrect"));
            helper.assertTrue(partStateWriter.getErrors(CraftingAspects.Write.ITEMSTACK_CRAFT).isEmpty(), Component.literal("Active aspect has errors"));

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.BRICK, Component.literal("Slot 0 item is incorrect"));
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 4, Component.literal("Slot 0 amount is incorrect"));
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsMechanicalSqueezerFluidAndItemOutputJobCompletion(GameTestHelper helper) {
        INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, RegistryEntries.BLOCK_MECHANICAL_SQUEEZER.value());

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east(), ChestBlockEntity.class);
        chestIn.setItem(0, new ItemStack(Items.MUD, 1));

        // Add recipe with both item and fluid output to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RegistryEntries.RECIPETYPE_MECHANICAL_SQUEEZER.get(), Identifier.fromNamespaceAndPath("integrateddynamics", "mechanical_squeezer/convenience/minecraft_water_mud")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.DIRT, 1));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), Component.literal("Recipe in crafting interface is not valid"));

            // Check crafting writer state
            IPartStateWriter partStateWriter = (IPartStateWriter) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)).getState();
            helper.assertFalse(partStateWriter.isDeactivated(), Component.literal("Crafting writer is deactivated"));
            helper.assertValueEqual(
                    PartTypes.CRAFTING_WRITER.getBlockState(PartHelpers.getPartContainerChecked(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)), Direction.NORTH).getValue(IgnoredBlockStatus.STATUS),
                    IgnoredBlockStatus.Status.ACTIVE,
                    Component.literal("Block status is incorrect")
            );
            helper.assertValueEqual(partStateWriter.getActiveAspect(), CraftingAspects.Write.ITEMSTACK_CRAFT, Component.literal("Active aspect is incorrect"));
            helper.assertTrue(partStateWriter.getErrors(CraftingAspects.Write.ITEMSTACK_CRAFT).isEmpty(), Component.literal("Active aspect has errors"));

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.DIRT, Component.literal("Slot 0 item is incorrect"));
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, Component.literal("Slot 0 amount is incorrect"));

            // Check if job queue got cleared
            helper.assertTrue(positions.interfaceStates().get(0).getCraftingJobsCount() == 0, Component.literal("Crafting job queue is not empty"));
        });
    }

}
