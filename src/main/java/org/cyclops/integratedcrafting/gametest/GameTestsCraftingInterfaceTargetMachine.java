package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.*;

/**
 * Game tests for the machine that crafting interfaces are targeting.
 * @author rubensworks
 */
public class GameTestsCraftingInterfaceTargetMachine {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
    public static final int TIMEOUT = 2000;
    /**
     * Attuned interfaces expose every recipe of their machine, which is too heavy to run
     * next to the default batch, so these tests get a batch (test environment) of their own.
     */
    public static final String BATCH = Reference.MOD_ID + ":crafting_interface_target_machine";
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    protected void testTargetMachineItem(GameTestHelper helper, boolean attuned, Block crafter, Item expectedItem) {
        INetworkPositions<PartTypeInterfaceCraftingBase.State<?, ?>> positions = createBasicNetwork(helper, POS, attuned, crafter);

        helper.succeedWhen(() -> {
            ItemStack machineItem = positions.interfaceStates().get(0).getTargetMachineItem();
            helper.assertValueEqual(machineItem.getItem(), expectedItem, "Target machine item is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT, environment = BATCH)
    public void testTargetMachineItemCraftingTable(GameTestHelper helper) {
        testTargetMachineItem(helper, false, Blocks.CRAFTING_TABLE, Items.CRAFTING_TABLE);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT, environment = BATCH)
    public void testTargetMachineItemFurnace(GameTestHelper helper) {
        testTargetMachineItem(helper, false, Blocks.FURNACE, Items.FURNACE);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT, environment = BATCH)
    public void testTargetMachineItemAttunedCraftingTable(GameTestHelper helper) {
        testTargetMachineItem(helper, true, Blocks.CRAFTING_TABLE, Items.CRAFTING_TABLE);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT, environment = BATCH)
    public void testTargetMachineItemAttunedFurnace(GameTestHelper helper) {
        testTargetMachineItem(helper, true, Blocks.FURNACE, Items.FURNACE);
    }

}
