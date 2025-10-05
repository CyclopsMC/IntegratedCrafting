package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.write.IPartStateWriter;
import org.cyclops.integrateddynamics.core.block.IgnoredBlockStatus;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.*;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemsStonecutting {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsStonecuttingCopperGrate(GameTestHelper helper) {
        INetworkPositions<PartTypeInterfaceCrafting.State> positions = createBasicNetwork(helper, POS, Blocks.STONECUTTER);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.COPPER_BLOCK, 1));

        // Add chest recipe to crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.STONECUTTING, ResourceLocation.fromNamespaceAndPath("minecraft", "copper_grate_from_copper_block_stonecutting")));

        // Enable crafting aspect in crafting writer
        enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.COPPER_GRATE));

        helper.succeedWhen(() -> {
            // Check crafting interface state
            helper.assertTrue(positions.interfaceStates().get(0).isRecipeSlotValid(0), "Recipe in crafting interface is not valid");

            // Check crafting writer state
            IPartStateWriter partStateWriter = (IPartStateWriter) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)).getState();
            helper.assertFalse(partStateWriter.isDeactivated(), "Importer is deactivated");
            helper.assertValueEqual(
                    PartTypes.CRAFTING_WRITER.getBlockState(PartHelpers.getPartContainerChecked(PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH)), Direction.NORTH).getValue(IgnoredBlockStatus.STATUS),
                    IgnoredBlockStatus.Status.ACTIVE,
                    "Block status is incorrect"
            );
            helper.assertValueEqual(partStateWriter.getActiveAspect(), CraftingAspects.Write.ITEMSTACK_CRAFT, "Active aspect is incorrect");
            helper.assertTrue(partStateWriter.getErrors(CraftingAspects.Write.ITEMSTACK_CRAFT).isEmpty(), "Active aspect has errors");

            // Check if items have been crafted
            helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.COPPER_GRATE, "Slot 0 item is incorrect");
            helper.assertValueEqual(chestIn.getItem(0).getCount(), 4, "Slot 0 amount is incorrect");
        });
    }

}
