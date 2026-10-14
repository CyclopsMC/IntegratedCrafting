package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobStatus;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingAttuned;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;

/**
 * Tests for attuned crafting interfaces whose target changes while crafting.
 *
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsAttunedTarget {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 1000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * A furnace notifies its neighbours whenever its contents change,
     * which must not remove the jobs of its attuned crafting interface from the network.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedTargetUpdatesKeepJobDependencies(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true, Blocks.CRAFTING_TABLE, Blocks.FURNACE);
        ChestBlockEntity chest = helper.getBlockEntity(POS.east());
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 1));

        PartTypeInterfaceCraftingAttuned.State interfaceCraftingTable = positions.interfaceStates().get(0);
        PartTypeInterfaceCraftingAttuned.State interfaceFurnace = positions.interfaceStates().get(1);
        CraftingJob[] jobs = new CraftingJob[2];

        helper.runAfterDelay(5, () -> {
            INetwork network = getNetwork(positions);
            jobs[0] = CraftingHelpers.calculateAndScheduleCraftingJob(network, IPositionedAddonsNetwork.DEFAULT_CHANNEL,
                    IngredientComponents.ITEMSTACK, new ItemStack(Items.STONE_BUTTON), ItemMatch.ITEM, true, false,
                    CraftingHelpers.getGlobalCraftingJobIdentifier(), null);
            helper.assertTrue(jobs[0] != null, "Crafting job could not be scheduled");
            CraftingJobDependencyGraph graph = CraftingHelpers.getCraftingNetworkChecked(network).getCraftingJobDependencyGraph();
            helper.assertValueEqual(graph.getDependencies(jobs[0]).size(), 1, "Dependencies of the button job");
            jobs[1] = graph.getDependencies(jobs[0]).iterator().next();
        });

        helper.onEachTick(() -> {
            if (jobs[1] == null) {
                return;
            }
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(getNetwork(positions));

            // As long as the furnace is smelting, the button job must keep waiting for it
            if (interfaceFurnace.getCraftingJobHandler().getAllCraftingJobs().containsKey(jobs[1].getId())) {
                helper.assertTrue(craftingNetwork.getCraftingJobDependencyGraph().getCraftingJob(jobs[1].getId()) != null,
                        "The smelting job left the dependency graph");
                helper.assertValueEqual(interfaceCraftingTable.getCraftingJobStatus(craftingNetwork, jobs[0].getChannel(), jobs[0].getId()),
                        CraftingJobStatus.PENDING_DEPENDENCIES, "Status of the button job");
            }

            if (chest.countItem(Items.STONE_BUTTON) == 1) {
                helper.succeed();
            }
        });
    }

    protected static INetwork getNetwork(GameTestHelpersIntegratedCrafting.INetworkPositions<?> positions) {
        return NetworkHelpers.getNetworkChecked(positions.writer().getPos().getLevel(true),
                positions.writer().getPos().getBlockPos(), positions.writer().getSide());
    }
}
