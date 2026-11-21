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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.RegistryEntries;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;

/**
 * Tests for crafting jobs whose network is rebuilt while they are waiting for their outputs.
 *
 * Placing or removing a cable anywhere in a network rebuilds it into a new network object.
 * Crafting jobs that are already running must still detect their outputs once they are inserted into the new network.
 *
 *
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemsCraftNetworkRebuild {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 600;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftIronIngotNetworkRebuiltWhileWaitingForOutput(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                createBasicNetwork(helper, POS, Blocks.FURNACE);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.RAW_IRON, 1));

        // Add iron ingot recipe to furnace
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.SMELTING,
                ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.IRON_INGOT));

        PartTypeInterfaceCrafting.State interfaceState = positions.interfaceStates().get(0);

        // While the furnace is smelting, rebuild the network by connecting an additional cable to it
        helper.runAtTickTime(60, () -> {
            helper.assertBlockProperty(POS.west(), AbstractFurnaceBlock.LIT, true);
            helper.assertTrue(interfaceState.getCraftingJobs().hasNext(), "A crafting job should be running");
            helper.setBlock(POS.south(), RegistryEntries.BLOCK_CABLE.value());
        });

        helper.succeedWhen(() -> {
            // The result reached storage
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.IRON_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 1, "Slot 0 amount is incorrect");

            // And the crafting job noticed it, so it is not waiting for its output anymore
            helper.assertFalse(interfaceState.getCraftingJobs().hasNext(),
                    "The crafting job should have completed after its output was inserted");
        });
    }

}
