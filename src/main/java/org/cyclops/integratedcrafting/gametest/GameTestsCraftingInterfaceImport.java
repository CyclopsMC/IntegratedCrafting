package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Map;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesList;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeFluidStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createVariableForRecipe;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.createVariableForValue;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.placeVariableInWriter;

/**
 * Tests for crafting results that a machine pushes directly into its crafting interface.
 *
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsCraftingInterfaceImport {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsPushedIntoInterfaceFinishJob(GameTestHelper helper) {
        testItemsPushedIntoInterface(helper, 2);
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testItemsPushedIntoInterfaceFinishJobBatch(GameTestHelper helper) {
        testItemsPushedIntoInterface(helper, 8);
    }

    protected void testItemsPushedIntoInterface(GameTestHelper helper, int amount) {
        // Storage
        helper.setBlock(POS, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.EAST, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM.getItem()));
        helper.setBlock(POS.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(POS.east());
        chest.setItem(0, new ItemStack(Items.RAW_IRON, amount));

        // Crafting writer
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH, PartTypes.CRAFTING_WRITER, new ItemStack(PartTypes.CRAFTING_WRITER.getItem()));
        PartPos writer = PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH);

        // Crafting interface on top of a furnace, without any importer
        BlockPos posInterface = POS.above().west();
        helper.setBlock(POS.above(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(posInterface, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN, PartTypes.INTERFACE_CRAFTING, new ItemStack(PartTypes.INTERFACE_CRAFTING.getItem()));
        PartTypeInterfaceCrafting.State interfaceState = (PartTypeInterfaceCrafting.State) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN)).getState();
        interfaceState.getInventoryVariables().setItem(0, createVariableForRecipe(helper.getLevel(), RecipeType.SMELTING,
                ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot_from_smelting_raw_iron")));

        helper.setBlock(POS.west(), Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST));
        FurnaceBlockEntity furnace = helper.getBlockEntity(POS.west());
        furnace.setItem(1, new ItemStack(Items.COAL, 64));

        // Let the furnace push its results upwards, like machines that auto-output to neighbours
        helper.onEachTick(() -> {
            ItemStack result = furnace.getItem(2);
            if (!result.isEmpty()) {
                IItemHandler target = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                        helper.absolutePos(posInterface), Direction.DOWN);
                helper.assertTrue(target != null, "The crafting interface should expose an item handler");
                // Simulate first, as many machines do
                ItemStack remaining = target.insertItem(0, result.copy(), true);
                if (remaining.getCount() < result.getCount()) {
                    furnace.setItem(2, target.insertItem(0, result.copy(), false));
                }
            }
        });

        enableRecipeInWriter(helper, writer, new ItemStack(Items.IRON_INGOT, amount));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(chest.getItem(0).getItem(), Items.IRON_INGOT, "Slot 0 item is incorrect");
            helper.assertValueEqual(chest.getItem(0).getCount(), amount, "Slot 0 amount is incorrect");
            helper.assertFalse(interfaceState.getCraftingJobs().hasNext(), "The crafting job should be finished");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testFluidsPushedIntoInterfaceFinishJob(GameTestHelper helper) {
        // Storage: items in a chest, fluids in a drying basin
        helper.setBlock(POS, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.EAST, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM.getItem()));
        helper.setBlock(POS.east(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(POS.east());
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.SOUTH, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_FLUID, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_FLUID.getItem()));
        helper.setBlock(POS.south(), RegistryEntries.BLOCK_DRYING_BASIN.value());

        // Crafting writer
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH, PartTypes.CRAFTING_WRITER, new ItemStack(PartTypes.CRAFTING_WRITER.getItem()));
        PartPos writer = PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.NORTH);

        // Crafting interface on top of a machine that turns cobblestone into water
        BlockPos posInterface = POS.above().west();
        helper.setBlock(POS.above(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(posInterface, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN, PartTypes.INTERFACE_CRAFTING, new ItemStack(PartTypes.INTERFACE_CRAFTING.getItem()));
        PartTypeInterfaceCrafting.State interfaceState = (PartTypeInterfaceCrafting.State) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(posInterface), Direction.DOWN)).getState();
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> recipeIn = Maps.newIdentityHashMap();
        recipeIn.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(Items.COBBLESTONE), ItemMatch.ITEM)))));
        Map<IngredientComponent<?, ?>, List<?>> recipeOut = Maps.newIdentityHashMap();
        recipeOut.put(IngredientComponents.FLUIDSTACK, Lists.newArrayList(new FluidStack(Fluids.WATER, 100)));
        interfaceState.getInventoryVariables().setItem(0, createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_RECIPE,
                ValueObjectTypeRecipe.ValueRecipe.of(new RecipeDefinition(recipeIn, new MixedIngredients(recipeOut)))));

        helper.setBlock(POS.west(), Blocks.BARREL);
        BarrelBlockEntity machine = helper.getBlockEntity(POS.west());

        // Let the machine push its results upwards, like machines that auto-output to neighbours
        helper.onEachTick(() -> {
            if (!machine.getItem(0).isEmpty()) {
                IFluidHandler target = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                        helper.absolutePos(posInterface), Direction.DOWN);
                helper.assertTrue(target != null, "The crafting interface should expose a fluid handler");
                if (target.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE) == 100) {
                    target.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);
                    machine.getItem(0).shrink(1);
                    machine.setChanged();
                }
            }
        });

        placeVariableInWriter(helper.getLevel(), writer, CraftingAspects.Write.FLUIDSTACK_CRAFT,
                createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_FLUIDSTACK, ValueObjectTypeFluidStack.ValueFluidStack.of(new FluidStack(Fluids.WATER, 300))));

        helper.succeedWhen(() -> {
            IFluidHandler basin = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(POS.south()), null);
            helper.assertValueEqual(basin.getFluidInTank(0).getAmount(), 300, "Water amount is incorrect");
            helper.assertTrue(chest.getItem(0).isEmpty(), "Cobblestone should be consumed");
            helper.assertFalse(interfaceState.getCraftingJobs().hasNext(), "The crafting job should be finished");
        });
    }

}
