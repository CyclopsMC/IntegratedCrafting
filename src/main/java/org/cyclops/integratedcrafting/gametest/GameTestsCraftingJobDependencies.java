package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;

/**
 * Tests for the dependencies between crafting jobs in the network.
 *
 * @author rubensworks
 */
public class GameTestsCraftingJobDependencies {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * A crafting interface that re-registers itself to the network must not duplicate the dependencies of its jobs.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testDependenciesNotDuplicatedOnInterfaceReRegister(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east(), ChestBlockEntity.class);
        chestIn.setItem(0, new ItemStack(Items.OAK_LOG, 2));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, Identifier.fromNamespaceAndPath("minecraft", "chest")));
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(1, RecipeType.CRAFTING, Identifier.fromNamespaceAndPath("minecraft", "oak_planks")));

        helper.runAfterDelay(5, () -> {
            INetwork network = NetworkHelpers.getNetworkChecked(positions.writer().getPos().getLevel(true),
                    positions.writer().getPos().getBlockPos(), positions.writer().getSide());
            CraftingJob craftingJob = CraftingHelpers.calculateAndScheduleCraftingJob(network, IPositionedAddonsNetwork.DEFAULT_CHANNEL,
                    IngredientComponents.ITEMSTACK, new ItemStack(Items.CHEST), ItemMatch.ITEM, true, false,
                    CraftingHelpers.getGlobalCraftingJobIdentifier(), null);
            helper.assertTrue(craftingJob != null, "Crafting job could not be scheduled");
            CraftingJobDependencyGraph graph = CraftingHelpers.getCraftingNetworkChecked(network).getCraftingJobDependencyGraph();
            helper.assertValueEqual(graph.getDependencies(craftingJob).size(), 1, "Dependencies before re-registering");

            // Re-registers the interface and its jobs to the network
            positions.interfaceStates().get(0).onDirty();

            helper.assertValueEqual(graph.getDependencies(craftingJob).size(), 1, "Dependencies after re-registering");
            CraftingJob dependency = graph.getDependencies(craftingJob).iterator().next();
            helper.assertValueEqual(graph.getDependents(dependency).size(), 1, "Dependents after re-registering");

            helper.succeedWhen(() -> {
                helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.CHEST, "Slot 0 item is incorrect");
                helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");
            });
        });
    }
}
