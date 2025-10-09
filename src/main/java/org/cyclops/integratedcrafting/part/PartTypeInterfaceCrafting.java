package org.cyclops.integratedcrafting.part;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeHandler;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.cyclopscore.helper.IModHelpersNeoForge;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.persist.nbt.NBTClassType;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.evaluate.InventoryVariableEvaluator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.part.PartTypeBase;
import org.cyclops.integrateddynamics.core.part.event.PartVariableDrivenVariableContentsUpdatedEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for auto crafting.
 * @author rubensworks
 */
public class PartTypeInterfaceCrafting extends PartTypeInterfaceCraftingBase<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    public PartTypeInterfaceCrafting(String name) {
        super(name);
    }

    @Override
    public int getConsumptionRate(State state) {
        return state.getCraftingJobHandler().getProcessingCraftingJobs().size() * GeneralConfig.interfaceCraftingBaseConsumption;
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
                PartTypeInterfaceCrafting.State partState = (PartTypeInterfaceCrafting.State) data.getLeft().getPartState(data.getRight().getCenter().getSide());
                return new ContainerPartInterfaceCrafting(id, playerInventory, partState.getInventoryVariables(),
                        Optional.of(data.getRight()), Optional.of(data.getLeft()), (PartTypeInterfaceCrafting) data.getMiddle());
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
        PartTypeInterfaceCrafting.State partState = (PartTypeInterfaceCrafting.State) partContainer.getPartState(pos.getSide());
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
    protected PartTypeInterfaceCrafting.State constructDefaultState() {
        return new PartTypeInterfaceCrafting.State();
    }

    @Override
    public void update(INetwork network, IPartNetwork partNetwork, PartTarget target, State state) {
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
                    // Remove the old recipe from the network
                    Int2ObjectMap<IRecipeDefinition> recipes = state.getRecipesIndexed();
                    IRecipeDefinition oldRecipe = recipes.get(slot);
                    if (oldRecipe != null) {
                        craftingNetwork.removeCraftingInterfaceRecipe(channel, state, oldRecipe);
                    }

                    // Reload the recipe in the slot
                    // We simulate initialization for the first two ticks, as dependency variables may still be loading,
                    // and errored may only go away after these dependencies are fully loaded.
                    // Related to CyclopsMC/IntegratedCrafting#110
                    state.reloadRecipe(slot, state.ticksAfterReload <= 1);

                    // Add the new recipe to the network
                    IRecipeDefinition newRecipe = recipes.get(slot);
                    if (newRecipe != null) {
                        craftingNetwork.addCraftingInterfaceRecipe(channel, state, newRecipe);
                    }
                }
            }
        }

        // Internal tick counter
        state.ticksAfterReload++;
    }

    @Override
    public void addDrops(PartTarget target, State state, List<ItemStack> itemStacks, boolean dropMainElement, boolean saveState) {
        // Drop the stored variables
        for(int i = 0; i < state.getInventoryVariables().getContainerSize(); i++) {
            ItemStack itemStack = state.getInventoryVariables().getItem(i);
            if(!itemStack.isEmpty()) {
                itemStacks.add(itemStack);
            }
        }
        state.getInventoryVariables().clearContent();

        super.addDrops(target, state, itemStacks, dropMainElement, saveState);
    }

    public static class State extends PartTypeInterfaceCraftingBase.State<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

        protected int ticksAfterReload = 0;

        private final SimpleInventory inventoryVariables;
        private final List<InventoryVariableEvaluator<ValueObjectTypeRecipe.ValueRecipe>> variableEvaluators;
        private final Int2ObjectMap<Component> recipeSlotMessages;
        private final Int2BooleanMap recipeSlotValidated;
        private final IntSet delayedRecipeReloads;
        private final Map<IVariable, Boolean> variableListeners;
        private boolean disableCraftingCheck = false;

        private final Int2ObjectMap<IRecipeDefinition> currentRecipes;

        public State() {
            this.inventoryVariables = new SimpleInventory(9, 1);
            this.inventoryVariables.addDirtyMarkListener(this);
            this.variableEvaluators = Lists.newArrayList();
            this.recipeSlotMessages = new Int2ObjectArrayMap<>();
            this.recipeSlotValidated = new Int2BooleanArrayMap();
            this.delayedRecipeReloads = new IntArraySet();
            this.variableListeners = new MapMaker().weakKeys().makeMap();
            this.currentRecipes = new Int2ObjectArrayMap<>();
        }

        /**
         * @return The inner variables inventory
         */
        public SimpleInventory getInventoryVariables() {
            return this.inventoryVariables;
        }

        @Override
        public void serialize(ValueOutput valueOutput) {
            super.serialize(valueOutput);
            inventoryVariables.writeToNBT(valueOutput, "variables");

            ValueOutput.ValueOutputList recipeSlotErrorsTag = valueOutput.childrenList("recipeSlotMessages");
            for (Int2ObjectMap.Entry<Component> entry : this.recipeSlotMessages.int2ObjectEntrySet()) {
                ValueOutput child = recipeSlotErrorsTag.addChild();
                child.putInt("key", entry.getIntKey());
                NBTClassType.writeNbt(Component.class, "value", entry.getValue(), child);
            }

            ValueOutput.ValueOutputList recipeSlotValidatedTag = valueOutput.childrenList("recipeSlotValidated");
            for (Int2BooleanMap.Entry entry : this.recipeSlotValidated.int2BooleanEntrySet()) {
                ValueOutput entryTag = recipeSlotValidatedTag.addChild();
                entryTag.putInt("key", entry.getIntKey());
                entryTag.putBoolean("value", entry.getBooleanValue());
            }

            valueOutput.putBoolean("disableCraftingCheck", disableCraftingCheck);
        }

        @Override
        public void deserialize(ValueInput valueInput) {
            super.deserialize(valueInput);
            inventoryVariables.readFromNBT(valueInput, "variables");

            this.recipeSlotMessages.clear();
            for (ValueInput slot : valueInput.childrenList("recipeSlotMessages").orElseThrow()) {
                Component unlocalizedString = NBTClassType.readNbt(Component.class, "value", slot);
                this.recipeSlotMessages.put(slot.getInt("key").orElseThrow(), unlocalizedString);
            }

            this.recipeSlotValidated.clear();
            for (ValueInput slot : valueInput.childrenList("recipeSlotValidated").orElseThrow()) {
                this.recipeSlotValidated.put((int) slot.getInt("key").orElseThrow(), slot.getBooleanOr("value", false));
            }

            this.disableCraftingCheck = valueInput.getBooleanOr("disableCraftingCheck", false);
        }

        @Override
        public void reloadRecipes(boolean initialize) {
            this.currentRecipes.clear();
            this.recipeSlotMessages.clear();
            this.recipeSlotValidated.clear();
            variableEvaluators.clear();
            for (int i = 0; i < getInventoryVariables().getContainerSize(); i++) {
                int slot = i;
                variableEvaluators.add(new InventoryVariableEvaluator<ValueObjectTypeRecipe.ValueRecipe>(
                        getInventoryVariables(), slot, valueDeseralizationContext, ValueTypes.OBJECT_RECIPE) {
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

        protected void reloadRecipe(int slot, boolean initialize) {
            this.currentRecipes.remove(slot);
            if (this.recipeSlotMessages.size() > slot) {
                this.recipeSlotMessages.remove(slot);
            }
            if (this.recipeSlotValidated.size() > slot) {
                this.recipeSlotValidated.remove(slot);
            }
            if (this.partNetwork != null) {
                InventoryVariableEvaluator<ValueObjectTypeRecipe.ValueRecipe> evaluator = variableEvaluators.get(slot);
                evaluator.refreshVariable(network, false);
                IVariable<ValueObjectTypeRecipe.ValueRecipe> variable = evaluator.getVariable(network);
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
                        if (value.getType() == ValueTypes.OBJECT_RECIPE) {
                            Optional<IRecipeDefinition> recipeWrapper = ((ValueObjectTypeRecipe.ValueRecipe) value).getRawValue();
                            if (recipeWrapper.isPresent()) {
                                IRecipeDefinition recipe = recipeWrapper.get();
                                if (!GeneralConfig.validateRecipesCraftingInterface || this.disableCraftingCheck || isValid(recipe)) {
                                    this.currentRecipes.put(slot, recipe);
                                    this.recipeSlotValidated.put(slot, true);
                                    this.recipeSlotMessages.put(slot, Component.translatable("gui.integratedcrafting.partinterface.slot.message.valid"));
                                } else {
                                    this.recipeSlotMessages.put(slot, Component.translatable("gui.integratedcrafting.partinterface.slot.message.invalid"));
                                }
                            }
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

                try {
                    IPartNetwork partNetwork = NetworkHelpers.getPartNetworkChecked(network);
                    NeoForge.EVENT_BUS.post(new PartVariableDrivenVariableContentsUpdatedEvent<>(network,
                            partNetwork, getTarget(),
                            PartTypes.INTERFACE_CRAFTING, this, lastPlayer, variable,
                            variable != null ? variable.getValue() : null));
                } catch (EvaluationException e) {
                    // Ignore error
                }
            }
            sendUpdate();
        }

        private void delayedReloadRecipe(int slot) {
            this.delayedRecipeReloads.add(slot);
        }


        private boolean isValid(IRecipeDefinition recipe) {
            DimPos dimPos = getTarget().getTarget().getPos();
            Direction side = getTarget().getTarget().getSide();
            IRecipeHandler recipeHandler = IModHelpersNeoForge.get().getCapabilityHelpers().getCapability(dimPos.getLevel(true), dimPos.getBlockPos(), side, org.cyclops.commoncapabilities.api.capability.Capabilities.RecipeHandler.BLOCK).orElse(null);
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
            return this.currentRecipes.values();
        }

        public Int2ObjectMap<IRecipeDefinition> getRecipesIndexed() {
            return currentRecipes;
        }

        public boolean isRecipeSlotValid(int slot) {
            return this.recipeSlotValidated.containsKey(slot);
        }

        @Nullable
        public Component getRecipeSlotUnlocalizedMessage(int slot) {
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

    }
}
