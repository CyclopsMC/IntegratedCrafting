package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.*;

/**
 * Game tests for the machine that crafting interfaces are targeting.
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsCraftingInterfaceTargetMachine {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    protected void testTargetMachineItem(GameTestHelper helper, boolean attuned, Block crafter, Item expectedItem) {
        INetworkPositions<PartTypeInterfaceCraftingBase.State<?, ?>> positions = createBasicNetwork(helper, POS, attuned, crafter);

        helper.succeedWhen(() -> {
            ItemStack machineItem = positions.interfaceStates().get(0).getTargetMachineItem();
            helper.assertValueEqual(machineItem.getItem(), expectedItem, "Target machine item is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testTargetMachineItemCraftingTable(GameTestHelper helper) {
        testTargetMachineItem(helper, false, Blocks.CRAFTING_TABLE, Items.CRAFTING_TABLE);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testTargetMachineItemFurnace(GameTestHelper helper) {
        testTargetMachineItem(helper, false, Blocks.FURNACE, Items.FURNACE);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testTargetMachineItemAttunedCraftingTable(GameTestHelper helper) {
        testTargetMachineItem(helper, true, Blocks.CRAFTING_TABLE, Items.CRAFTING_TABLE);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testTargetMachineItemAttunedFurnace(GameTestHelper helper) {
        testTargetMachineItem(helper, true, Blocks.FURNACE, Items.FURNACE);
    }

}
