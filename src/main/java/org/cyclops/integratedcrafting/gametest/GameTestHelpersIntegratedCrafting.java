package org.cyclops.integratedcrafting.gametest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    public static NetworkPositions createBasicNetwork(GameTestHelper helper, BlockPos pos) {
        return createBasicNetwork(helper, pos, Blocks.CRAFTING_TABLE);
    }

    public static NetworkPositions createBasicNetwork(GameTestHelper helper, BlockPos pos, Block... crafters) {
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
        List<PartTypeInterfaceCrafting.State> interfaceStates = Lists.newArrayList();
        List<Consumer<Triple<Integer, RecipeType<?>, ResourceLocation>>> interfaceRecipeAdders = Lists.newArrayList();
        for (Block crafter : crafters) {
            if (!pos.equals(posi)) {
                helper.setBlock(posi, RegistryEntries.BLOCK_CABLE.value());
            }

            // Place cables to support crafting interface above crafter
            helper.setBlock(posi.above(), RegistryEntries.BLOCK_CABLE.value());
            helper.setBlock(posi.above().west(), RegistryEntries.BLOCK_CABLE.value());

            // Place crafter
            PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posi.above().west()), Direction.DOWN, PartTypes.INTERFACE_CRAFTING, new ItemStack(PartTypes.INTERFACE_CRAFTING.getItem()));

            // Place crafter before crafting interface
            helper.setBlock(posi.west(), crafter);

            if (crafter == Blocks.FURNACE) {
                helper.setBlock(posi.west(), crafter.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST));

                // Add fuel
                FurnaceBlockEntity furnace = helper.getBlockEntity(posi.west());
                furnace.setItem(1, new ItemStack(Items.COAL, 64));

                // Extract result
                helper.setBlock(posi.below(), RegistryEntries.BLOCK_CABLE.value());
                helper.setBlock(posi.below().west(), RegistryEntries.BLOCK_CABLE.value());
                PartHelpers.addPart(helper.getLevel(), helper.absolutePos(posi.below().west()), Direction.UP, org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM, new ItemStack(org.cyclops.integratedtunnels.part.PartTypes.IMPORTER_ITEM.getItem()));
                placeVariableInWriter(helper.getLevel(), PartPos.of(helper.getLevel(), helper.absolutePos(posi.below().west()), Direction.UP), TunnelAspects.Write.Item.BOOLEAN_IMPORT, new ItemStack(RegistryEntries.ITEM_VARIABLE));
            }

            interfaces.add(PartPos.of(helper.getLevel(), helper.absolutePos(posi.above().west()), Direction.DOWN));
            PartTypeInterfaceCrafting.State partStateCraftingInterface = (PartTypeInterfaceCrafting.State) PartHelpers.getPart(PartPos.of(helper.getLevel(), helper.absolutePos(posi.above().west()), Direction.DOWN)).getState();
            interfaceStates.add(partStateCraftingInterface);
            interfaceRecipeAdders.add((pair) -> {
                ItemStack variableRecipe = createVariableForRecipe(helper.getLevel(), pair.getMiddle(), pair.getRight());
                partStateCraftingInterface.getInventoryVariables().setItem(pair.getLeft(), variableRecipe);
            });

            posi = posi.south();
        }

        return new NetworkPositions(pos.east(), PartPos.of(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH), interfaces, interfaceStates, interfaceRecipeAdders);
    }

    public static ItemStack createVariableForRecipe(Level level, RecipeType<?> recipeType, ResourceLocation recipeName) {
        RecipeHolder<?> recipeUnknown = null;
        try {
            recipeUnknown = (RecipeHolder<?>) IModHelpers.get().getCraftingHelpers().<RecipeInput, Recipe>getServerRecipe((RecipeType) recipeType, recipeName).orElseThrow(() -> new IllegalStateException("Recipe " + recipeName.toString() + " could not be found"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> recipeIn = Maps.newIdentityHashMap();
        List<IPrototypedIngredientAlternatives<?, ?>> alternatives = Lists.newArrayList();
        Map<IngredientComponent<?, ?>, List<?>> recipeOut = Maps.newIdentityHashMap();
        if (recipeUnknown.value() instanceof CraftingRecipe recipeCrafting) {
            if (recipeCrafting.canCraftInDimensions(2, 2)) {
                int i = 0;
                for (Ingredient ingredient : recipeCrafting.getIngredients()) {
                    if (ingredient.isEmpty()) {
                        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
                        )));
                    } else {
                        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ingredient.getItems()[0], ItemMatch.ITEM | ItemMatch.DATA)
                        )));
                    }
                    i++;

                    if (i == 2) {
                        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
                        )));
                    }
                }
            } else {
                for (Ingredient ingredient : recipeCrafting.getIngredients()) {
                    if (ingredient.isEmpty()) {
                        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ItemStack.EMPTY, ItemMatch.ITEM | ItemMatch.DATA)
                        )));
                    } else {
                        // First check if the ingredient is a tag.
                        boolean wasTag = false;
                        for (Ingredient.Value value : ingredient.getValues()) {
                            if (value instanceof Ingredient.TagValue tagValue) {
                                alternatives.add(new PrototypedIngredientAlternativesItemStackTag(Lists.newArrayList(tagValue.tag().location().toString()), ItemMatch.ITEM, 1));
                                wasTag = true;
                                break;
                            }
                        }
                        if (wasTag) {
                            continue;
                        }

                        alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                                new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ingredient.getItems()[0], ItemMatch.ITEM | ItemMatch.DATA)
                        )));
                    }
                }
            }
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(recipeCrafting.getResultItem(level.registryAccess())));
        } else if (recipeUnknown.value() instanceof SmeltingRecipe recipeSmelting) {
            for (Ingredient ingredient : recipeSmelting.getIngredients()) {
                alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, ingredient.getItems()[0], ItemMatch.ITEM | ItemMatch.DATA)
                )));
            }
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(recipeSmelting.getResultItem(level.registryAccess())));
        } else if (recipeUnknown.value() instanceof SmithingTransformRecipe recipeSmithing) {
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, recipeSmithing.template.getItems()[0], ItemMatch.ITEM | ItemMatch.DATA)
            )));
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, recipeSmithing.base.getItems()[0], ItemMatch.ITEM | ItemMatch.DATA)
            )));
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, recipeSmithing.addition.getItems()[0], ItemMatch.ITEM | ItemMatch.DATA)
            )));
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(recipeSmithing.getResultItem(level.registryAccess())));
        } else if (recipeUnknown.value() instanceof StonecutterRecipe recipeStoneCutter) {
            alternatives.add(new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                    new PrototypedIngredient<>(IngredientComponents.ITEMSTACK, recipeStoneCutter.getIngredients().getFirst().getItems()[0], ItemMatch.ITEM | ItemMatch.DATA)
            )));
            recipeIn.put(IngredientComponents.ITEMSTACK, alternatives);
            recipeOut.put(IngredientComponents.ITEMSTACK, Lists.newArrayList(recipeStoneCutter.getResultItem(level.registryAccess())));
        } else {
            throw new IllegalStateException("Unknown recipe type " + recipeType);
        }
        return createVariableForValue(level, ValueTypes.OBJECT_RECIPE, ValueObjectTypeRecipe.ValueRecipe.of(new RecipeDefinition(recipeIn, new MixedIngredients(recipeOut))));
    }

    public static void enableRecipeInWriter(GameTestHelper helper, PartPos writerPos, ItemStack itemStack) {
        placeVariableInWriter(helper.getLevel(), writerPos, CraftingAspects.Write.ITEMSTACK_CRAFT, createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_ITEMSTACK, ValueObjectTypeItemStack.ValueItemStack.of(itemStack)));
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

    public static record NetworkPositions(
            BlockPos chest,
            PartPos writer,
            List<PartPos> interfaces,
            List<PartTypeInterfaceCrafting.State> interfaceStates,
            List<Consumer<Triple<Integer, RecipeType<?>, ResourceLocation>>> interfaceRecipeAdders
    ){}

}
