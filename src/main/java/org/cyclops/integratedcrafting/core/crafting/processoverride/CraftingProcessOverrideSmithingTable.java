package org.cyclops.integratedcrafting.core.crafting.processoverride;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SmithingTableBlock;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.IModHelpers;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import java.util.List;
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
        List<ItemStack> itemStacks = ingredients.getInstances(IngredientComponent.ITEMSTACK);
        if (itemStacks == null || itemStacks.size() != 3) {
            throw new IllegalArgumentException("Can only smith in a grid with 3 items, while got" + (itemStacks == null ? "null" : itemStacks.size()));
        }
        SmithingRecipeInput smithingRecipeInput = new SmithingRecipeInput(itemStacks.get(0), itemStacks.get(1), itemStacks.get(2));
        Level level = target.getPos().getLevel(true);

        return IModHelpers.get().getCraftingHelpers().findRecipe(RecipeType.SMITHING, smithingRecipeInput, level)
                .map(recipeHolder -> {
                    SmithingRecipe smithingRecipe = recipeHolder.value();
                    ItemStack result = smithingRecipe.assemble(smithingRecipeInput, level.registryAccess());

                    if (result.isEmpty()) {
                        return false;
                    }

                    if (!simulate) {
                        Player player = CraftingProcessOverrideCraftingTable.getFakePlayer((ServerLevel) target.getPos().getLevel(true));

                        // Fire all required events
                        result.onCraftedBy(player, 1);

                        // Insert the result into the sink
                        resultsSink.addResult(IngredientComponent.ITEMSTACK, result);
                    }
                    return true;
                })
                .orElse(false);
    }

}
