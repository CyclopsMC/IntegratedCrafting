package org.cyclops.integratedcrafting.gametest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesItemStackTag;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesList;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.cyclopscore.helper.IModHelpers;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.aspect.IAspectWrite;
import org.cyclops.integrateddynamics.api.part.aspect.property.IAspectProperties;
import org.cyclops.integrateddynamics.api.part.aspect.property.IAspectPropertyTypeInstance;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integratedtunnels.part.aspect.TunnelAspects;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.createVariableForValue;
import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.placeVariableInWriter;

/**
 * @author rubensworks
 */
public class GameTestHelpersIntegratedCrafting {

    public static INetworkPositions<PartTypeInterfaceCrafting.State> createBasicNetwork(GameTestHelper helper, BlockPos pos) {
        return createBasicNetwork(helper, pos, false);
    }

    public static <T extends PartTypeInterfaceCraftingBase.State<?, ?>> INetworkPositions<T> createBasicNetwork(GameTestHelper helper, BlockPos pos, boolean attuned) {
        return createBasicNetwork(helper, pos, attuned, Blocks.CRAFTING_TABLE);
    }

    public static INetworkPositions<PartTypeInterfaceCrafting.State> createBasicNetwork(GameTestHelper helper, BlockPos pos, Block... crafters) {
        return createBasicNetwork(helper, pos, false, crafters);
    }

    public static <T extends PartTypeInterfaceCraftingBase.State<?, ?>> INetworkPositions<T> createBasicNetwork(GameTestHelper helper, BlockPos pos, boolean attuned, Block... crafters) {
        PartTypeInterfaceCraftingBase<? extends PartTypeInterfaceCraftingBase<?, ?>, ? extends PartTypeInterfaceCraftingBase.State<? extends PartTypeInterfaceCraftingBase<?, ?>, ? extends PartTypeInterfaceCraftingBase.State<?, ?>>> partInterface = attuned ? PartTypes.INTERFACE_CRAFTING_ATTUNED : PartTypes.INTERFACE_CRAFTING;

        // Place cable
        helper.setBlock(pos, RegistryEntries.BLOCK_CABLE.value());

        // Place item interface
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(pos), Direction.EAST, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM.getItem()));

        // Place crafting writer
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH, PartTypes.CRAFTING_WRITER, new ItemStack(PartTypes.CRAFTING_WRITER.getItem()));

        // Place chest before item interface
        helper.setBlock(pos.east(), Blocks.CHEST);

        BlockPos posi = pos;
        List<PartPos> interfaces = Lists.newArrayList();
        List<T> interfaceStates = Lists.newArrayList();
        List<Consumer<Triple<Integer, RecipeType<?>, Identifier>>> interfaceRecipeAdders = Lists.newArrayList();
        for (Block crafter : crafters) {
            if (!pos.equals(posi)) {
                helper.setBlock(posi, RegistryEntries.BLOCK_CABLE.value());
            }

            // Place cables to support crafting interface above crafter
            helper.setBlock(posi.above(), RegistryEntries.BLOCK_CABLE.value());
            helper.setBlock(posi.above().west(), RegistryEntries.BLOCK_CABLE.value());

            // Place crafter interface
            PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posi.above().west()), Direction.DOWN, partInterface, new ItemStack(partInterface.getItem()));

            // Place crafter before crafting interface
            helper.setBlock(posi.west(), crafter);

            if (crafter == Blocks.FURNACE) {
                helper.setBlock(posi.west(), crafter.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST));

                // Add fuel
                FurnaceBlockEntity furnace = helper.getBlockEntity(posi.west(), FurnaceBlockEntity.class);
                furnace.setItem(1, new ItemStack(Items.COAL, 64));

                // Extract result
                helper.setBlock(posi.below(), RegistryEntries.BLOCK_CABLE.value());
                helper.setBlock(posi.below().west(), RegistryEntries.BLOCK_CABLE.value());
                PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posi.below().west()), Direction.UP, org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM.getItem()));
                placeVariableInWriter(helper, helper.getLevel(), PartPos.of(helper.getLevel(), helper.absolutePos(posi.below().west()), Direction.UP), TunnelAspects.Write.Item.BOOLEAN_IMPORT, new ItemStack(RegistryEntries.ITEM_VARIABLE));
            } else if (crafter instanceof org.cyclops.integrateddynamics.block.BlockMechanicalDryingBasin
                    || crafter instanceof org.cyclops.integrateddynamics.block.BlockMechanicalSqueezer) {
                // Add energy battery adjacent to the mechanical machine
                helper.setBlock(posi.west().north(), RegistryEntries.BLOCK_ENERGY_BATTERY.value());
                org.cyclops.integrateddynamics.blockentity.BlockEntityEnergyBattery battery = helper.getBlockEntity(posi.west().north(), org.cyclops.integrateddynamics.blockentity.BlockEntityEnergyBattery.class);
                battery.setEnergyStored(100_000);

                // Extract result from output slots via the DOWN face
                helper.setBlock(posi.below(), RegistryEntries.BLOCK_CABLE.value());
                helper.setBlock(posi.below().west(), RegistryEntries.BLOCK_CABLE.value());
                PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posi.below().west()), Direction.UP, org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM.getItem()));
                placeVariableInWriter(helper, helper.getLevel(), PartPos.of(helper.getLevel(), helper.absolutePos(posi.below().west()), Direction.UP), TunnelAspects.Write.Item.BOOLEAN_IMPORT, new ItemStack(RegistryEntries.ITEM_VARIABLE));

                // Add a dedicated machine with a fluid tank as fluid storage in the network,
                // and extract fluid output from the target machine.
                helper.setBlock(posi.below().west().south(), RegistryEntries.BLOCK_CABLE.value());
                helper.setBlock(posi.west().south(), RegistryEntries.BLOCK_CABLE.value());
                helper.setBlock(posi.west().south().above(), RegistryEntries.BLOCK_MECHANICAL_DRYING_BASIN.value());
                PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posi.west().south()), Direction.UP, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_FLUID, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_FLUID.getItem()));
                PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posi.west().south()), Direction.NORTH, org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_FLUID, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_FLUID.getItem()));
                placeVariableInWriter(helper, helper.getLevel(), PartPos.of(helper.getLevel(), helper.absolutePos(posi.west().south()), Direction.NORTH), TunnelAspects.Write.Fluid.BOOLEAN_IMPORT, new ItemStack(RegistryEntries.ITEM_VARIABLE));
            }

            interfaces.add(PartPos.of(helper.getLevel(), helper.absolutePos(posi.above().west()), Direction.DOWN));
            T partStateCraftingInterface = (T) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(posi.above().west()), Direction.DOWN)).getState();
            interfaceStates.add(partStateCraftingInterface);
            interfaceRecipeAdders.add((pair) -> {
                ItemStack variableRecipe = createVariableForRecipe(helper.getLevel(), pair.getMiddle(), pair.getRight());
                if (partStateCraftingInterface instanceof PartTypeInterfaceCrafting.State partStateCraftingInterfaceRegular) {
                    partStateCraftingInterfaceRegular.getInventoryVariables().setItem(pair.getLeft(), variableRecipe);
                }
            });

            posi = posi.south();
        }

        return new NetworkPositions(pos.east(), PartPos.of(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH), interfaces, interfaceStates, interfaceRecipeAdders);
    }

    public static ItemStack createVariableForRecipe(Level level, RecipeType<?> recipeType, Identifier recipeName) {
        RecipeHolder<?> recipeUnknown = null;
        RecipeDisplay recipeDisplay = null;
        try {
            ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeName);
            recipeUnknown = (RecipeHolder<?>) IModHelpers.get().getCraftingHelpers().<RecipeInput, Recipe>getRecipe((RecipeType) recipeType, recipeKey).orElseThrow(() -> new IllegalStateException("Recipe " + recipeName.toString() + " could not be found"));
            recipeDisplay = IModHelpers.get().getCraftingHelpers().getRecipeDisplays(recipeType, recipeKey).getFirst().display();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> recipeIn = Maps.newIdentityHashMap();
        List<IPrototypedIngredientAlternatives<?, ?>> alternatives = Lists.newArrayList();
        Map<IngredientComponent<?, ?>, List<?>> recipeOut = Maps.newIdentityHashMap();
        ItemStack result = recipeDisplay.result().resolveForFirstStack(SlotDisplayContext.fromLevel(level));
        if (recipeUnknown.value() instanceof CraftingRecipe recipeCrafting) {
            int width = 3;
            int height = 3;
            for (int i = 0; i < width * height; i++) {
                alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
                )));
            }
            List<Ingredient> ingredients = recipeCrafting.placementInfo().ingredients();
            PlaceRecipeHelper.placeRecipe(width, height, recipeCrafting, recipeCrafting.placementInfo().slotsToIngredientIndex(), (ingredientSlot, slot, x, y) -> {
                // This is a bit hacky, see VanillaRecipeTypeRecipeHandler for a better implementation.
                // First check if the ingredient is a tag.
                String tag = null;
                if (ingredientSlot >= 0 && slot >= 0) {
                    RecipeDisplay display = recipeCrafting.display().get(0);
                    if (display instanceof ShapelessCraftingRecipeDisplay displayCrafting && displayCrafting.ingredients().get(ingredientSlot) instanceof SlotDisplay.TagSlotDisplay slotTag) {
                        tag = slotTag.tag().location().toString();
                    } else if (display instanceof ShapedCraftingRecipeDisplay displayCrafting && slot < displayCrafting.ingredients().size() && displayCrafting.ingredients().get(slot) instanceof SlotDisplay.TagSlotDisplay slotTag) {
                        tag = slotTag.tag().location().toString();
                    }
                }

                if (tag != null) {
                    alternatives.set(slot, new PrototypedIngredientAlternativesItemStackTag(Lists.newArrayList(tag), ItemMatch.ITEM, 1));
                } else {
                    ItemStack itemStack = ingredientSlot < 0 ? ItemStack.EMPTY : new ItemStack(ingredients.get(ingredientSlot).items().findFirst().get());
                    alternatives.set(slot, new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                            new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, itemStack, ItemMatch.ITEM | ItemMatch.DATA)
                    )));
                }
            });

            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(result));
        } else if (recipeUnknown.value() instanceof SmeltingRecipe recipeSmelting) {
            for (Ingredient ingredient : recipeSmelting.placementInfo().ingredients()) {
                alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(ingredient.items().findFirst().get()), ItemMatch.ITEM | ItemMatch.DATA)
                )));
            }
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(result));
        } else if (recipeUnknown.value() instanceof SmithingTransformRecipe recipeSmithing) {
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(recipeSmithing.template.get().items().findFirst().get()), ItemMatch.ITEM | ItemMatch.DATA)
            )));
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(recipeSmithing.base.items().findFirst().get()), ItemMatch.ITEM | ItemMatch.DATA)
            )));
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(recipeSmithing.addition.get().items().findFirst().get()), ItemMatch.ITEM | ItemMatch.DATA)
            )));
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(result));
        } else if (recipeUnknown.value() instanceof StonecutterRecipe recipeStoneCutter) {
            for (Ingredient ingredient : recipeStoneCutter.placementInfo().ingredients()) {
                alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(ingredient.items().findFirst().get()), ItemMatch.ITEM | ItemMatch.DATA)
                )));
            }
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(result));
        } else if (recipeUnknown.value() instanceof org.cyclops.integrateddynamics.core.recipe.type.RecipeDryingBasin recipeDryingBasin) {
            recipeDryingBasin.getInputIngredient().ifPresent(ingredient ->
                    alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                            new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(ingredient.items().findFirst().get()), ItemMatch.ITEM | ItemMatch.DATA)
                    )))
            );
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeDryingBasin.getOutputItemFirst().ifPresent(outputItem ->
                    recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(outputItem))
            );
        } else if (recipeUnknown.value() instanceof org.cyclops.integrateddynamics.core.recipe.type.RecipeSqueezer recipeSqueezer) {
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, new ItemStack(recipeSqueezer.getInputIngredient().items().findFirst().get()), ItemMatch.ITEM | ItemMatch.DATA)
            )));
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            java.util.List<ItemStack> squeezerOutputItems = recipeSqueezer.getOutputItems().stream()
                    .filter(ic -> ic.getChance() == 1.0F)
                    .map(ic -> ic.getIngredientFirst().copy())
                    .filter(stack -> !stack.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            if (!squeezerOutputItems.isEmpty()) {
                recipeOut.put(IngredientComponents.ITEMSTACK, squeezerOutputItems);
            }
            recipeSqueezer.getOutputFluid().ifPresent(outputFluid ->
                    recipeOut.put(IngredientComponents.FLUIDSTACK, Lists.newArrayList(outputFluid))
            );
        } else {
            throw new IllegalStateException("Unknown recipe type " + recipeType);
        }
        return createVariableForValue(level, ValueTypes.OBJECT_RECIPE, ValueObjectTypeRecipe.ValueRecipe.of(new RecipeDefinition(recipeIn, new MixedIngredients(recipeOut))));
    }

    public static void enableRecipeInWriter(GameTestHelper helper, PartPos writerPos, ItemStack itemStack) {
        placeVariableInWriter(helper, helper.getLevel(), writerPos, CraftingAspects.Write.ITEMSTACK_CRAFT, createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_ITEMSTACK, ValueObjectTypeItemStack.ValueItemStack.of(itemStack)));
    }

    public static <T extends IValueType<V>, V extends IValue> void setWriterAspectProperty(PartPos writerPos, IAspectWrite aspect, IAspectPropertyTypeInstance<T, V> type, V value) {
        PartHelpers.PartStateHolder partStateHolder = PartHelpers.getPart(writerPos);
        IAspectProperties properties = aspect.getProperties(partStateHolder.getPart(), PartTarget.fromCenter(writerPos), partStateHolder.getState());
        properties.setValue(type, value);
        partStateHolder.getState().setAspectProperties(aspect, properties);
    }

    public static <T extends IValueType<V>, V extends IValue> void setCraftingInterfaceBlockingMode(PartPos writerPos, boolean blocking) {
        PartHelpers.PartStateHolder partStateHolder = PartHelpers.getPart(writerPos);
        ((PartTypeInterfaceCrafting.State) partStateHolder.getState()).getCraftingJobHandler().setBlockingJobsMode(blocking);
    }

    public static <T extends IValueType<V>, V extends IValue> void setCraftingInterfaceUpdateInterval(PartPos writerPos, int updateInterval) {
        PartHelpers.PartStateHolder partStateHolder = PartHelpers.getPart(writerPos);
        partStateHolder.getState().setUpdateInterval(updateInterval);
    }

    public static void chestContains(GameTestHelper helper, ChestBlockEntity chest, ItemStack itemStack) {
        boolean found = false;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (ItemStack.matches(chest.getItem(i), itemStack)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new GameTestAssertException(Component.literal("Could not find " + itemStack + " in chest"), (int) helper.getTick());
        }
    }

    public static record NetworkPositions<T extends PartTypeInterfaceCraftingBase.State<?, ?>> (
            BlockPos chest,
            PartPos writer,
            List<PartPos> interfaces,
            List<T> interfaceStates,
            List<Consumer<Triple<Integer, RecipeType<?>, Identifier>>> interfaceRecipeAdders
    ) implements INetworkPositions<T> {}

    public static interface INetworkPositions<T extends PartTypeInterfaceCraftingBase.State<?, ?>> {
        public BlockPos chest();
        public PartPos writer();
        public List<PartPos> interfaces();
        public List<T> interfaceStates();
        public List<Consumer<Triple<Integer, RecipeType<?>, Identifier>>> interfaceRecipeAdders();
    }

}
