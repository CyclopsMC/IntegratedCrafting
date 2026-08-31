package org.cyclops.integratedcrafting.inventory.container;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.cyclopscore.inventory.container.ScrollingInventoryContainer;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.RegistryEntries;
import org.cyclops.integratedcrafting.api.recipe.RecipeKey;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCraftingAttuned;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Container that lists all recipes of an attuned crafting interface,
 * and that allows the player to enable or disable each of them.
 *
 * Recipes are identified by their {@link RecipeKey}, and never by their position in this list,
 * because the client sorts its copy of the list by display name,
 * and because the server may re-read its recipes while this gui is open.
 *
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingAttunedRecipes extends ScrollingInventoryContainer<IRecipeDefinition> {

    public static final String BUTTON_SETTINGS = "button_settings";
    public static final String BUTTON_OFFSETS = "button_offsets";

    public static final int PAGE_SIZE = 5;

    public static final int BULK_ACTION_ENABLE = 0;
    public static final int BULK_ACTION_DISABLE = 1;
    public static final int BULK_ACTION_INVERT = 2;

    private final PartTarget target;
    private final IPartContainer partContainer;
    private final IPartType partType;

    private final Map<IRecipeDefinition, RecipeEntry> entries;
    private final Set<RecipeKey> disabledRecipes;
    private final int recipesVersion;

    private final int toggleRecipeValueId;
    private final int bulkActionValueId;

    private int sequence = 0;

    public ContainerPartInterfaceCraftingAttunedRecipes(int id, Inventory playerInventory, RegistryFriendlyByteBuf packetBuffer) {
        this(id, playerInventory, new SimpleContainer(0),
                PartHelpers.readPartTarget(packetBuffer), Optional.empty(), PartHelpers.readPart(packetBuffer),
                readRecipes(packetBuffer));
    }

    public ContainerPartInterfaceCraftingAttunedRecipes(int id, Inventory playerInventory, Container inventory,
                                                        PartTarget target, Optional<IPartContainer> partContainer,
                                                        IPartType partType, List<IRecipeDefinition> recipes,
                                                        Set<RecipeKey> disabledRecipes, int recipesVersion) {
        this(id, playerInventory, inventory, target, partContainer, partType,
                new GuiRecipes(createServerEntries(recipes), disabledRecipes, recipesVersion));
    }

    private ContainerPartInterfaceCraftingAttunedRecipes(int id, Inventory playerInventory, Container inventory,
                                                         PartTarget target, Optional<IPartContainer> partContainer,
                                                         IPartType partType, GuiRecipes guiRecipes) {
        super(RegistryEntries.CONTAINER_INTERFACE_CRAFTING_ATTUNED_RECIPES.get(), id, playerInventory, inventory,
                guiRecipes.getRecipes(), (recipe, pattern) -> {
                    RecipeEntry entry = guiRecipes.getEntries().get(recipe);
                    return entry == null || pattern.matcher(entry.searchString()).matches();
                });
        this.target = target;
        this.partContainer = partContainer.orElseGet(() -> PartHelpers.getPartContainerChecked(target.getCenter()));
        this.partType = partType;

        this.entries = guiRecipes.getEntries();
        this.disabledRecipes = Sets.newHashSet(guiRecipes.getDisabledRecipes());
        this.recipesVersion = guiRecipes.getRecipesVersion();

        this.toggleRecipeValueId = getNextValueId();
        this.bulkActionValueId = getNextValueId();

        addPlayerInventory(player.getInventory(), 9, 131);

        putButtonAction(BUTTON_SETTINGS, (s, containerExtended) -> {
            if (!player.level().isClientSide()) {
                PartHelpers.openContainerPartSettings((ServerPlayer) player, getTarget().getCenter(), getPartType());
            }
        });
        putButtonAction(BUTTON_OFFSETS, (s, containerExtended) -> {
            if (!player.level().isClientSide()) {
                PartHelpers.openContainerPartOffsets((ServerPlayer) player, getTarget().getCenter(), getPartType());
            }
        });
    }

    public IPartType getPartType() {
        return this.partType;
    }

    public PartTarget getTarget() {
        return this.target;
    }

    public PartTypeInterfaceCraftingAttuned.State getPartState() {
        return (PartTypeInterfaceCraftingAttuned.State) this.partContainer.getPartState(getTarget().getCenter().getSide());
    }

    @Override
    public int getPageSize() {
        return PAGE_SIZE;
    }

    @Override
    protected int getSizeInventory() {
        return 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return PartHelpers.canInteractWith(getTarget(), player, this.partContainer);
    }

    /**
     * @param recipe One of the recipes in this container.
     * @return The gui data of the given recipe, or null if it is unknown.
     */
    @Nullable
    public RecipeEntry getEntry(IRecipeDefinition recipe) {
        return this.entries.get(recipe);
    }

    /**
     * @param recipe One of the recipes in this container.
     * @return If the given recipe is exposed to the crafting network.
     */
    public boolean isRecipeEnabled(IRecipeDefinition recipe) {
        RecipeEntry entry = getEntry(recipe);
        return entry == null || !this.disabledRecipes.contains(entry.key());
    }

    /**
     * Enable or disable a single recipe.
     *
     * The change is applied locally right away, and sent to the server keyed by recipe key.
     *
     * @param recipe One of the recipes in this container.
     * @param enabled If the recipe should be exposed to the crafting network.
     */
    public void setRecipeEnabled(IRecipeDefinition recipe, boolean enabled) {
        RecipeEntry entry = getEntry(recipe);
        if (entry == null) {
            return;
        }
        setRecipeEnabledLocally(entry, enabled);

        CompoundTag tag = new CompoundTag();
        tag.put("key", entry.key().serialize());
        tag.putBoolean("enabled", enabled);
        // The value notifier drops values that are equal to the previously sent one,
        // so a monotonic sequence number is needed to make repeated identical toggles arrive.
        tag.putInt("seq", this.sequence++);
        ValueNotifierHelpers.setValue(this, this.toggleRecipeValueId, tag);
    }

    /**
     * Apply a bulk action to all recipes that match the current search filter.
     *
     * The affected recipes are sent as indexes in the server's recipe list,
     * as sending thousands of recipe keys would be needlessly large.
     * The server ignores the action if it re-read its recipes in the meantime,
     * which is detected via the recipe list version.
     *
     * @param action One of {@link #BULK_ACTION_ENABLE}, {@link #BULK_ACTION_DISABLE} and {@link #BULK_ACTION_INVERT}.
     */
    public void applyBulkAction(int action) {
        List<Pair<Integer, IRecipeDefinition>> filteredItems = getFilteredItems();
        int[] indexes = new int[filteredItems.size()];
        int i = 0;
        for (Pair<Integer, IRecipeDefinition> filteredItem : filteredItems) {
            RecipeEntry entry = getEntry(filteredItem.getRight());
            if (entry == null) {
                continue;
            }
            indexes[i++] = entry.serverIndex();
            setRecipeEnabledLocally(entry, isEnabledAfterBulkAction(action, this.disabledRecipes.contains(entry.key())));
        }
        if (i < indexes.length) {
            indexes = Arrays.copyOf(indexes, i);
        }

        CompoundTag tag = new CompoundTag();
        tag.putInt("action", action);
        tag.putInt("version", this.recipesVersion);
        tag.put("indexes", new IntArrayTag(indexes));
        // See setRecipeEnabled: the value notifier drops repeated identical values.
        tag.putInt("seq", this.sequence++);
        ValueNotifierHelpers.setValue(this, this.bulkActionValueId, tag);
    }

    protected void setRecipeEnabledLocally(RecipeEntry entry, boolean enabled) {
        if (enabled) {
            this.disabledRecipes.remove(entry.key());
        } else {
            this.disabledRecipes.add(entry.key());
        }
    }

    protected static boolean isEnabledAfterBulkAction(int action, boolean wasDisabled) {
        return switch (action) {
            case BULK_ACTION_ENABLE -> true;
            case BULK_ACTION_DISABLE -> false;
            default -> wasDisabled;
        };
    }

    @Override
    public void onUpdate(int valueId, CompoundTag value) {
        super.onUpdate(valueId, value);
        if (player.level().isClientSide()) {
            return;
        }
        try {
            Tag rawValue = ValueNotifierHelpers.getValueNbt(this, valueId);
            if (!(rawValue instanceof CompoundTag payload)) {
                return;
            }
            if (valueId == this.toggleRecipeValueId) {
                getPartState().setRecipesEnabled(
                        Collections.singleton(RecipeKey.deserialize(payload.getCompound("key"))),
                        payload.getBoolean("enabled"));
            } else if (valueId == this.bulkActionValueId) {
                applyBulkActionServer(payload);
            }
        } catch (RuntimeException e) {
            IntegratedCrafting.clog(Level.WARN,
                    "Could not apply a recipe change to an attuned crafting interface: " + e.getMessage());
        }
    }

    protected void applyBulkActionServer(CompoundTag payload) {
        PartTypeInterfaceCraftingAttuned.State partState = getPartState();
        if (payload.getInt("version") != partState.getRecipesVersion()) {
            // The recipes were re-read since the client built its list, so its indexes are stale.
            return;
        }

        int action = payload.getInt("action");
        List<IRecipeDefinition> allRecipes = partState.getAllRecipes();
        List<RecipeKey> toEnable = Lists.newArrayList();
        List<RecipeKey> toDisable = Lists.newArrayList();
        for (int index : payload.getIntArray("indexes")) {
            if (index < 0 || index >= allRecipes.size()) {
                continue;
            }
            RecipeKey key = partState.getRecipeKey(allRecipes.get(index));
            if (key == null) {
                continue;
            }
            (isEnabledAfterBulkAction(action, !partState.isRecipeEnabled(key)) ? toEnable : toDisable).add(key);
        }
        partState.setRecipesEnabled(toEnable, true);
        partState.setRecipesEnabled(toDisable, false);
    }

    /**
     * Write all recipes and disabled recipe keys of the given part state to the gui data buffer.
     * @param packetBuffer A packet buffer.
     * @param partState An attuned crafting interface part state.
     */
    public static void writeRecipes(RegistryFriendlyByteBuf packetBuffer, PartTypeInterfaceCraftingAttuned.State partState) {
        HolderLookup.Provider lookupProvider = packetBuffer.registryAccess();

        packetBuffer.writeVarInt(partState.getRecipesVersion());

        List<IRecipeDefinition> recipes = partState.getAllRecipes();
        packetBuffer.writeVarInt(recipes.size());
        for (IRecipeDefinition recipe : recipes) {
            ResourceLocation recipeId = recipe.getRecipeId();
            if (recipeId != null) {
                // Built-in recipes are sent by id only, the client resolves them via its own recipe manager.
                packetBuffer.writeBoolean(true);
                packetBuffer.writeResourceLocation(recipeId);
            } else {
                packetBuffer.writeBoolean(false);
                packetBuffer.writeNbt(IRecipeDefinition.serialize(lookupProvider, recipe));
            }
        }

        Set<RecipeKey> disabledRecipes = partState.getDisabledRecipes();
        packetBuffer.writeVarInt(disabledRecipes.size());
        for (RecipeKey disabledRecipe : disabledRecipes) {
            String recipeId = disabledRecipe.getRecipeId();
            if (recipeId != null) {
                packetBuffer.writeBoolean(true);
                packetBuffer.writeUtf(recipeId);
            } else {
                packetBuffer.writeBoolean(false);
                packetBuffer.writeNbt(disabledRecipe.serialize());
            }
        }
    }

    protected static GuiRecipes readRecipes(RegistryFriendlyByteBuf packetBuffer) {
        HolderLookup.Provider lookupProvider = packetBuffer.registryAccess();

        int recipesVersion = packetBuffer.readVarInt();

        int recipeCount = packetBuffer.readVarInt();
        List<RecipeEntry> entryList = Lists.newArrayListWithCapacity(recipeCount);
        for (int i = 0; i < recipeCount; i++) {
            IRecipeDefinition recipe = null;
            RecipeKey key;
            if (packetBuffer.readBoolean()) {
                ResourceLocation recipeId = packetBuffer.readResourceLocation();
                key = RecipeKey.ofRecipeId(recipeId.toString());
                try {
                    recipe = RecipeDefinition.fromRecipeId(lookupProvider, recipeId);
                } catch (RuntimeException e) {
                    // The client does not know this recipe, so it can not be shown.
                }
            } else {
                CompoundTag tag = packetBuffer.readNbt();
                key = RecipeKey.deserialize(tag);
                try {
                    recipe = IRecipeDefinition.deserialize(lookupProvider, tag);
                } catch (RuntimeException e) {
                    // The recipe could not be reconstructed, so it can not be shown.
                }
            }
            if (recipe != null) {
                entryList.add(RecipeEntry.of(i, key, recipe));
            }
        }

        int disabledCount = packetBuffer.readVarInt();
        Set<RecipeKey> disabledRecipes = Sets.newHashSetWithExpectedSize(disabledCount);
        for (int i = 0; i < disabledCount; i++) {
            if (packetBuffer.readBoolean()) {
                disabledRecipes.add(RecipeKey.ofRecipeId(packetBuffer.readUtf()));
            } else {
                disabledRecipes.add(RecipeKey.deserialize(packetBuffer.readNbt()));
            }
        }

        // Sorting happens client-side, so that the shown order follows the client's language.
        // The server keeps its recipes in the order in which the target exposes them.
        entryList.sort(RecipeEntry.COMPARATOR);

        return new GuiRecipes(entryList, disabledRecipes, recipesVersion);
    }

    protected static List<RecipeEntry> createServerEntries(List<IRecipeDefinition> recipes) {
        List<RecipeEntry> entryList = Lists.newArrayListWithCapacity(recipes.size());
        int i = 0;
        for (IRecipeDefinition recipe : recipes) {
            entryList.add(RecipeEntry.ofServer(i++, recipe));
        }
        return entryList;
    }

    /**
     * @param recipe A recipe.
     * @return The first item output of the given recipe, or an empty stack if it has none.
     */
    public static ItemStack getOutputItem(IRecipeDefinition recipe) {
        List<ItemStack> outputs = recipe.getOutput().getInstances(IngredientComponent.ITEMSTACK);
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
    }

    /**
     * The gui-side data of a single recipe.
     *
     * @param serverIndex The index of this recipe in the server's recipe list.
     * @param key The key that identifies this recipe.
     * @param recipe The recipe.
     * @param displayName The name that is shown for this recipe.
     * @param sortName The lowercased name that this entry is sorted by.
     * @param identifier A stable identifier that breaks sorting ties.
     * @param searchString The lowercased string that the search field matches against.
     */
    public static record RecipeEntry(int serverIndex, RecipeKey key, IRecipeDefinition recipe,
                                     Component displayName, String sortName, String identifier, String searchString) {

        /**
         * Sorts entries by their output name, and breaks ties on their identifier,
         * so that recipes that share an output name keep a stable order across sessions.
         */
        public static final Comparator<RecipeEntry> COMPARATOR = (a, b) -> {
            int comparison = a.sortName().compareTo(b.sortName());
            return comparison != 0 ? comparison : a.identifier().compareTo(b.identifier());
        };

        public static RecipeEntry of(int serverIndex, RecipeKey key, IRecipeDefinition recipe) {
            String identifier = key.getRecipeId() != null ? key.getRecipeId() : key.serialize().toString();
            ItemStack outputItem = getOutputItem(recipe);
            Component displayName = outputItem.isEmpty()
                    ? Component.literal(identifier) : outputItem.getHoverName();
            String name = displayName.getString();

            // The search strings are precomputed once,
            // as localizing thousands of recipes on every keystroke would be far too slow.
            StringBuilder searchString = new StringBuilder(name).append(' ').append(identifier);
            if (!outputItem.isEmpty()) {
                searchString.append(' ')
                        .append(BuiltInRegistries.ITEM.getKey(outputItem.getItem()).getNamespace());
            }

            return new RecipeEntry(serverIndex, key, recipe, displayName, name.toLowerCase(Locale.ENGLISH),
                    identifier, searchString.toString().toLowerCase(Locale.ENGLISH));
        }

        /**
         * Construct an entry without any gui data, for use on the server.
         * @param serverIndex The index of this recipe in the server's recipe list.
         * @param recipe The recipe.
         * @return An entry.
         */
        public static RecipeEntry ofServer(int serverIndex, IRecipeDefinition recipe) {
            return new RecipeEntry(serverIndex, null, recipe, Component.empty(), "", "", "");
        }

    }

    /**
     * All recipe data that an instance of this container is constructed from.
     */
    protected static class GuiRecipes {

        private final List<IRecipeDefinition> recipes;
        private final Map<IRecipeDefinition, RecipeEntry> entries;
        private final Set<RecipeKey> disabledRecipes;
        private final int recipesVersion;

        public GuiRecipes(List<RecipeEntry> entryList, Set<RecipeKey> disabledRecipes, int recipesVersion) {
            this.recipes = Lists.newArrayListWithCapacity(entryList.size());
            this.entries = Maps.newIdentityHashMap();
            for (RecipeEntry entry : entryList) {
                this.recipes.add(entry.recipe());
                this.entries.put(entry.recipe(), entry);
            }
            this.disabledRecipes = disabledRecipes;
            this.recipesVersion = recipesVersion;
        }

        public List<IRecipeDefinition> getRecipes() {
            return this.recipes;
        }

        public Map<IRecipeDefinition, RecipeEntry> getEntries() {
            return this.entries;
        }

        public Set<RecipeKey> getDisabledRecipes() {
            return this.disabledRecipes;
        }

        public int getRecipesVersion() {
            return this.recipesVersion;
        }

    }

}
