package org.cyclops.integratedcrafting.core.crafting.processoverride;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.CraftingHelpers;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * A crafting process override for crafting tables.
 * @author rubensworks
 */
public class CraftingProcessOverrideCraftingTable implements ICraftingProcessOverride {

    private static GameProfile PROFILE = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BB57-13D2C99CAE77"), "[IntegratedCrafting]");
    private static final Map<ServerLevel, FakePlayer> FAKE_PLAYERS = new WeakHashMap<ServerLevel, FakePlayer>();

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
        return target.getPos().getLevel(true).getBlockState(target.getPos().getBlockPos()).getBlock() instanceof CraftingTableBlock;
    }

    @Override
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                         IMixedIngredients ingredients, ICraftingResultsSink resultsSink, boolean simulate) {
        PartPos target = targetGetter.apply(IngredientComponent.ITEMSTACK);
        CraftingGrid grid = new CraftingGrid(ingredients, 3, 3);

        return CraftingHelpers.findServerRecipe(RecipeType.CRAFTING, grid, target.getPos().getLevel(true))
                .or(() -> {
                    CraftingGrid gridSmall = new CraftingGrid(ingredients, 2, 2);
                    return CraftingHelpers.findServerRecipe(RecipeType.CRAFTING, gridSmall, target.getPos().getLevel(true));
                })
                .map(recipe -> {
                    ItemStack result = recipe.assemble(grid);

                    if (result.isEmpty()) {
                        return false;
                    }

                    if (!simulate) {
                        Player player = getFakePlayer((ServerLevel) target.getPos().getLevel(true));

                        // Fire all required events
                        result.onCraftedBy(target.getPos().getLevel(true), player, 1);
                        ForgeEventFactory.firePlayerCraftingEvent(player, result, grid);

                        // Insert the result into the sink
                        resultsSink.addResult(IngredientComponent.ITEMSTACK, result);

                        // Insert the remaining items into the sink
                        for (ItemStack remainingItem : recipe.getRemainingItems(grid)) {
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
