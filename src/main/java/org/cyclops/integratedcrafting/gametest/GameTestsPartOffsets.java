package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingAttuned;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.setPartOffset;

/**
 * Game tests for crafting interfaces that target machines via a part offset.
 *
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsPartOffsets {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * The crafting interface points at an empty block, and reaches the crafting table via an offset.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftChestOffset(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

        // Move the crafting table one block away from the crafting interface,
        // and make the crafting interface target it via an offset.
        helper.setBlock(POS.west(), Blocks.AIR);
        helper.setBlock(POS.west().north(), Blocks.CRAFTING_TABLE);
        setPartOffset(positions.interfaces().get(0), new Vec3i(0, 0, -1));

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.OAK_PLANKS, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 56, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CHEST, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
        });
    }

    /**
     * The crafting interface points at a furnace, but must ignore it because an offset makes it target a crafting table.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftChestOffsetIgnoresAdjacentMachine(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS);

        // Place a furnace directly in front of the crafting interface,
        // and make the crafting interface target a crafting table via an offset.
        helper.setBlock(POS.west(), Blocks.FURNACE);
        helper.setBlock(POS.west().north(), Blocks.CRAFTING_TABLE);
        setPartOffset(positions.interfaces().get(0), new Vec3i(0, 0, -1));

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING, ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.OAK_PLANKS, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 56, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CHEST, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
        });
    }

    /**
     * The attuned crafting interface must read the recipes of the machine it targets via an offset.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsCraftAttunedPlanksOffset(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions = createBasicNetwork(helper, POS, true);

        // Move the crafting table one block away from the crafting interface,
        // and make the crafting interface target it via an offset.
        helper.setBlock(POS.west(), Blocks.AIR);
        helper.setBlock(POS.west().north(), Blocks.CRAFTING_TABLE);
        setPartOffset(positions.interfaces().get(0), new Vec3i(0, 0, -1));

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_LOG, 64));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.OAK_PLANKS));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).hasValidTarget(), "Crafting interface has no valid target");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.OAK_LOG, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 63, "Slot 0 amount is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.OAK_PLANKS, "Slot 1 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(1).getCount(), 4, "Slot 1 amount is incorrect");
        });
    }

}
