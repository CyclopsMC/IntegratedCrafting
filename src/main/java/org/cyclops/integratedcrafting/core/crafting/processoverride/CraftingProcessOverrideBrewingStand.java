package org.cyclops.integratedcrafting.core.crafting.processoverride;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.BlockEntityHelpers;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A crafting process override for brewing stands.
 * This makes it so that items can be inserted from any slot,
 * instead of the annoying side restrictions that vanilla adds.
 * @author rubensworks
 */
public class CraftingProcessOverrideBrewingStand implements ICraftingProcessOverride {

    private static final Direction SIDE_INGREDIENT = Direction.UP;
    private static final Direction SIDE_BOTTLE = Direction.NORTH;

    @Override
    public boolean isApplicable(PartPos target) {
        return getTile(target).isPresent();
    }

    @Nullable
    private Optional<BrewingStandBlockEntity> getTile(PartPos target) {
        return BlockEntityHelpers.get(target.getPos(), BrewingStandBlockEntity.class);
    }

    @Override
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                         IMixedIngredients ingredients, ICraftingResultsSink resultsSink, boolean simulate) {
        // Validate the ingredients
        List<ItemStack> instances = ingredients.getInstances(IngredientComponent.ITEMSTACK);
        if (instances.size() != 4 || ingredients.getComponents().size() != 1) {
            return false;
        }

        // Insert the ingredients into the target
        return getTile(targetGetter.apply(IngredientComponent.ITEMSTACK))
                .map(tile -> {
                    IItemHandler ingredientHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, SIDE_INGREDIENT).orElse(null);
                    IItemHandler bottleHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, SIDE_BOTTLE).orElse(null);
                    if (ingredientHandler != null && bottleHandler != null) {
                        int ingredientSlotIndex = 0;
                        int bottleSlotIndex = 0;
                        for (ItemStack instance : instances) {
                            if (BrewingRecipeRegistry.isValidIngredient(instance)) {
                                // The instance is for the ingredient slot
                                if (!ingredientHandler.insertItem(ingredientSlotIndex++, instance, simulate).isEmpty()) {
                                    return false;
                                }
                            } else {
                                // The instance is for one of the bottle slots
                                if (!bottleHandler.insertItem(bottleSlotIndex++, instance, simulate).isEmpty()) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

}
