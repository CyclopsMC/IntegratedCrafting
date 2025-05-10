package org.cyclops.integratedcrafting.core.crafting.processoverride;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.StonecutterBlock;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.IMinecraftHelpers;
import org.cyclops.cyclopscore.helper.IModHelpers;
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
        List<ItemStack> itemStacks = ingredients.getInstances(IngredientComponent.ITEMSTACK);
        if (itemStacks == null || itemStacks.size() != 1) {
            throw new IllegalArgumentException("Can only craft in a grid with 1 item, while got" + (itemStacks == null ? "null" : itemStacks.size()));
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(itemStacks.get(0));
        ServerLevel level = (ServerLevel) target.getPos().getLevel(true);

        // Get expected output
        IMixedIngredients output = recipe.getOutput();
        List<ItemStack> recipeOutputs = output.getInstances(IngredientComponent.ITEMSTACK);
        if (output.getComponents().size() != 1 || recipeOutputs.size() != 1) {
            throw new IllegalArgumentException("Can only craft for an output of 1 item, while got" + (recipeOutputs == null ? "null" : recipeOutputs.size()));
        }
        ItemStack recipeOutput = recipeOutputs.getFirst();

        // Find recipe with input AND output
        IMinecraftHelpers mcHelpers = IModHelpers.get().getMinecraftHelpers();
        return IModHelpers.get().getCraftingHelpers().findRecipes(level, RecipeType.STONECUTTING)
                .stream()
                .filter(recipeHolder ->
                        recipeHolder.value().matches(recipeInput, level) &&
                                ItemStack.isSameItemSameComponents(mcHelpers.getRecipeOutput(recipeHolder, level), recipeOutput))
                .findFirst()
                .map(recipeHolder -> {
                    StonecutterRecipe stonecutterRecipe = recipeHolder.value();
                    ItemStack result = stonecutterRecipe.assemble(recipeInput, level.registryAccess());

                    if (result.isEmpty()) {
                        return false;
                    }

                    if (!simulate) {
                        Player player = CraftingProcessOverrideCraftingTable.getFakePlayer((ServerLevel) target.getPos().getLevel(true));

                        // Fire all required events
                        result.onCraftedBy(target.getPos().getLevel(true), player, 1);

                        // Insert the result into the sink
                        resultsSink.addResult(IngredientComponent.ITEMSTACK, result);
                    }
                    return true;
                })
                .orElse(false);
    }

}
