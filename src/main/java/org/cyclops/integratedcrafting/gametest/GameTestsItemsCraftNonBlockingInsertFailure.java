package org.cyclops.integratedcrafting.gametest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesList;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.describeContainer;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.describeCraftingState;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.createVariableForValue;

/**
 * Tests for non-blocking crafting interfaces whose target accepts recipe inputs in simulation,
 * but not all of them for real, which happens once the target is nearly full.
 *
 * @author rubensworks
 */
public class GameTestsItemsCraftNonBlockingInsertFailure {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
    public static final int TIMEOUT = 400;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);
    public static final int MACHINE_BASE = 61;

    /**
     * The recipe needs two separate cobblestone instances, which each fit in the machine in simulation,
     * while only one of them fits for real once the machine is nearly full.
     * Operations that were already running must still finish the job.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testNonBlockingJobFinishesWhenTargetFillsUp(GameTestHelper helper) {
        int amount = 3;

        // Storage
        helper.setBlock(POS, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.EAST, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM.getItem()));
        helper.setBlock(POS.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(POS.east(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, amount * 2));

        // Crafting writer
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH, PartTypes.CRAFTING_WRITER, new ItemStack(PartTypes.CRAFTING_WRITER.getItem()));
        PartPos writer = PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH);

        // Machine that only has room for three more cobblestone
        helper.setBlock(POS.west(), Blocks.BARREL);
        BarrelBlockEntity machine = helper.getBlockEntity(POS.west(), BarrelBlockEntity.class);
        for (int slot = 0; slot < machine.getContainerSize() - 1; slot++) {
            machine.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        int machineSlot = machine.getContainerSize() - 1;
        machine.setItem(machineSlot, new ItemStack(Items.COBBLESTONE, MACHINE_BASE));

        // Non-blocking crafting interface on top of the machine
        BlockPos posInterface = POS.above().west();
        helper.setBlock(POS.above(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(posInterface, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN, PartTypes.INTERFACE_CRAFTING, new ItemStack(PartTypes.INTERFACE_CRAFTING.getItem()));
        PartTypeInterfaceCrafting.State interfaceState = (PartTypeInterfaceCrafting.State) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN)).getState();
        interfaceState.getCraftingJobHandler().setBlockingJobsMode(false);

        // Two separate cobblestone inputs make one stone
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> recipeIn = Maps.newIdentityHashMap();
        recipeIn.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(
                new PrototypedIngredientAlternativesList<>(Lists.newArrayList(new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(Items.COBBLESTONE), ItemMatch.ITEM))),
                new PrototypedIngredientAlternativesList<>(Lists.newArrayList(new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(Items.COBBLESTONE), ItemMatch.ITEM)))));
        Map<IngredientComponent<?, ?>, List<?>> recipeOut = Maps.newIdentityHashMap();
        recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(new ItemStack(Items.STONE)));
        interfaceState.getInventoryVariables().setItem(0, createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_RECIPE,
                ValueObjectTypeRecipe.ValueRecipe.of(new RecipeDefinition(recipeIn, new MixedIngredients(recipeOut)))));

        // The machine turns two of its added cobblestone into one stone, and pushes it into the interface
        helper.onEachTick(() -> {
            ItemStack contents = machine.getItem(machineSlot);
            if (contents.getCount() >= MACHINE_BASE + 2) {
                if (insertStone(helper, posInterface)) {
                    contents.shrink(2);
                    machine.setChanged();
                }
            }
        });

        enableRecipeInWriter(helper, writer, new ItemStack(Items.STONE, amount));

        helper.succeedWhen(() -> {
            helper.assertFalse(interfaceState.getCraftingJobs().hasNext(), "The crafting job should be finished: "
                    + describeCraftingState(helper, interfaceState, POS.west()) + " storage=" + describeContainer(chest));
            int stone = 0;
            int cobblestone = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (stack.getItem() == Items.STONE) {
                    stone += stack.getCount();
                } else if (stack.getItem() == Items.COBBLESTONE) {
                    cobblestone += stack.getCount();
                }
            }
            helper.assertValueEqual(stone, amount, "Crafted stone");
            helper.assertValueEqual(cobblestone, 0, "Remaining cobblestone");
            helper.assertValueEqual(machine.getItem(machineSlot).getCount(), MACHINE_BASE, "Cobblestone left in the machine");
        });
    }

    /**
     * The recipe needs two different unstackable inputs, which each fit in the last free slot of the machine in simulation,
     * while only one of them fits for real.
     * The operation that was already running must stay tracked,
     * and the refused input must go back to the crafting job instead of the storage network.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testNonBlockingJobKeepsRunningOperationsWhenInsertionFails(GameTestHelper helper) {
        int amount = 3;

        // Storage
        helper.setBlock(POS, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.EAST, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM.getItem()));
        helper.setBlock(POS.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(POS.east(), ChestBlockEntity.class);
        for (int i = 0; i < amount; i++) {
            chest.setItem(i * 2, new ItemStack(Items.WOODEN_SWORD));
            chest.setItem(i * 2 + 1, new ItemStack(Items.STONE_SWORD));
        }

        // Crafting writer
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH, PartTypes.CRAFTING_WRITER, new ItemStack(PartTypes.CRAFTING_WRITER.getItem()));
        PartPos writer = PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH);

        // Machine that only has three free slots
        helper.setBlock(POS.west(), Blocks.BARREL);
        BarrelBlockEntity machine = helper.getBlockEntity(POS.west(), BarrelBlockEntity.class);
        for (int slot = 0; slot < machine.getContainerSize() - 3; slot++) {
            machine.setItem(slot, new ItemStack(Items.DIRT, 64));
        }

        // Non-blocking crafting interface on top of the machine
        BlockPos posInterface = POS.above().west();
        helper.setBlock(POS.above(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(posInterface, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN, PartTypes.INTERFACE_CRAFTING, new ItemStack(PartTypes.INTERFACE_CRAFTING.getItem()));
        PartTypeInterfaceCrafting.State interfaceState = (PartTypeInterfaceCrafting.State) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN)).getState();
        interfaceState.getCraftingJobHandler().setBlockingJobsMode(false);

        // A wooden and a stone sword make one stone
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> recipeIn = Maps.newIdentityHashMap();
        recipeIn.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(
                new PrototypedIngredientAlternativesList<>(Lists.newArrayList(new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(Items.WOODEN_SWORD), ItemMatch.ITEM))),
                new PrototypedIngredientAlternativesList<>(Lists.newArrayList(new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(Items.STONE_SWORD), ItemMatch.ITEM)))));
        Map<IngredientComponent<?, ?>, List<?>> recipeOut = Maps.newIdentityHashMap();
        recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(new ItemStack(Items.STONE)));
        interfaceState.getInventoryVariables().setItem(0, createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_RECIPE,
                ValueObjectTypeRecipe.ValueRecipe.of(new RecipeDefinition(recipeIn, new MixedIngredients(recipeOut)))));

        // The machine turns a pair of swords into one stone, and pushes it into the interface
        helper.onEachTick(() -> {
            int woodenSlot = -1;
            int stoneSlot = -1;
            for (int slot = 0; slot < machine.getContainerSize(); slot++) {
                if (machine.getItem(slot).getItem() == Items.WOODEN_SWORD) {
                    woodenSlot = slot;
                } else if (machine.getItem(slot).getItem() == Items.STONE_SWORD) {
                    stoneSlot = slot;
                }
            }
            if (woodenSlot >= 0 && stoneSlot >= 0) {
                if (insertStone(helper, posInterface)) {
                    machine.setItem(woodenSlot, ItemStack.EMPTY);
                    machine.setItem(stoneSlot, ItemStack.EMPTY);
                }
            }
        });

        enableRecipeInWriter(helper, writer, new ItemStack(Items.STONE, amount));

        // The refused wooden sword stays in the machine, so the last operation can not be started anymore
        helper.succeedWhen(() -> {
            int stone = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                helper.assertTrue(stack.isEmpty() || stack.getItem() == Items.STONE, "Unexpected item in storage: " + stack);
                stone += stack.getCount();
            }
            helper.assertValueEqual(stone, amount - 1, "Crafted stone");
            Iterator<CraftingJob> craftingJobs = interfaceState.getCraftingJobs();
            helper.assertTrue(craftingJobs.hasNext(), "The crafting job should still exist");
            CraftingJob craftingJob = craftingJobs.next();
            helper.assertValueEqual(craftingJob.getAmount(), 1, "Remaining job amount: "
                    + describeCraftingState(helper, interfaceState, POS.west()));
            helper.assertValueEqual(craftingJob.getIngredientsStorageBuffer().getInstances(IngredientComponents.ITEMSTACK).stream()
                    .filter(i -> !((ItemStack) i).isEmpty()).map(i -> ((ItemStack) i).getItem()).toList(),
                    List.of(Items.STONE_SWORD), "Inputs in the job");
        });
    }

    private static boolean insertStone(GameTestHelper helper, BlockPos posInterface) {
        ResourceHandler<ItemResource> target = helper.getLevel().getCapability(Capabilities.Item.BLOCK,
                helper.absolutePos(posInterface), Direction.DOWN);
        try (var tx = Transaction.openRoot()) {
            int inserted = target.insert(0, ItemResource.of(new ItemStack(Items.STONE)), 1, tx);
            tx.commit();
            return inserted == 1;
        }
    }

}
