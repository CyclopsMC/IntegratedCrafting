package org.cyclops.integratedcrafting.core.crafting.processoverride;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.cyclopscore.helper.IModHelpers;
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
        DimPos dimPos = target.getPos();
        return IModHelpers.get().getBlockEntityHelpers().get(dimPos.getLevel(true), dimPos.getBlockPos(), BrewingStandBlockEntity.class);
    }

    @Override
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                         IMixedIngredients ingredients, IRecipeDefinition recipe, ICraftingResultsSink resultsSink, boolean simulate) {
        // Validate the ingredients
        List<ItemStack> instances = ingredients.getInstances(IngredientComponent.ITEMSTACK);
        if (instances.size() != 4 || ingredients.getComponents().size() != 1) {
            return false;
        }

        // Insert the ingredients into the target
        return getTile(targetGetter.apply(IngredientComponent.ITEMSTACK))
                .map(tile -> {
                    ResourceHandler<ItemResource> ingredientHandler = tile.getLevel().getCapability(Capabilities.Item.BLOCK, tile.getBlockPos(), tile.getBlockState(), tile, SIDE_INGREDIENT);
                    ResourceHandler<ItemResource> bottleHandler = tile.getLevel().getCapability(Capabilities.Item.BLOCK, tile.getBlockPos(), tile.getBlockState(), tile, SIDE_BOTTLE);
                    if (ingredientHandler != null && bottleHandler != null) {
                        int ingredientSlotIndex = 0;
                        int bottleSlotIndex = 0;
                        for (ItemStack instance : instances) {
                            if (tile.getLevel().potionBrewing().isIngredient(instance)) {
                                // The instance is for the ingredient slot
                                int inserted;
                                try (var tx = Transaction.openRoot()) {
                                    inserted = ingredientHandler.insert(ingredientSlotIndex++, ItemResource.of(instance), instance.getCount(), tx);
                                    if (!simulate) {
                                        tx.commit();
                                    }
                                }
                                if (inserted == 0) {
                                    return false;
                                }
                            } else {
                                // The instance is for one of the bottle slots
                                int inserted;
                                try (var tx = Transaction.openRoot()) {
                                    inserted = bottleHandler.insert(bottleSlotIndex++, ItemResource.of(instance), instance.getCount(), tx);
                                    if (!simulate) {
                                        tx.commit();
                                    }
                                }
                                if (inserted == 0) {
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
