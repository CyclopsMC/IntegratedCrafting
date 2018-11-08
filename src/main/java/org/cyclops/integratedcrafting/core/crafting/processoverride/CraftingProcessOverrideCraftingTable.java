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
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integrateddynamics.api.part.PartPos;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A crafting process override for crafting tables.
 * @author rubensworks
 */
public class CraftingProcessOverrideCraftingTable implements ICraftingProcessOverride {

    private static final LoadingCache<Pair<CraftingGrid, Integer>, IRecipe> CACHE_RECIPES = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader<Pair<CraftingGrid, Integer>, IRecipe>() {
                @Override
                public IRecipe load(Pair<CraftingGrid, Integer> key) {
                    IRecipe recipe = CraftingManager.findMatchingRecipe(key.getLeft(), DimensionManager.getWorld(key.getRight()));
                    if (recipe == null) {
                        recipe = NULL_RECIPE;
                    }
                    return recipe;
                }
            });
    // A dummy recipe that represents null, because guava's cache doesn't allow null entries.
    private static final IRecipe NULL_RECIPE = new IRecipe() {
        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {
            return false;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            return null;
        }

        @Override
        public boolean canFit(int width, int height) {
            return false;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }

        @Override
        public IRecipe setRegistryName(ResourceLocation name) {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getRegistryName() {
            return null;
        }

        @Override
        public Class<IRecipe> getRegistryType() {
            return null;
        }
    };

    private static GameProfile PROFILE = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BB57-13D2C99CAE77"), "[IntegratedCrafting]");
    private static final Map<WorldServer, FakePlayer> FAKE_PLAYERS = new WeakHashMap<WorldServer, FakePlayer>();

    @Nullable
    public static IRecipe getRecipe(CraftingGrid grid, World world) {
        try {
            IRecipe recipe = CACHE_RECIPES.get(Pair.of(grid, world.provider.getDimension()));
            if (recipe == NULL_RECIPE) {
                recipe = null;
            }
            return recipe;
        } catch (ExecutionException | UncheckedExecutionException e) {
            return null;
        }
    }

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
    public boolean craft(PartPos target, IMixedIngredients ingredients, ICraftingResultsSink resultsSink, boolean simulate) {
        CraftingGrid grid = new CraftingGrid(ingredients, 3, 3);
        IRecipe recipe = getRecipe(grid, target.getPos().getWorld());
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
                    if (remainingItem.isEmpty()) {
                        resultsSink.addResult(IngredientComponent.ITEMSTACK, remainingItem);
                    }
                }
            }
            return true;
        }
        return false;
    }

}
