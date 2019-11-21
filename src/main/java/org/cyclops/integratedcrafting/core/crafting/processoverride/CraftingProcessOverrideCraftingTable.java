package org.cyclops.integratedcrafting.core.crafting.processoverride;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.CraftingHelpers;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A crafting process override for crafting tables.
 * @author rubensworks
 */
public class CraftingProcessOverrideCraftingTable implements ICraftingProcessOverride {

    private static GameProfile PROFILE = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BB57-13D2C99CAE77"), "[IntegratedCrafting]");
    private static final Map<WorldServer, FakePlayer> FAKE_PLAYERS = new WeakHashMap<WorldServer, FakePlayer>();

    public static FakePlayer getFakePlayer(WorldServer world) {
        FakePlayer fakePlayer = FAKE_PLAYERS.get(world);
        if (fakePlayer == null) {
            fakePlayer = new FakePlayer(world, PROFILE);
            FAKE_PLAYERS.put(world, fakePlayer);
        }
        return fakePlayer;
    }

    @Override
    public boolean isApplicable(PartPos target) {
        return target.getPos().getWorld().getBlockState(target.getPos().getBlockPos()).getBlock() instanceof BlockWorkbench;
    }

    @Override
    public boolean craft(Function<IngredientComponent<?, ?>, PartPos> targetGetter,
                         IMixedIngredients ingredients, ICraftingResultsSink resultsSink, boolean simulate) {
        PartPos target = targetGetter.apply(IngredientComponent.ITEMSTACK);
        CraftingGrid grid = new CraftingGrid(ingredients, 3, 3);
        IRecipe recipe = CraftingHelpers.findMatchingRecipeCached(grid, target.getPos().getWorld(), true);
        if (recipe != null) {
            ItemStack result = recipe.getCraftingResult(grid);

            if (result.isEmpty()) {
                return false;
            }

            if (!simulate) {
                EntityPlayer player = getFakePlayer((WorldServer) target.getPos().getWorld());

                // Fire all required events
                result.onCrafting(target.getPos().getWorld(), player, 1);
                net.minecraftforge.fml.common.FMLCommonHandler.instance().firePlayerCraftingEvent(player, result, grid);

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
        }
        return false;
    }

}
