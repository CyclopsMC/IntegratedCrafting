package org.cyclops.integratedcrafting.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedOffsets;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedRecipes;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingAttuned;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    protected static int indexOfRecipeId(List<IRecipeDefinition> recipes, String recipeId) {
        for (int i = 0; i < recipes.size(); i++) {
            IRecipeDefinition recipe = recipes.get(i);
            if (recipe.getRecipeId() != null && recipe.getRecipeId().toString().equals(recipeId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Wrap a gui payload the way {@link ValueNotifierHelpers} does, so that it can be fed to a container.
     */
    protected static CompoundTag wrapValue(CompoundTag payload) {
        CompoundTag tag = new CompoundTag();
        tag.put(ValueNotifierHelpers.KEY, payload);
        return tag;
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
     * All recipes must survive the trip to the client through the gui data buffer,
     * together with which of them are disabled.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedRecipesReachTheGui(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);
        RecipeKey chestKey = RecipeKey.ofRecipeId(RECIPE_CHEST);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(partState.getCraftingNetwork() != null, "The interface has no crafting network");
                    helper.assertTrue(containsRecipeId(partState.getAllRecipes(), RECIPE_CHEST),
                            "The chest recipe is not read yet");
                })
                .thenExecute(() -> {
                    partState.setRecipesEnabled(Collections.singleton(chestKey), false);

                    ContainerPartInterfaceCraftingAttunedRecipes container = openGuiContainer(helper, positions);

                    helper.assertValueEqual(container.getUnfilteredItemCount(), partState.getAllRecipes().size(),
                            "Not all recipes reached the gui");

                    // The search matches recipe ids, so this isolates the chest recipe
                    container.updateFilter(RECIPE_CHEST);
                    IRecipeDefinition chestRecipe = null;
                    for (int i = 0; i < container.getPageSize() * container.getColumns(); i++) {
                        IRecipeDefinition recipe = container.getVisibleElement(i);
                        if (recipe != null && RECIPE_CHEST.equals(container.getEntry(recipe).identifier())) {
                            chestRecipe = recipe;
                        }
                    }
                    helper.assertTrue(chestRecipe != null, "The chest recipe was not found through the gui search");
                    helper.assertFalse(container.isRecipeEnabled(chestRecipe),
                            "The gui does not show the chest recipe as disabled");
                    helper.assertValueEqual(container.getEntry(chestRecipe).serverIndex(),
                            indexOfRecipeId(partState.getAllRecipes(), RECIPE_CHEST),
                            "The gui has the wrong server index for the chest recipe");

                    // The whole grid is filled when there are more recipes than fit on one page
                    container.updateFilter("");
                    int cells = container.getPageSize() * container.getColumns();
                    helper.assertTrue(container.getFilteredItemCount() > cells,
                            "The target does not expose enough recipes to fill the gui grid");
                    for (int i = 0; i < cells; i++) {
                        helper.assertTrue(container.getVisibleElement(i) != null,
                                "The gui grid has a hole at cell " + i);
                    }

                    // Recipes that do not match the search must not be shown
                    container.updateFilter("this recipe does not exist");
                    helper.assertValueEqual(container.getFilteredItemCount(), 0,
                            "The gui shows recipes that do not match the search");
                    helper.assertTrue(container.getVisibleElement(0) == null,
                            "The gui grid still shows a recipe that does not match the search");

                    // The part must also hand out the same container to a player opening it
                    ServerPlayer player = helper.makeMockServerPlayerInLevel();
                    MenuProvider menuProvider = PartTypes.INTERFACE_CRAFTING_ATTUNED
                            .getContainerProvider(positions.interfaces().get(0)).orElse(null);
                    helper.assertTrue(menuProvider != null, "The part has no gui");
                    helper.assertValueEqual(menuProvider.getDisplayName().getString(),
                            Component.translatable(PartTypes.INTERFACE_CRAFTING_ATTUNED.getTranslationKey()).getString(),
                            "The gui has the wrong title");
                    AbstractContainerMenu menu = menuProvider.createMenu(2, player.getInventory(), player);
                    helper.assertTrue(menu instanceof ContainerPartInterfaceCraftingAttunedRecipes,
                            "The part opened the wrong gui");
                    helper.assertValueEqual(((ContainerPartInterfaceCraftingAttunedRecipes) menu).getUnfilteredItemCount(),
                            partState.getAllRecipes().size(), "The opened gui has the wrong number of recipes");
                })
                .thenSucceed();
    }

    /**
     * The settings and offsets guis are opened from the recipes gui,
     * so this part exposes guis of its own that return to it when they are closed.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedSubGuisAreOwnedByThisMod(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(partState.getCraftingNetwork() != null, "The interface has no crafting network");
                })
                .thenExecute(() -> {
                    ServerPlayer player = helper.makeMockServerPlayerInLevel();
                    PartPos pos = positions.interfaces().get(0);

                    // The offsets gui must be the one of this mod,
                    // as the one of Integrated Dynamics closes all guis instead of returning to the part's gui.
                    MenuProvider offsetsProvider = PartTypes.INTERFACE_CRAFTING_ATTUNED
                            .getContainerProviderOffsets(pos).orElse(null);
                    helper.assertTrue(offsetsProvider != null, "The part has no offsets gui");
                    helper.assertTrue(offsetsProvider.createMenu(2, player.getInventory(), player)
                                    instanceof ContainerPartInterfaceCraftingAttunedOffsets,
                            "The part has the wrong offsets gui");

                    MenuProvider settingsProvider = PartTypes.INTERFACE_CRAFTING_ATTUNED
                            .getContainerProviderSettings(pos).orElse(null);
                    helper.assertTrue(settingsProvider != null, "The part has no settings gui");
                    helper.assertTrue(settingsProvider.createMenu(3, player.getInventory(), player)
                                    instanceof ContainerPartInterfaceCraftingSettings,
                            "The part has the wrong settings gui");
                })
                .thenSucceed();
    }

    /**
     * The gui toggles recipes by sending the recipe key, and applies bulk actions
     * by sending the server-side indexes of the recipes that match its search.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAttunedRecipesGuiActionsAreApplied(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions =
                createBasicNetwork(helper, POS, true);
        PartTypeInterfaceCraftingAttuned.State partState = positions.interfaceStates().get(0);
        RecipeKey chestKey = RecipeKey.ofRecipeId(RECIPE_CHEST);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(partState.getCraftingNetwork() != null, "The interface has no crafting network");
                    helper.assertTrue(containsRecipeId(partState.getAllRecipes(), RECIPE_CHEST),
                            "The chest recipe is not read yet");
                })
                .thenExecute(() -> {
                    ContainerPartInterfaceCraftingAttunedRecipes container = openGuiContainer(helper, positions);
                    int chestIndex = indexOfRecipeId(partState.getAllRecipes(), RECIPE_CHEST);

                    // Single toggle, keyed by recipe key
                    container.onUpdate(container.getToggleRecipeValueId(),
                            wrapValue(toggleTag(chestKey, false, 0)));
                    helper.assertFalse(partState.isRecipeEnabled(chestKey),
                            "The gui toggle did not disable the chest recipe");

                    // Repeating the same toggle after re-enabling it must not be swallowed,
                    // which is what the sequence number in the payload is for.
                    container.onUpdate(container.getToggleRecipeValueId(),
                            wrapValue(toggleTag(chestKey, true, 1)));
                    helper.assertTrue(partState.isRecipeEnabled(chestKey),
                            "The gui toggle did not enable the chest recipe");
                    container.onUpdate(container.getToggleRecipeValueId(),
                            wrapValue(toggleTag(chestKey, false, 2)));
                    helper.assertFalse(partState.isRecipeEnabled(chestKey),
                            "The repeated gui toggle was swallowed");

                    // Bulk actions
                    container.onUpdate(container.getBulkActionValueId(), wrapValue(bulkTag(
                            ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_ENABLE,
                            partState.getRecipesVersion(), new int[]{chestIndex}, 3)));
                    helper.assertTrue(partState.isRecipeEnabled(chestKey), "The bulk enable was not applied");

                    container.onUpdate(container.getBulkActionValueId(), wrapValue(bulkTag(
                            ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_INVERT,
                            partState.getRecipesVersion(), new int[]{chestIndex}, 4)));
                    helper.assertFalse(partState.isRecipeEnabled(chestKey), "The bulk invert was not applied");

                    // Indexes from a recipe list the server no longer has must be ignored
                    container.onUpdate(container.getBulkActionValueId(), wrapValue(bulkTag(
                            ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_ENABLE,
                            partState.getRecipesVersion() + 1, new int[]{chestIndex}, 5)));
                    helper.assertFalse(partState.isRecipeEnabled(chestKey),
                            "A bulk action with stale indexes was applied");

                    // Out-of-range indexes must be ignored instead of throwing
                    container.onUpdate(container.getBulkActionValueId(), wrapValue(bulkTag(
                            ContainerPartInterfaceCraftingAttunedRecipes.BULK_ACTION_ENABLE,
                            partState.getRecipesVersion(),
                            new int[]{-1, partState.getAllRecipes().size(), chestIndex}, 6)));
                    helper.assertTrue(partState.isRecipeEnabled(chestKey),
                            "The bulk enable with out-of-range indexes was not applied");
                })
                .thenSucceed();
    }

    /**
     * Open the part's gui container the way a player would, by writing its gui data
     * and constructing the container from it.
     */
    protected ContainerPartInterfaceCraftingAttunedRecipes openGuiContainer(
            GameTestHelper helper,
            GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCraftingAttuned.State> positions) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RegistryFriendlyByteBuf packetBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        PartTypes.INTERFACE_CRAFTING_ATTUNED.writeExtraGuiData(packetBuffer, positions.interfaces().get(0), player);
        return new ContainerPartInterfaceCraftingAttunedRecipes(1, player.getInventory(), packetBuffer);
    }

    protected static CompoundTag toggleTag(RecipeKey key, boolean enabled, int sequence) {
        CompoundTag tag = new CompoundTag();
        tag.put("key", key.serialize());
        tag.putBoolean("enabled", enabled);
        tag.putInt("seq", sequence);
        return tag;
    }

    protected static CompoundTag bulkTag(int action, int version, int[] indexes, int sequence) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("action", action);
        tag.putInt("version", version);
        tag.put("indexes", new IntArrayTag(indexes));
        tag.putInt("seq", sequence);
        return tag;
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
