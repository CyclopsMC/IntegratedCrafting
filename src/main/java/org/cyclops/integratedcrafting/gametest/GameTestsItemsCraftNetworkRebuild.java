package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.core.PendingCraftingJobResultIndexObserver;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;

/**
 * Tests for crafting jobs whose network is rebuilt while they are waiting for their outputs.
 *
 * Placing or removing a cable anywhere in a network rebuilds it into a new network object.
 * Running crafting jobs must still detect their outputs once they are inserted into the new network.
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemsCraftNetworkRebuild {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 600;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    protected static PartTypeInterfaceCrafting.State startIronIngotJob(GameTestHelper helper,
                                                                       GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions) {
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.RAW_IRON, 1));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.SMELTING,
                ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_INGOT));
        return positions.interfaceStates().get(0);
    }

    protected static void assertJobCompletedWithOutput(GameTestHelper helper, PartTypeInterfaceCrafting.State interfaceState) {
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_INGOT, "Slot 0 item is incorrect");
        helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
        helper.assertFalse(interfaceState.getCraftingJobs().hasNext(),
                "The crafting job should have completed after its output was inserted");
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftNetworkRebuiltByCableAdditionWhileWaitingForOutput(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS, Blocks.FURNACE);
        PartTypeInterfaceCrafting.State interfaceState = startIronIngotJob(helper, positions);

        helper.runAtTickTime(60, () -> {
            helper.assertBlockProperty(POS.west(), AbstractFurnaceBlock.LIT, true);
            helper.assertTrue(interfaceState.getCraftingJobs().hasNext(), "A crafting job should be running");
            helper.setBlock(POS.south(), RegistryEntries.BLOCK_CABLE.value());
        });

        helper.succeedWhen(() -> assertJobCompletedWithOutput(helper, interfaceState));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftNetworkRebuiltByCableRemovalWhileWaitingForOutput(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS, Blocks.FURNACE);
        helper.setBlock(POS.south(), RegistryEntries.BLOCK_CABLE.value());
        PartTypeInterfaceCrafting.State interfaceState = startIronIngotJob(helper, positions);

        helper.runAtTickTime(60, () -> {
            helper.assertBlockProperty(POS.west(), AbstractFurnaceBlock.LIT, true);
            helper.assertTrue(interfaceState.getCraftingJobs().hasNext(), "A crafting job should be running");
            helper.setBlock(POS.south(), Blocks.AIR);
        });

        helper.succeedWhen(() -> assertJobCompletedWithOutput(helper, interfaceState));
    }

    /**
     * Outputs can arrive in the same tick as the rebuild, before the crafting interface is ticked again,
     * for example when a machine pushes its outputs into an item interface.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftNetworkRebuiltRightBeforeOutputInserted(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS, Blocks.FURNACE);

        // Without fuel, the output is only produced by the insertion below
        FurnaceBlockEntity furnace = helper.getBlockEntity(POS.west());
        furnace.setItem(1, ItemStack.EMPTY);

        PartTypeInterfaceCrafting.State interfaceState = startIronIngotJob(helper, positions);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(interfaceState.getCraftingJobs().hasNext(), "A crafting job should be running");
                    helper.assertValueEqual(furnace.getItem(0).getItem(), Items.RAW_IRON, "The furnace should hold the input");
                })
                .thenExecute(() -> {
                    helper.setBlock(POS.south(), RegistryEntries.BLOCK_CABLE.value());
                    INetwork network = NetworkHelpers.getNetworkChecked(positions.writer());
                    ItemStack remaining = CraftingHelpers.getNetworkStorage(network, IPositionedAddonsNetwork.DEFAULT_CHANNEL,
                            IngredientComponent.ITEMSTACK, false).insert(new ItemStack(Items.IRON_INGOT), false);
                    helper.assertTrue(remaining.isEmpty(), "The output should be inserted");
                })
                .thenWaitUntil(() -> assertJobCompletedWithOutput(helper, interfaceState))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftInterfaceRemovedWhileWaitingForOutput(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS, Blocks.FURNACE);
        PartTypeInterfaceCrafting.State interfaceState = startIronIngotJob(helper, positions);
        PartPos interfacePos = positions.interfaces().get(0);
        FurnaceBlockEntity furnace = helper.getBlockEntity(POS.west());

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(interfaceState.getCraftingJobs().hasNext(), "A crafting job should be running");
                    helper.assertValueEqual(furnace.getItem(0).getItem(), Items.RAW_IRON, "The furnace should hold the input");
                })
                .thenExecute(() -> PartHelpers.removePart(helper.getLevel(), interfacePos.getPos().getBlockPos(), interfacePos.getSide(), null, false, false, false))
                .thenExecute(() -> {
                    INetwork network = NetworkHelpers.getNetworkChecked(positions.writer());
                    long observers = CraftingHelpers.getIngredientsNetworkChecked(network, IngredientComponent.ITEMSTACK)
                            .getInsertPreConsumers().stream()
                            .filter(preConsumer -> preConsumer instanceof PendingCraftingJobResultIndexObserver)
                            .count();
                    helper.assertValueEqual(observers, 0L, "Observers of the removed interface should be unregistered");
                })
                .thenSucceed();
    }

}
