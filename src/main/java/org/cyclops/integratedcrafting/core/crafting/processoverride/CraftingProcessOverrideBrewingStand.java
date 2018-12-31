package org.cyclops.integratedcrafting.core.crafting.processoverride;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.TileHelpers;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * A crafting process override for brewing stands.
 * This makes it so that items can be inserted from any slot,
 * instead of the annoying side restrictions that vanilla adds.
 * @author rubensworks
 */
public class CraftingProcessOverrideBrewingStand implements ICraftingProcessOverride {

    private static final EnumFacing SIDE_INGREDIENT = EnumFacing.UP;
    private static final EnumFacing SIDE_BOTTLE = EnumFacing.NORTH;

    @Override
    public boolean isApplicable(PartPos target) {
        return getTile(target) != null;
    }

    @Nullable
    private TileEntityBrewingStand getTile(PartPos target) {
        return TileHelpers.getSafeTile(target.getPos(), TileEntityBrewingStand.class);
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
        TileEntityBrewingStand tile = getTile(targetGetter.apply(IngredientComponent.ITEMSTACK));
        if (tile != null) {
            IItemHandler ingredientHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, SIDE_INGREDIENT);
            IItemHandler bottleHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, SIDE_BOTTLE);
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
        }

        return false;
    }

}
