package org.cyclops.integratedcrafting.core.crafting.processoverride;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.IModHelpers;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import com.google.common.collect.MapMaker;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * A crafting process override for crafting tables.
 * @author rubensworks
 */
public class CraftingProcessOverrideCraftingTable implements ICraftingProcessOverride {

    private static GameProfile PROFILE = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BB57-13D2C99CAE77"), "[IntegratedCrafting]");
    // Weak keys AND values: the FakePlayer value holds a reference to its ServerLevel key,
    // so a WeakHashMap (weak keys only) would keep the key alive forever and leak.
    private static final Map<ServerLevel, FakePlayer> FAKE_PLAYERS = new MapMaker().weakKeys().weakValues().makeMap();

    public static FakePlayer getFakePlayer(ServerLevel world) {
        FakePlayer fakePlayer = FAKE_PLAYERS.get(world);
        if (fakePlayer == null) {
            fakePlayer = new FakePlayer(world, PROFILE);
            FAKE_PLAYERS.put(world, fakePlayer);
        }
        return fakePlayer;
    }

    @Override
    public boolean isApplicable(PartPos target) {
        return target.getPos().getLevel(true).getBlockState(target.getPos().getBlockPos()).is(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES);
    }

    @Override
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                         IMixedIngredients ingredients, IRecipeDefinition recipe, ICraftingResultsSink resultsSink, CraftingJob craftingJob, boolean simulate) {
        PartPos target = targetGetter.apply(IngredientComponent.ITEMSTACK);
        CraftingGrid gridFull = new CraftingGrid(ingredients, 3, 3);
        CraftingInput gridInput = gridFull.asCraftInput();
        Level level = target.getPos().getLevel(true);

        return IModHelpers.get().getCraftingHelpers().findRecipe(RecipeType.CRAFTING, gridInput, level)
                .or(() -> {
                    try {
                        CraftingGrid gridSmall = new CraftingGrid(ingredients, 2, 2);
                        return IModHelpers.get().getCraftingHelpers().findRecipe(RecipeType.CRAFTING, gridSmall.asCraftInput(), level);
                    } catch (IllegalArgumentException e) {
                        // This can occur if the ingredients don't fit in a 2x2 grid.
                        return Optional.empty();
                    }
                })
                .map(recipeHolder -> {
                    CraftingRecipe craftingRecipe = recipeHolder.value();
                    ItemStack result = craftingRecipe.assemble(gridInput);

                    if (result.isEmpty()) {
                        return false;
                    }

                    if (!simulate) {
                        Player player = getFakePlayer((ServerLevel) target.getPos().getLevel(true));

                        // Fire all required events
                        result.onCraftedBy(player, 1);
                        EventHooks.firePlayerCraftingEvent(player, result, gridFull);

                        // Insert the result into the sink
                        resultsSink.addResult(IngredientComponent.ITEMSTACK, result);

                        // Insert the remaining items into the sink
                        for (ItemStack remainingItem : craftingRecipe.getRemainingItems(gridInput)) {
                            if (!remainingItem.isEmpty()) {
                                craftingJob.addToIngredientsStorageBuffer(IngredientComponent.ITEMSTACK, remainingItem);
                            }
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

}
