package org.cyclops.integratedcrafting.core.crafting.processoverride;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StonecutterBlock;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.CraftingHelpers;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import java.util.List;
import java.util.function.Function;

/**
 * A crafting process override for stone cutters.
 * @author rubensworks
 */
public class CraftingProcessOverrideStonecutter implements ICraftingProcessOverride {

    @Override
    public boolean isApplicable(PartPos target) {
        return target.getPos().getLevel(true).getBlockState(target.getPos().getBlockPos()).getBlock() instanceof StonecutterBlock;
    }

    @Override
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                         IMixedIngredients ingredients, IRecipeDefinition recipe, ICraftingResultsSink resultsSink, boolean simulate) {
        // Prepare input
        PartPos target = targetGetter.apply(IngredientComponent.ITEMSTACK);
        CraftingGrid grid = new CraftingGrid(ingredients, 1, 1);
        Level level = target.getPos().getLevel(true);

        // Get expected output
        IMixedIngredients output = recipe.getOutput();
        List<ItemStack> recipeOutputs = output.getInstances(IngredientComponent.ITEMSTACK);
        if (output.getComponents().size() != 1 || recipeOutputs.size() != 1) {
            throw new IllegalArgumentException("Can only craft for an output of 1 item, while got" + (recipeOutputs == null ? "null" : recipeOutputs.size()));
        }
        ItemStack recipeOutput = recipeOutputs.get(0);

        // Find recipe with input AND output
        return CraftingHelpers.findRecipes(level, RecipeType.STONECUTTING)
                .stream()
                .filter(recipeHolder ->
                        recipeHolder.matches(grid, level) &&
                                ItemStack.isSameItemSameTags(recipeHolder.getResultItem(level.registryAccess()), recipeOutput))
                .findFirst()
                .map(stonecutterRecipe -> {
                    ItemStack result = stonecutterRecipe.assemble(grid, level.registryAccess());

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
                        for (ItemStack remainingItem : stonecutterRecipe.getRemainingItems(grid)) {
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
