package org.cyclops.integratedcrafting.core.part;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeHandler;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.cyclopscore.helper.BlockEntityHelpers;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.persist.nbt.NBTClassType;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.evaluate.InventoryVariableEvaluator;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.part.PartTypeBase;
import org.cyclops.integrateddynamics.core.part.event.PartVariableDrivenVariableContentsUpdatedEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Base part for crafting interfaces that derive their recipes from variables in an inventory.
 *
 * Each slot can contribute zero or more recipes, which allows both one-recipe-per-slot variants
 * and variants where a single slot holds a whole list of recipes.
 *
 * @author rubensworks
 */
public abstract class PartTypeInterfaceCraftingVariableBase<P extends PartTypeInterfaceCraftingVariableBase<P, S>, S extends PartTypeInterfaceCraftingVariableBase.State<P, S>>
        extends PartTypeInterfaceCraftingBase<P, S> {

    public PartTypeInterfaceCraftingVariableBase(String name) {
        super(name);
    }

    /**
     * @return The value type that the variable slots of this part accept.
     */
    public abstract IValueType<?> getSlotValueType();

    /**
     * @return The gui background texture, derived from the part name.
     */
    public ResourceLocation getGuiTexture() {
        return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/part_" + getUniqueName().getPath() + ".png");
    }

    @Override
    public Optional<MenuProvider> getContainerProvider(PartPos pos) {
        return Optional.of(new MenuProvider() {

            @Override
            public MutableComponent getDisplayName() {
                return Component.translatable(getTranslationKey());
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
                Triple<IPartContainer, PartTypeBase, PartTarget> data = PartHelpers.getContainerPartConstructionData(pos);
                S partState = (S) data.getLeft().getPartState(data.getRight().getCenter().getSide());
                return new ContainerPartInterfaceCrafting<>(id, playerInventory, partState.getInventoryVariables(),
                        Optional.of(data.getRight()), Optional.of(data.getLeft()), (P) data.getMiddle());
            }

            @Override
            public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                return false;
            }
        });
    }

    @Override
    public void writeExtraGuiData(RegistryFriendlyByteBuf packetBuffer, PartPos pos, ServerPlayer player) {
        // Write inventory size
        IPartContainer partContainer = PartHelpers.getPartContainerChecked(pos);
        S partState = (S) partContainer.getPartState(pos.getSide());
        packetBuffer.writeInt(partState.getInventoryVariables().getContainerSize());

        super.writeExtraGuiData(packetBuffer, pos, player);
    }

    @Override
    public Optional<MenuProvider> getContainerProviderSettings(PartPos pos) {
        return Optional.of(new MenuProvider() {

            @Override
            public MutableComponent getDisplayName() {
                return Component.translatable(getTranslationKey());
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
                Triple<IPartContainer, PartTypeBase, PartTarget> data = PartHelpers.getContainerPartConstructionData(pos);
                return new ContainerPartInterfaceCraftingSettings(id, playerInventory, new SimpleContainer(0),
                        data.getRight(), Optional.of(data.getLeft()), data.getMiddle());
            }

            @Override
            public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                return false;
            }
        });
    }

    @Override
    public void update(INetwork network, IPartNetwork partNetwork, PartTarget target, S state) {
        super.update(network, partNetwork, target, state);

        // Reload recipes if needed
        IntSet slots = state.getDelayedRecipeReloads();
        if (!slots.isEmpty()) {
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability()).orElse(null);
            if (craftingNetwork != null) {
                IntSet slotsCopy = new IntOpenHashSet(slots); // Create a copy, to allow insertion into slots during this loop
                slots.clear();
                int channel = state.getChannelCrafting();
                for (Integer slot : slotsCopy) {
                    Int2ObjectMap<List<IRecipeDefinition>> recipes = state.getRecipesIndexed();
                    List<IRecipeDefinition> oldRecipes = recipes.get(slot);
                    oldRecipes = oldRecipes == null ? Collections.emptyList() : Lists.newArrayList(oldRecipes);

                    // Reload the recipes in the slot
                    // We simulate initialization for the first two ticks, as dependency variables may still be loading,
                    // and errored may only go away after these dependencies are fully loaded.
                    // Related to CyclopsMC/IntegratedCrafting#110
                    if (!state.reloadRecipe(slot, state.ticksAfterReload <= 1)) {
                        // The recipes of this slot are unchanged, so the network index is still correct
                        continue;
                    }

                    List<IRecipeDefinition> newRecipes = recipes.get(slot);
                    newRecipes = newRecipes == null ? Collections.emptyList() : newRecipes;

                    // Only patch what actually changed, as slots can hold many recipes
                    // that are re-evaluated whenever their variable is invalidated.
                    Set<IRecipeDefinition> oldRecipesLookup = Sets.newHashSet(oldRecipes);
                    Set<IRecipeDefinition> newRecipesLookup = Sets.newHashSet(newRecipes);
                    for (IRecipeDefinition oldRecipe : oldRecipes) {
                        if (!newRecipesLookup.contains(oldRecipe)) {
                            craftingNetwork.removeCraftingInterfaceRecipe(channel, state, oldRecipe);
                        }
                    }
                    for (IRecipeDefinition newRecipe : newRecipes) {
                        if (!oldRecipesLookup.contains(newRecipe)) {
                            craftingNetwork.addCraftingInterfaceRecipe(channel, state, newRecipe);
                        }
                    }
                }
            }
        }

        // Internal tick counter
        state.ticksAfterReload++;
    }

    @Override
    public void addDrops(PartTarget target, S state, List<ItemStack> itemStacks, boolean dropMainElement, boolean saveState) {
        // Drop the stored variables
        for (int i = 0; i < state.getInventoryVariables().getContainerSize(); i++) {
            ItemStack itemStack = state.getInventoryVariables().getItem(i);
            if (!itemStack.isEmpty()) {
                itemStacks.add(itemStack);
            }
        }
        state.getInventoryVariables().clearContent();

        super.addDrops(target, state, itemStacks, dropMainElement, saveState);
    }

    public static abstract class State<P extends PartTypeInterfaceCraftingVariableBase<P, S>, S extends PartTypeInterfaceCraftingVariableBase.State<P, S>>
            extends PartTypeInterfaceCraftingBase.State<P, S> {

        protected int ticksAfterReload = 0;

        private final SimpleInventory inventoryVariables;
        private final List<InventoryVariableEvaluator<IValue>> variableEvaluators;
        private final Int2ObjectMap<MutableComponent> recipeSlotMessages;
        private final Int2BooleanMap recipeSlotValidated;
        private final IntSet delayedRecipeReloads;
        private final Map<IVariable, Boolean> variableListeners;
        private boolean disableCraftingCheck = false;

        private final Int2ObjectMap<List<IRecipeDefinition>> currentRecipes;
        private List<IRecipeDefinition> currentRecipesFlattened;
        // Validation results, reused until the whole part reloads, which is when the target may have changed.
        // Keyed by identity: recipe handlers hand out the same instances on every read,
        // while hashing a recipe by value is expensive.
        private final Map<IRecipeDefinition, Boolean> validationCache;

        public State(int inventorySize) {
            this.inventoryVariables = new SimpleInventory(inventorySize, 1);
            this.inventoryVariables.addDirtyMarkListener(this);
            this.variableEvaluators = Lists.newArrayList();
            this.recipeSlotMessages = new Int2ObjectArrayMap<>();
            this.recipeSlotValidated = new Int2BooleanArrayMap();
            this.delayedRecipeReloads = new IntArraySet();
            this.variableListeners = new MapMaker().weakKeys().makeMap();
            this.currentRecipes = new Int2ObjectArrayMap<>();
            this.currentRecipesFlattened = Collections.emptyList();
            this.validationCache = Maps.newIdentityHashMap();
        }

        /**
         * @return The part type that this state belongs to.
         */
        protected abstract P getPartTypeInstance();

        /**
         * Derive the recipes that the given evaluated variable value contributes to this interface.
         * @param slot The slot that the value was evaluated for.
         * @param value The value that was evaluated.
         * @return The recipes in the value, which may be empty.
         * @throws EvaluationException If the value could not be converted into recipes.
         */
        protected abstract List<IRecipeDefinition> extractRecipes(int slot, IValue value) throws EvaluationException;

        /**
         * @return The message to show for a slot for which all recipes were accepted by the target.
         */
        protected MutableComponent getRecipesValidMessage(int slot, int count) {
            return count > 1
                    ? Component.translatable("gui.integratedcrafting.partinterface.slot.message.valid.multiple", count)
                    : Component.translatable("gui.integratedcrafting.partinterface.slot.message.valid");
        }

        /**
         * @return The inner variables inventory
         */
        public SimpleInventory getInventoryVariables() {
            return this.inventoryVariables;
        }

        @Override
        public void writeToNBT(ValueDeseralizationContext valueDeseralizationContext, CompoundTag tag) {
            super.writeToNBT(valueDeseralizationContext, tag);
            inventoryVariables.writeToNBT(valueDeseralizationContext.holderLookupProvider(), tag, "variables");

            CompoundTag recipeSlotErrorsTag = new CompoundTag();
            for (Int2ObjectMap.Entry<MutableComponent> entry : this.recipeSlotMessages.int2ObjectEntrySet()) {
                NBTClassType.writeNbt(MutableComponent.class, String.valueOf(entry.getIntKey()), entry.getValue(), recipeSlotErrorsTag, valueDeseralizationContext.holderLookupProvider());
            }
            tag.put("recipeSlotMessages", recipeSlotErrorsTag);

            CompoundTag recipeSlotValidatedTag = new CompoundTag();
            for (Int2BooleanMap.Entry entry : this.recipeSlotValidated.int2BooleanEntrySet()) {
                recipeSlotValidatedTag.putBoolean(String.valueOf(entry.getIntKey()), entry.getBooleanValue());
            }
            tag.put("recipeSlotValidated", recipeSlotValidatedTag);

            tag.putBoolean("disableCraftingCheck", disableCraftingCheck);
        }

        @Override
        public void readFromNBT(ValueDeseralizationContext valueDeseralizationContext, CompoundTag tag) {
            super.readFromNBT(valueDeseralizationContext, tag);
            inventoryVariables.readFromNBT(valueDeseralizationContext.holderLookupProvider(), tag, "variables");

            this.recipeSlotMessages.clear();
            CompoundTag recipeSlotErrorsTag = tag.getCompound("recipeSlotMessages");
            for (String slot : recipeSlotErrorsTag.getAllKeys()) {
                MutableComponent unlocalizedString = NBTClassType.readNbt(MutableComponent.class, slot, recipeSlotErrorsTag, valueDeseralizationContext.holderLookupProvider());
                this.recipeSlotMessages.put(Integer.parseInt(slot), unlocalizedString);
            }

            this.recipeSlotValidated.clear();
            CompoundTag recipeSlotValidatedTag = tag.getCompound("recipeSlotValidated");
            for (String slot : recipeSlotValidatedTag.getAllKeys()) {
                this.recipeSlotValidated.put(Integer.parseInt(slot), recipeSlotValidatedTag.getBoolean(slot));
            }

            this.disableCraftingCheck = tag.getBoolean("disableCraftingCheck");
        }

        @Override
        public void reloadRecipes(boolean initialize) {
            this.validationCache.clear();
            this.currentRecipes.clear();
            this.invalidateRecipesFlattened();
            this.recipeSlotMessages.clear();
            this.recipeSlotValidated.clear();
            variableEvaluators.clear();
            for (int i = 0; i < getInventoryVariables().getContainerSize(); i++) {
                int slot = i;
                variableEvaluators.add(new InventoryVariableEvaluator<IValue>(
                        getInventoryVariables(), slot, valueDeseralizationContext, (IValueType<IValue>) getPartTypeInstance().getSlotValueType()) {
                    @Override
                    public void onErrorsChanged() {
                        super.onErrorsChanged();
                        setLocalErrors(slot, getErrors());
                    }
                });
            }
            if (this.partNetwork != null) {
                for (int i = 0; i < getInventoryVariables().getContainerSize(); i++) {
                    reloadRecipe(i, initialize);
                }
            }
        }

        private void setLocalErrors(int slot, List<MutableComponent> errors) {
            if (errors.isEmpty()) {
                if (this.recipeSlotMessages.size() > slot) {
                    this.recipeSlotMessages.remove(slot);
                }
            } else {
                this.recipeSlotMessages.put(slot, errors.get(0));
            }
        }

        protected boolean reloadRecipe(int slot, boolean initialize) {
            List<IRecipeDefinition> previousRecipes = this.currentRecipes.get(slot);
            MutableComponent previousMessage = this.recipeSlotMessages.get(slot);
            boolean changed = true;

            this.currentRecipes.remove(slot);
            this.invalidateRecipesFlattened();
            if (this.recipeSlotMessages.size() > slot) {
                this.recipeSlotMessages.remove(slot);
            }
            if (this.recipeSlotValidated.size() > slot) {
                this.recipeSlotValidated.remove(slot);
            }
            if (this.partNetwork != null) {
                InventoryVariableEvaluator<IValue> evaluator = variableEvaluators.get(slot);
                evaluator.refreshVariable(network, false);
                IVariable<IValue> variable = evaluator.getVariable(network);
                if (variable != null) {
                    try {
                        // Refresh the recipe if variable is changed
                        // The map is needed because we only want to register the listener once for each variable
                        if (!this.variableListeners.containsKey(variable)) {
                            variable.addInvalidationListener(() -> {
                                this.variableListeners.remove(variable);
                                delayedReloadRecipe(slot);
                            });
                            this.variableListeners.put(variable, true);
                        }

                        IValue value = variable.getValue();
                        if (value.getType() == getPartTypeInstance().getSlotValueType()) {
                            setSlotRecipes(slot, extractRecipes(slot, value));
                        } else {
                            this.recipeSlotMessages.put(slot, Component.translatable("gui.integratedcrafting.partinterface.slot.message.norecipe"));
                        }
                    } catch (EvaluationException e) {
                        this.recipeSlotMessages.put(slot, e.getErrorMessage());
                    }
                } else {
                    // If we're initializing, the variable might be referencing other variables that are not yet loaded.
                    // So let's retry once in the next tick.
                    if (initialize && evaluator.hasVariable()) {
                        this.delayedReloadRecipe(slot);
                    } else {
                        this.recipeSlotMessages.put(slot, Component.translatable("gui.integratedcrafting.partinterface.slot.message.norecipe"));
                    }
                }

                changed = !Objects.equals(previousRecipes, this.currentRecipes.get(slot))
                        || !Objects.equals(previousMessage, this.recipeSlotMessages.get(slot));

                if (changed) {
                    try {
                        IPartNetwork partNetwork = NetworkHelpers.getPartNetworkChecked(network);
                        NeoForge.EVENT_BUS.post(new PartVariableDrivenVariableContentsUpdatedEvent<>(network,
                                partNetwork, getTarget(),
                                getPartTypeInstance(), (S) this, lastPlayer, variable,
                                variable != null ? variable.getValue() : null));
                    } catch (EvaluationException e) {
                        // Ignore error
                    }
                }
            }

            // A slot whose recipes did not change needs no client sync and no network re-indexing
            if (changed) {
                sendUpdate();
            }
            return changed;
        }

        /**
         * Validate the given recipes, store the valid ones in the given slot, and set the slot message accordingly.
         */
        private void setSlotRecipes(int slot, List<IRecipeDefinition> recipes) {
            if (recipes.isEmpty()) {
                this.recipeSlotMessages.put(slot, Component.translatable("gui.integratedcrafting.partinterface.slot.message.empty"));
                return;
            }

            List<IRecipeDefinition> validRecipes;
            if (!GeneralConfig.validateRecipesCraftingInterface || this.disableCraftingCheck) {
                validRecipes = recipes;
            } else {
                validRecipes = Lists.newArrayListWithExpectedSize(recipes.size());
                for (IRecipeDefinition recipe : recipes) {
                    if (this.validationCache.computeIfAbsent(recipe, this::isValid)) {
                        validRecipes.add(recipe);
                    }
                }
            }

            if (validRecipes.isEmpty()) {
                this.recipeSlotMessages.put(slot, Component.translatable("gui.integratedcrafting.partinterface.slot.message.invalid"));
                return;
            }

            this.currentRecipes.put(slot, validRecipes);
            this.invalidateRecipesFlattened();
            this.recipeSlotValidated.put(slot, true);
            if (validRecipes.size() < recipes.size()) {
                this.recipeSlotMessages.put(slot, Component.translatable("gui.integratedcrafting.partinterface.slot.message.partial",
                        validRecipes.size(), recipes.size()));
            } else {
                this.recipeSlotMessages.put(slot, getRecipesValidMessage(slot, validRecipes.size()));
            }
        }

        protected void delayedReloadRecipe(int slot) {
            this.delayedRecipeReloads.add(slot);
        }

        protected boolean isValid(IRecipeDefinition recipe) {
            DimPos dimPos = getTarget().getTarget().getPos();
            Direction side = getTarget().getTarget().getSide();
            IRecipeHandler recipeHandler = BlockEntityHelpers.getCapability(dimPos.getLevel(true), dimPos.getBlockPos(), side, org.cyclops.commoncapabilities.api.capability.Capabilities.RecipeHandler.BLOCK).orElse(null);
            if (recipeHandler != null) {
                IMixedIngredients simulatedOutput = recipeHandler.simulate(recipe);
                if (simulatedOutput != null && !simulatedOutput.isEmpty()) {
                    if (recipe.getOutput().containsAll(simulatedOutput)) {
                        return true;
                    } else {
                        if (GeneralConfig.logRecipeValidationFailures) {
                            IntegratedCrafting.clog(Level.INFO, "Recipe validation failure: incompatible recipe output and simulated output:\nRecipe output: " + recipe.getOutput() + "\nSimulated output: " + simulatedOutput);
                        }
                        return false;
                    }
                }
                if (GeneralConfig.logRecipeValidationFailures) {
                    IntegratedCrafting.clog(Level.INFO, "Recipe validation failure: No output was obtained when simulating a recipe\n" + recipe);
                }
                return false;
            }
            return true; // No recipe handler capability is present, so we can't confirm that the recipe will work.
        }

        @Override
        public void onDirty() {
            super.onDirty();

            // Unregister from the network, when all old recipes are still in place
            if (craftingNetwork != null) {
                craftingNetwork.removeCraftingInterface(getChannelCrafting(), this);
            }

            // Recalculate recipes
            if (getTarget() != null && !getTarget().getCenter().getPos().getLevel(true).isClientSide) {
                reloadRecipes(false);
            }

            // Re-register to the network, to force an update for all new recipes
            if (craftingNetwork != null) {
                craftingNetwork.addCraftingInterface(getChannelCrafting(), this);
            }
        }

        @Override
        public Collection<IRecipeDefinition> getRecipes() {
            if (this.currentRecipesFlattened == null) {
                List<IRecipeDefinition> flattened = Lists.newArrayList();
                for (List<IRecipeDefinition> recipes : this.currentRecipes.values()) {
                    flattened.addAll(recipes);
                }
                this.currentRecipesFlattened = flattened;
            }
            return this.currentRecipesFlattened;
        }

        private void invalidateRecipesFlattened() {
            this.currentRecipesFlattened = null;
        }

        public Int2ObjectMap<List<IRecipeDefinition>> getRecipesIndexed() {
            return currentRecipes;
        }

        public boolean isRecipeSlotValid(int slot) {
            return this.recipeSlotValidated.containsKey(slot);
        }

        @Nullable
        public MutableComponent getRecipeSlotUnlocalizedMessage(int slot) {
            return this.recipeSlotMessages.get(slot);
        }

        public IntSet getDelayedRecipeReloads() {
            return delayedRecipeReloads;
        }

        public void setDisableCraftingCheck(boolean disableCraftingCheck) {
            if (disableCraftingCheck != this.disableCraftingCheck) {
                this.disableCraftingCheck = disableCraftingCheck;

                this.sendUpdate();
            }
        }

        public boolean isDisableCraftingCheck() {
            return disableCraftingCheck;
        }

        /**
         * Remove duplicates while preserving order.
         *
         * The crafting network drops a recipe from its index as soon as one removal is requested for it,
         * so the same recipe must never be added twice from a single interface.
         */
        protected static List<IRecipeDefinition> deduplicate(List<IRecipeDefinition> recipes) {
            return Lists.newArrayList(new LinkedHashSet<>(recipes));
        }

    }
}
