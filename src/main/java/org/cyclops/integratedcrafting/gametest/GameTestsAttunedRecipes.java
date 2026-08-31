package org.cyclops.integratedcrafting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.RecursiveCraftingRecipeException;
import org.cyclops.integratedcrafting.api.crafting.UnknownCraftingRecipeException;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.RecipeKey;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingAttuned;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import java.util.Collection;
import java.util.Collections;

import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.createBasicNetwork;
import static org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting.enableRecipeInWriter;

/**
 * Game tests for enabling and disabling individual recipes of an attuned crafting interface.
 *
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsAttunedRecipes {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    public static final String RECIPE_CHEST = "minecraft:chest";

    protected static boolean containsRecipeId(Collection<IRecipeDefinition> recipes, String recipeId) {
        return recipes.stream()
                .anyMatch(recipe -> recipe.getRecipeId() != null && recipe.getRecipeId().toString().equals(recipeId));
    }

    protected static INetwork getNetwork(GameTestHelper helper, PartPos partPos) {
        return NetworkHelpers.getNetworkChecked(partPos.getPos().getLevel(true),
                partPos.getPos().getBlockPos(), partPos.getSide());
    }

    /**
     * Disabling a recipe must remove it from the crafting network's recipe index,
     * and enabling it again must put it back.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedDisabledRecipeIsNotIndexed(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);
        RecipeKey chestKey = RecipeKey.ofRecipeId(RECIPE_CHEST);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    ICraftingNetwork craftingNetwork = partState.getCraftingNetwork();
                    helper.assertTrue(craftingNetwork != null, "The interface has no crafting network");
                    helper.assertTrue(containsRecipeId(craftingNetwork.getRecipeIndex(0).getRecipes(), RECIPE_CHEST),
                            "The chest recipe is not indexed yet");
                })
                .thenExecute(() -> {
                    ICraftingNetwork craftingNetwork = partState.getCraftingNetwork();
                    int indexedBefore = craftingNetwork.getRecipeIndex(0).getRecipes().size();
                    int exposedBefore = partState.getRecipes().size();
                    helper.assertValueEqual(partState.getAllRecipes().size(), exposedBefore,
                            "Not all recipes are exposed initially");

                    partState.setRecipesEnabled(Collections.singleton(chestKey), false);

                    helper.assertFalse(partState.isRecipeEnabled(chestKey), "The chest recipe is still enabled");
                    helper.assertValueEqual(partState.getRecipes().size(), exposedBefore - 1,
                            "The interface still exposes the disabled recipe");
                    helper.assertFalse(containsRecipeId(partState.getRecipes(), RECIPE_CHEST),
                            "The interface still exposes the chest recipe");
                    helper.assertValueEqual(craftingNetwork.getRecipeIndex(0).getRecipes().size(), indexedBefore - 1,
                            "The network recipe index did not shrink");
                    helper.assertFalse(containsRecipeId(craftingNetwork.getRecipeIndex(0).getRecipes(), RECIPE_CHEST),
                            "The chest recipe is still indexed after disabling it");

                    // All recipes stay listed for the gui, only the exposed ones shrink
                    helper.assertTrue(containsRecipeId(partState.getAllRecipes(), RECIPE_CHEST),
                            "The chest recipe disappeared from the full recipe list");

                    partState.setRecipesEnabled(Collections.singleton(chestKey), true);

                    helper.assertTrue(partState.isRecipeEnabled(chestKey), "The chest recipe is still disabled");
                    helper.assertValueEqual(partState.getRecipes().size(), exposedBefore,
                            "The interface did not expose the re-enabled recipe");
                    helper.assertValueEqual(craftingNetwork.getRecipeIndex(0).getRecipes().size(), indexedBefore,
                            "The network recipe index did not grow back");
                    helper.assertTrue(containsRecipeId(craftingNetwork.getRecipeIndex(0).getRecipes(), RECIPE_CHEST),
                            "The chest recipe was not indexed again after enabling it");
                })
                .thenSucceed();
    }

    /**
     * The crafting job planner must not select a disabled recipe.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedDisabledRecipeIsNotPlanned(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);
        RecipeKey chestKey = RecipeKey.ofRecipeId(RECIPE_CHEST);

        // Insert items in interface chest, so that the planner has ingredients to work with
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(partState.getCraftingNetwork() != null, "The interface has no crafting network");
                    helper.assertTrue(containsRecipeId(partState.getCraftingNetwork().getRecipeIndex(0).getRecipes(), RECIPE_CHEST),
                            "The chest recipe is not indexed yet");
                })
                .thenExecute(() -> {
                    INetwork network = getNetwork(helper, positions.interfaces().get(0));

                    helper.assertTrue(planChest(network) != null, "The planner did not find the chest recipe");

                    partState.setRecipesEnabled(Collections.singleton(chestKey), false);
                    helper.assertTrue(planChest(network) == null, "The planner still selected the disabled chest recipe");

                    partState.setRecipesEnabled(Collections.singleton(chestKey), true);
                    helper.assertTrue(planChest(network) != null, "The planner did not find the re-enabled chest recipe");
                })
                .thenSucceed();
    }

    protected CraftingJob planChest(INetwork network) {
        try {
            return CraftingHelpers.calculateCraftingJobs(network, 0, IngredientComponents.ITEMSTACK,
                    new ItemStack(Items.CHEST), ItemMatch.ITEM, true,
                    CraftingHelpers.getGlobalCraftingJobIdentifier(), new CraftingJobDependencyGraph(), false);
        } catch (UnknownCraftingRecipeException | RecursiveCraftingRecipeException e) {
            return null;
        }
    }

    /**
     * The crafting network unregisters an interface by iterating over the recipes it exposes,
     * so the exposed recipes must never change without the network being told about it.
     * Otherwise, disabled recipes would leak into the network's recipe index forever.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedDisabledRecipeDoesNotLeakOnUnregister(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);
        RecipeKey chestKey = RecipeKey.ofRecipeId(RECIPE_CHEST);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    ICraftingNetwork craftingNetwork = partState.getCraftingNetwork();
                    helper.assertTrue(craftingNetwork != null, "The interface has no crafting network");
                    helper.assertTrue(containsRecipeId(craftingNetwork.getRecipeIndex(0).getRecipes(), RECIPE_CHEST),
                            "The chest recipe is not indexed yet");
                })
                .thenExecute(() -> {
                    ICraftingNetwork craftingNetwork = partState.getCraftingNetwork();

                    partState.setRecipesEnabled(Collections.singleton(chestKey), false);

                    // Moving the interface to another channel unregisters and re-registers it,
                    // which is the code path that iterates over the exposed recipes.
                    partState.setChannelCrafting(1);

                    // The channel-independent index is never cleaned up per channel,
                    // so it is where recipes would leak into.
                    Collection<IRecipeDefinition> allIndexed = craftingNetwork
                            .getRecipeIndex(IPositionedAddonsNetwork.WILDCARD_CHANNEL).getRecipes();
                    helper.assertFalse(containsRecipeId(allIndexed, RECIPE_CHEST),
                            "The disabled chest recipe leaked into the network recipe index");
                    helper.assertValueEqual(allIndexed.size(), partState.getRecipes().size(),
                            "The network recipe index does not match the exposed recipes");
                    helper.assertTrue(containsRecipeId(craftingNetwork.getRecipeIndex(1).getRecipes(), "minecraft:stick"),
                            "The interface did not re-register its recipes on the new channel");
                    helper.assertFalse(containsRecipeId(craftingNetwork.getRecipeIndex(1).getRecipes(), RECIPE_CHEST),
                            "The disabled chest recipe was registered on the new channel");
                })
                .thenSucceed();
    }

    /**
     * Disabled recipes must survive a save and load of the part state.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedDisabledRecipesArePersisted(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);
        RecipeKey chestKey = RecipeKey.ofRecipeId(RECIPE_CHEST);
        RecipeKey removedKey = RecipeKey.ofRecipeId("somemod:removed_recipe");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(partState.getCraftingNetwork() != null,
                        "The interface has no crafting network"))
                .thenExecute(() -> {
                    ValueDeseralizationContext context = ValueDeseralizationContext.of(helper.getLevel());
                    partState.setRecipesEnabled(java.util.List.of(chestKey, removedKey), false);

                    CompoundTag tag = new CompoundTag();
                    partState.writeToNBT(context, tag);

                    PartTypeInterfaceCraftingAttuned.State loadedState = new PartTypeInterfaceCraftingAttuned.State();
                    loadedState.readFromNBT(context, tag);

                    helper.assertFalse(loadedState.isRecipeEnabled(chestKey),
                            "The disabled chest recipe was not persisted");
                    // Keys of recipes that no longer exist must stay opaque,
                    // so that a pack update can not silently re-enable them.
                    helper.assertFalse(loadedState.isRecipeEnabled(removedKey),
                            "The key of a recipe that no longer exists was not persisted");
                    helper.assertValueEqual(loadedState.getDisabledRecipes().size(), 2,
                            "The wrong number of disabled recipes was persisted");
                })
                .thenSucceed();
    }

    /**
     * A disabled recipe must not be crafted, and must be craftable again after enabling it.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedDisabledRecipeIsNotCrafted(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);
        RecipeKey chestKey = RecipeKey.ofRecipeId(RECIPE_CHEST);

        // Insert items in interface chest
        ChestBlockEntity chestIn = helper.getBlockEntity(POS.east());
        chestIn.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        helper.startSequence()
                .thenWaitUntil(() -> {
                    ICraftingNetwork craftingNetwork = partState.getCraftingNetwork();
                    helper.assertTrue(craftingNetwork != null, "The interface has no crafting network");
                    helper.assertTrue(containsRecipeId(craftingNetwork.getRecipeIndex(0).getRecipes(), RECIPE_CHEST),
                            "The chest recipe is not indexed yet");
                })
                .thenExecute(() -> {
                    partState.setRecipesEnabled(Collections.singleton(chestKey), false);
                    enableRecipeInWriter(helper, positions.writer(), new ItemStack(Items.CHEST));
                })
                .thenExecuteAfter(200, () -> {
                    helper.assertValueEqual(chestIn.getItem(0).getItem(), Items.OAK_PLANKS, "Slot 0 item is incorrect");
                    helper.assertValueEqual(chestIn.getItem(0).getCount(), 64,
                            "Planks were consumed while the chest recipe was disabled");
                    helper.assertTrue(chestIn.getItem(1).isEmpty(), "A chest was crafted while its recipe was disabled");

                    partState.setRecipesEnabled(Collections.singleton(chestKey), true);
                })
                .thenWaitUntil(() -> {
                    helper.assertValueEqual(chestIn.getItem(1).getItem(), Items.CHEST, "Slot 1 item is incorrect");
                    helper.assertValueEqual(chestIn.getItem(1).getCount(), 1, "Slot 1 amount is incorrect");
                })
                .thenSucceed();
    }

}
