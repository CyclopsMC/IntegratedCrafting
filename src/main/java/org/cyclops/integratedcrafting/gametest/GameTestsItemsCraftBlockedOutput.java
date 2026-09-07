package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobStatus;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import java.util.Iterator;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;

/**
 * Tests for crafting interfaces whose results can not be pushed into the storage network.
 *
 * A storage network that is full, or that is backed by storages which only accept the item types
 * they already hold, leaves crafting results stuck in the interface's output buffer.
 * The interface then stops making progress, which should be visible in the job status.
 *
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemsCraftBlockedOutput {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 400;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * Fill the storage chest so that it holds the recipe inputs, but has no room left for the result.
     */
    protected static ChestBlockEntity createFullChestWithInputs(GameTestHelper helper) {
        ChestBlockEntity chest = helper.getBlockEntity(POS.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
        for (int slot = 1; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        return chest;
    }

    protected static int countItems(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * A crafting result that the storage network refuses must stay in the interface's output buffer,
     * and the job's status must say so instead of silently sitting there.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testBlockedOutputIsReportedInJobStatus(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS);
        ChestBlockEntity chest = createFullChestWithInputs(helper);

        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING,
                ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST, 1));

        PartTypeInterfaceCrafting.State interfaceState = positions.interfaceStates().get(0);

        helper.runAtTickTime(TIMEOUT - 20, () -> {
            // The result was crafted, but could not be pushed into the network
            helper.assertValueEqual(countItems(chest, Items.CHEST), 0, "Crafted chests in storage");
            helper.assertFalse(interfaceState.getOutputBuffer().isEmpty(),
                    "The crafting interface output buffer should hold the blocked result");

            // And the job says why it is not progressing
            INetwork network = NetworkHelpers.getNetworkChecked(positions.writer().getPos().getLevel(true),
                    positions.writer().getPos().getBlockPos(), positions.writer().getSide());
            Iterator<CraftingJob> craftingJobs = interfaceState.getCraftingJobs();
            helper.assertTrue(craftingJobs.hasNext(), "There should still be a crafting job");
            CraftingJob craftingJob = craftingJobs.next();
            helper.assertValueEqual(
                    interfaceState.getCraftingJobStatus(CraftingHelpers.getCraftingNetworkChecked(network),
                            craftingJob.getChannel(), craftingJob.getId()),
                    CraftingJobStatus.PENDING_OUTPUT_STORAGE,
                    "Crafting job status");

            helper.succeed();
        });
    }

    /**
     * Once the network can accept the result again, the interface has to recover on its own.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testBlockedOutputRecoversWhenStorageFreesUp(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS);
        ChestBlockEntity chest = createFullChestWithInputs(helper);

        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING,
                ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST, 1));

        PartTypeInterfaceCrafting.State interfaceState = positions.interfaceStates().get(0);

        // Make room once the result is stuck
        helper.runAtTickTime(TIMEOUT / 2, () -> {
            helper.assertFalse(interfaceState.getOutputBuffer().isEmpty(),
                    "The crafting interface output buffer should hold the blocked result");
            chest.setItem(1, ItemStack.EMPTY);
        });

        helper.runAtTickTime(TIMEOUT - 20, () -> {
            helper.assertTrue(interfaceState.getOutputBuffer().isEmpty(),
                    "The crafting interface output buffer should have been flushed");

            // The interface picked up where it left off.
            // The crafting writer may have scheduled further jobs in the meantime,
            // so assert on the ratio rather than on an exact count.
            int chests = countItems(chest, Items.CHEST);
            int planks = countItems(chest, Items.OAK_PLANKS);
            helper.assertTrue(chests >= 1, "The blocked result should have reached storage, got " + chests);
            helper.assertValueEqual(64 - planks, chests * 8, "Planks consumed per crafted chest");

            helper.succeed();
        });
    }

}
