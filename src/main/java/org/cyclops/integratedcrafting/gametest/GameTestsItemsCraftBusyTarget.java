package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobStatus;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;

/**
 * Tests for crafting interfaces whose target temporarily refuses the recipe inputs.
 *
 * @author rubensworks
 */
public class GameTestsItemsCraftBusyTarget {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
    public static final int TIMEOUT = 600;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * Jobs that a busy target refused must not keep reporting invalid inputs
     * while they are only waiting for another job of the same interface.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testRefusedJobsWaitForInterfaceOnceAnotherJobStarts(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS, Blocks.FURNACE);
        ChestBlockEntity chest = helper.getBlockEntity(POS.east(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.SMELTING,
                Identifier.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));

        // The furnace is busy with something else, so it refuses raw iron for a while
        FurnaceBlockEntity furnace = helper.getBlockEntity(POS.west(), FurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.RAW_GOLD, 1));

        PartTypeInterfaceCrafting.State interfaceState = positions.interfaceStates().get(0);
        CraftingJob[] jobs = new CraftingJob[2];
        boolean[] refused = new boolean[1];

        helper.runAfterDelay(5, () -> {
            INetwork network = getNetwork(positions);
            for (int i = 0; i < jobs.length; i++) {
                jobs[i] = CraftingHelpers.calculateAndScheduleCraftingJob(network, IPositionedAddonsNetwork.DEFAULT_CHANNEL,
                        IngredientComponents.ITEMSTACK, new ItemStack(Items.IRON_INGOT), ItemMatch.ITEM, false, false,
                        CraftingHelpers.getGlobalCraftingJobIdentifier(), null);
                helper.assertTrue(jobs[i] != null, "Crafting job " + i + " could not be scheduled");
            }
        });

        helper.onEachTick(() -> {
            if (jobs[1] == null) {
                return;
            }
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(getNetwork(positions));
            CraftingJobStatus status0 = interfaceState.getCraftingJobStatus(craftingNetwork, jobs[0].getChannel(), jobs[0].getId());
            CraftingJobStatus status1 = interfaceState.getCraftingJobStatus(craftingNetwork, jobs[1].getChannel(), jobs[1].getId());
            if (status0 == CraftingJobStatus.INVALID_INPUTS && status1 == CraftingJobStatus.INVALID_INPUTS) {
                refused[0] = true;
            }

            // Once one job runs, the other one only waits for the interface
            CraftingJobStatus waiting = status0 == CraftingJobStatus.PROCESSING ? status1
                    : status1 == CraftingJobStatus.PROCESSING ? status0 : null;
            if (waiting != null) {
                helper.assertTrue(refused[0], "The busy furnace should have refused both jobs first");
                helper.assertValueEqual(waiting, CraftingJobStatus.PENDING_INTERFACE, "Status of the waiting job");
                helper.succeed();
            }
        });
    }

    protected static INetwork getNetwork(GameTestHelpersIntegratedCrafting.INetworkPositions<?> positions) {
        return NetworkHelpers.getNetworkChecked(positions.writer().getPos().getLevel(true),
                positions.writer().getPos().getBlockPos(), positions.writer().getSide());
    }
}
