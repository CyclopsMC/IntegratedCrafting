package org.cyclops.integratedcrafting.core.crafting.processoverride;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SmithingTableBlock;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.CraftingHelpers;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import java.util.function.Function;

/**
 * A crafting process override for smithing tables.
 * @author rubensworks
 */
public class CraftingProcessOverrideSmithingTable implements ICraftingProcessOverride {

    @Override
    public boolean isApplicable(PartPos target) {
        return target.getPos().getLevel(true).getBlockState(target.getPos().getBlockPos()).getBlock() instanceof SmithingTableBlock;
    }

    @Override
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                         IMixedIngredients ingredients, IRecipeDefinition recipe, ICraftingResultsSink resultsSink, boolean simulate) {
        PartPos target = targetGetter.apply(IngredientComponent.ITEMSTACK);
        CraftingGrid grid = new CraftingGrid(ingredients, 1, 3);
        Level level = target.getPos().getLevel(true);

        return CraftingHelpers.findServerRecipe(RecipeType.SMITHING, grid, level)
                .map(smithingRecipe -> {
                    ItemStack result = smithingRecipe.assemble(grid, level.registryAccess());

                    if (result.isEmpty()) {
                        return false;
                    }

                    if (!simulate) {
                        Player player = CraftingProcessOverrideCraftingTable.getFakePlayer((ServerLevel) target.getPos().getLevel(true));

                        // Fire all required events
                        result.onCraftedBy(target.getPos().getLevel(true), player, 1);

                        // Insert the result into the sink
                        resultsSink.addResult(IngredientComponent.ITEMSTACK, result);

                        // Insert the remaining items into the sink
                        for (ItemStack remainingItem : smithingRecipe.getRemainingItems(grid)) {
                            if (!remainingItem.isEmpty()) {
                                resultsSink.addResult(IngredientComponent.ITEMSTACK, remainingItem);
                            }
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

}
