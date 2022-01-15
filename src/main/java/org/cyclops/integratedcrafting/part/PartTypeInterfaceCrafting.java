package org.cyclops.integratedcrafting.part;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.capability.block.BlockCapabilities;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeHandler;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.IngredientInstanceWrapper;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.cyclopscore.helper.TileHelpers;
import org.cyclops.cyclopscore.ingredient.storage.IngredientStorageHelpers;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.persist.nbt.NBTClassType;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobStatus;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.capability.network.CraftingInterfaceConfig;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkConfig;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.core.CraftingJobHandler;
import org.cyclops.integratedcrafting.core.CraftingProcessOverrides;
import org.cyclops.integratedcrafting.core.part.PartTypeCraftingBase;
import org.cyclops.integratedcrafting.ingredient.storage.IngredientComponentStorageSlottedInsertProxy;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.PrioritizedPartPos;
import org.cyclops.integrateddynamics.capability.network.PositionedAddonsNetworkIngredientsHandlerConfig;
import org.cyclops.integrateddynamics.core.evaluate.InventoryVariableEvaluator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.part.PartStateBase;
import org.cyclops.integrateddynamics.core.part.PartTypeBase;
import org.cyclops.integrateddynamics.core.part.event.PartVariableDrivenVariableContentsUpdatedEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for item handlers.
 * @author rubensworks
 */
public class PartTypeInterfaceCrafting extends PartTypeCraftingBase<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    public PartTypeInterfaceCrafting(String name) {
        super(name);
    }

    @Override
    public int getConsumptionRate(State state) {
        return state.getCraftingJobHandler().getProcessingCraftingJobs().size() * GeneralConfig.interfaceCraftingBaseConsumption;
    }

    @Override
    public Optional<INamedContainerProvider> getContainerProvider(PartPos pos) {
        return Optional.of(new INamedContainerProvider() {

            @Override
            public IFormattableTextComponent getDisplayName() {
                return new TranslationTextComponent(getTranslationKey());
            }

            @Override
            public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                Triple<IPartContainer, PartTypeBase, PartTarget> data = PartHelpers.getContainerPartConstructionData(pos);
                PartTypeInterfaceCrafting.State partState = (PartTypeInterfaceCrafting.State) data.getLeft().getPartState(data.getRight().getCenter().getSide());
                return new ContainerPartInterfaceCrafting(id, playerInventory, partState.getInventoryVariables(),
                        Optional.of(data.getRight()), Optional.of(data.getLeft()), (PartTypeInterfaceCrafting) data.getMiddle());
            }
        });
    }

    @Override
    public void writeExtraGuiData(PacketBuffer packetBuffer, PartPos pos, ServerPlayerEntity player) {
        // Write inventory size
        IPartContainer partContainer = PartHelpers.getPartContainerChecked(pos);
        PartTypeInterfaceCrafting.State partState = (PartTypeInterfaceCrafting.State) partContainer.getPartState(pos.getSide());
        packetBuffer.writeInt(partState.getInventoryVariables().getContainerSize());

        super.writeExtraGuiData(packetBuffer, pos, player);
    }

    @Override
    public Optional<INamedContainerProvider> getContainerProviderSettings(PartPos pos) {
        return Optional.of(new INamedContainerProvider() {

            @Override
            public IFormattableTextComponent getDisplayName() {
                return new TranslationTextComponent(getTranslationKey());
            }

            @Override
            public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                Triple<IPartContainer, PartTypeBase, PartTarget> data = PartHelpers.getContainerPartConstructionData(pos);
                return new ContainerPartInterfaceCraftingSettings(id, playerInventory, new Inventory(0),
                        data.getRight(), Optional.of(data.getLeft()), data.getMiddle());
            }
        });
    }

    @Override
    protected PartTypeInterfaceCrafting.State constructDefaultState() {
        return new PartTypeInterfaceCrafting.State();
    }

    @Override
    public void afterNetworkReAlive(INetwork network, IPartNetwork partNetwork, PartTarget target, PartTypeInterfaceCrafting.State state) {
        super.afterNetworkReAlive(network, partNetwork, target, state);
        addTargetToNetwork(network, target, state);
    }

    @Override
    public void onNetworkRemoval(INetwork network, IPartNetwork partNetwork, PartTarget target, PartTypeInterfaceCrafting.State state) {
        super.onNetworkRemoval(network, partNetwork, target, state);
        removeTargetFromNetwork(network, target.getTarget(), state);
    }

    @Override
    public void onNetworkAddition(INetwork network, IPartNetwork partNetwork, PartTarget target, PartTypeInterfaceCrafting.State state) {
        super.onNetworkAddition(network, partNetwork, target, state);
        addTargetToNetwork(network, target, state);
    }

    @Override
    public void setPriorityAndChannel(INetwork network, IPartNetwork partNetwork, PartTarget target, PartTypeInterfaceCrafting.State state, int priority, int channel) {
        // We need to do this because the crafting network is not automagically aware of the priority changes,
        // so we have to re-add it.
        removeTargetFromNetwork(network, target.getTarget(), state);
        super.setPriorityAndChannel(network, partNetwork, target, state, priority, channel);
        addTargetToNetwork(network, target, state);
    }

    protected Capability<ICraftingNetwork> getNetworkCapability() {
        return CraftingNetworkConfig.CAPABILITY;
    }

    protected void addTargetToNetwork(INetwork network, PartTarget pos, PartTypeInterfaceCrafting.State state) {
        network.getCapability(getNetworkCapability())
                .ifPresent(craftingNetwork -> {
                    int channelCrafting = state.getChannelCrafting();
                    state.setTarget(pos);
                    state.setNetworks(network, craftingNetwork, NetworkHelpers.getPartNetworkChecked(network), channelCrafting);
                    state.setShouldAddToCraftingNetwork(true);
                });
    }

    protected void removeTargetFromNetwork(INetwork network, PartPos pos, PartTypeInterfaceCrafting.State state) {
        ICraftingNetwork craftingNetwork = state.getCraftingNetwork();
        if (craftingNetwork != null) {
            network.getCapability(getNetworkCapability())
                    .ifPresent(n -> n.removeCraftingInterface(state.getChannelCrafting(), state));
        }
        state.setNetworks(null, null, null, -1);
        state.setTarget(null);
    }

    @Override
    public boolean isUpdate(State state) {
        return true;
    }

    @Override
    public int getMinimumUpdateInterval(State state) {
        return state.getDefaultUpdateInterval();
    }

    @Override
    public void update(INetwork network, IPartNetwork partNetwork, PartTarget target, State state) {
        super.update(network, partNetwork, target, state);

        // Init network data in part state if it has not been done yet.
        // This can occur when the part chunk is being reloaded.
        if (state.getCraftingNetwork() == null) {
            addTargetToNetwork(network, target, state);
        }

        int channel = state.getChannelCrafting();

        // Update the network data in the part state
        if (state.shouldAddToCraftingNetwork()) {
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability()).orElse(null);
            craftingNetwork.addCraftingInterface(channel, state);
            state.setShouldAddToCraftingNetwork(false);
        }

        // Push any pending output ingredients into the network
        ListIterator<IngredientInstanceWrapper<?, ?>> outputBufferIt = state.getInventoryOutputBuffer().listIterator();
        while (outputBufferIt.hasNext()) {
            IngredientInstanceWrapper<?, ?> newWrapper = insertIntoNetwork(outputBufferIt.next(),
                    network, state.getChannelCrafting());
            if (newWrapper == null) {
                outputBufferIt.remove();
            } else {
                outputBufferIt.set(newWrapper);
            }
        }

        // Block job ticking if there still are outputs in our crafting result buffer.
        if (state.getInventoryOutputBuffer().isEmpty()) {
            // Tick the job handler
            PartPos targetPos = state.getTarget().getTarget();
            state.getCraftingJobHandler().update(network, channel, targetPos);
        }

        // Reload recipes if needed
        IntSet slots = state.getDelayedRecipeReloads();
        if (!slots.isEmpty()) {
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability()).orElse(null);
            if (craftingNetwork != null) {
                for (Integer slot : slots) {
                    // Remove the old recipe from the network
                    Int2ObjectMap<IRecipeDefinition> recipes = state.getRecipesIndexed();
                    IRecipeDefinition oldRecipe = recipes.get(slot);
                    if (oldRecipe != null) {
                        craftingNetwork.removeCraftingInterfaceRecipe(channel, state, oldRecipe);
                    }

                    // Reload the recipe in the slot
                    state.reloadRecipe(slot);

                    // Add the new recipe to the network
                    IRecipeDefinition newRecipe = recipes.get(slot);
                    if (newRecipe != null) {
                        craftingNetwork.addCraftingInterfaceRecipe(channel, state, newRecipe);
                    }
                }
            }
            slots.clear();
        }
    }

    @Nullable
    protected static <T, M> IngredientInstanceWrapper<T, M> insertIntoNetwork(IngredientInstanceWrapper<T, M> wrapper,
                                                                              INetwork network, int channel) {
        IPositionedAddonsNetworkIngredients<T, M> storageNetwork = wrapper.getComponent()
                .getCapability(PositionedAddonsNetworkIngredientsHandlerConfig.CAPABILITY)
                .map(n -> (IPositionedAddonsNetworkIngredients<T, M>) n.getStorage(network).orElse(null))
                .orElse(null);
        if (storageNetwork != null) {
            IIngredientComponentStorage<T, M> storage = storageNetwork.getChannel(channel);
            T remaining = storage.insert(wrapper.getInstance(), false);
            if (wrapper.getComponent().getMatcher().isEmpty(remaining)) {
                return null;
            } else {
                return new IngredientInstanceWrapper<>(wrapper.getComponent(), remaining);
            }
        }
        return wrapper;
    }

    @Override
    public void addDrops(PartTarget target, State state, List<ItemStack> itemStacks, boolean dropMainElement, boolean saveState) {
        // Drop any remaining output ingredients (only items)
        for (IngredientInstanceWrapper<?, ?> ingredientInstanceWrapper : state.getInventoryOutputBuffer()) {
            if (ingredientInstanceWrapper.getComponent() == IngredientComponent.ITEMSTACK) {
                itemStacks.add((ItemStack) ingredientInstanceWrapper.getInstance());
            }
        }
        state.getInventoryOutputBuffer().clear();

        // Drop the stored variables
        for(int i = 0; i < state.getInventoryVariables().getContainerSize(); i++) {
            ItemStack itemStack = state.getInventoryVariables().getItem(i);
            if(!itemStack.isEmpty()) {
                itemStacks.add(itemStack);
            }
        }
        // state.getInventoryVariables().clearContent(); // TODO: restore

        super.addDrops(target, state, itemStacks, dropMainElement, saveState);
    }

    public static class State extends PartStateBase<PartTypeInterfaceCrafting>
            implements ICraftingInterface, ICraftingResultsSink {

        private final CraftingJobHandler craftingJobHandler;
        private final SimpleInventory inventoryVariables;
        private final List<InventoryVariableEvaluator<ValueObjectTypeRecipe.ValueRecipe>> variableEvaluators;
        private final List<IngredientInstanceWrapper<?, ?>> inventoryOutputBuffer;
        private final Int2ObjectMap<IFormattableTextComponent> recipeSlotMessages;
        private final Int2BooleanMap recipeSlotValidated;
        private final IntSet delayedRecipeReloads;
        private final Map<IVariable, Boolean> variableListeners;
        private int channelCrafting = 0;
        private boolean disableCraftingCheck = false;

        private final Int2ObjectMap<IRecipeDefinition> currentRecipes;
        private PartTarget target = null;
        private INetwork network = null;
        private ICraftingNetwork craftingNetwork = null;
        private IPartNetwork partNetwork = null;
        private int channel = -1;
        private boolean shouldAddToCraftingNetwork = false;
        private PlayerEntity lastPlayer;

        public State() {
            this.craftingJobHandler = new CraftingJobHandler(1,
                    CraftingProcessOverrides.REGISTRY.getCraftingProcessOverrides(), this);
            this.inventoryVariables = new SimpleInventory(9, 1);
            this.inventoryVariables.addDirtyMarkListener(this);
            this.variableEvaluators = Lists.newArrayList();
            this.inventoryOutputBuffer = Lists.newArrayList();
            this.recipeSlotMessages = new Int2ObjectArrayMap<>();
            this.recipeSlotValidated = new Int2BooleanArrayMap();
            this.delayedRecipeReloads = new IntArraySet();
            this.variableListeners = new MapMaker().weakKeys().makeMap();
            this.currentRecipes = new Int2ObjectArrayMap<>();
        }

        @Override
        protected int getDefaultUpdateInterval() {
            return GeneralConfig.minCraftingInterfaceUpdateFreq;
        }

        /**
         * @return The inner variables inventory
         */
        public SimpleInventory getInventoryVariables() {
            return this.inventoryVariables;
        }

        @Override
        public void writeToNBT(CompoundNBT tag) {
            super.writeToNBT(tag);
            inventoryVariables.writeToNBT(tag, "variables");

            ListNBT instanceTags = new ListNBT();
            for (IngredientInstanceWrapper instanceWrapper : inventoryOutputBuffer) {
                CompoundNBT instanceTag = new CompoundNBT();
                instanceTag.putString("component", instanceWrapper.getComponent().getRegistryName().toString());
                instanceTag.put("instance", instanceWrapper.getComponent().getSerializer().serializeInstance(instanceWrapper.getInstance()));
                instanceTags.add(instanceTag);
            }
            tag.put("inventoryOutputBuffer", instanceTags);

            this.craftingJobHandler.writeToNBT(tag);
            tag.putInt("channelCrafting", channelCrafting);

            CompoundNBT recipeSlotErrorsTag = new CompoundNBT();
            for (Int2ObjectMap.Entry<IFormattableTextComponent> entry : this.recipeSlotMessages.int2ObjectEntrySet()) {
                NBTClassType.writeNbt(IFormattableTextComponent.class, String.valueOf(entry.getIntKey()), entry.getValue(), recipeSlotErrorsTag);
            }
            tag.put("recipeSlotMessages", recipeSlotErrorsTag);

            CompoundNBT recipeSlotValidatedTag = new CompoundNBT();
            for (Int2BooleanMap.Entry entry : this.recipeSlotValidated.int2BooleanEntrySet()) {
                recipeSlotValidatedTag.putBoolean(String.valueOf(entry.getIntKey()), entry.getBooleanValue());
            }
            tag.put("recipeSlotValidated", recipeSlotValidatedTag);

            tag.putBoolean("disableCraftingCheck", disableCraftingCheck);
        }

        @Override
        public void readFromNBT(CompoundNBT tag) {
            super.readFromNBT(tag);
            inventoryVariables.readFromNBT(tag, "variables");

            this.inventoryOutputBuffer.clear();
            for (INBT instanceTagRaw : tag.getList("inventoryOutputBuffer", Constants.NBT.TAG_COMPOUND)) {
                CompoundNBT instanceTag = (CompoundNBT) instanceTagRaw;
                String componentName = instanceTag.getString("component");
                IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.getValue(new ResourceLocation(componentName));
                this.inventoryOutputBuffer.add(new IngredientInstanceWrapper(component,
                        component.getSerializer().deserializeInstance(instanceTag.get("instance"))));
            }

            this.craftingJobHandler.readFromNBT(tag);
            this.channelCrafting = tag.getInt("channelCrafting");

            this.recipeSlotMessages.clear();
            CompoundNBT recipeSlotErrorsTag = tag.getCompound("recipeSlotMessages");
            for (String slot : recipeSlotErrorsTag.getAllKeys()) {
                IFormattableTextComponent unlocalizedString = NBTClassType.readNbt(IFormattableTextComponent.class, slot, recipeSlotErrorsTag);
                this.recipeSlotMessages.put(Integer.parseInt(slot), unlocalizedString);
            }

            this.recipeSlotValidated.clear();
            CompoundNBT recipeSlotValidatedTag = tag.getCompound("recipeSlotValidated");
            for (String slot : recipeSlotValidatedTag.getAllKeys()) {
                this.recipeSlotValidated.put(Integer.parseInt(slot), recipeSlotValidatedTag.getBoolean(slot));
            }

            this.disableCraftingCheck = tag.getBoolean("disableCraftingCheck");
        }

        public void setChannelCrafting(int channelCrafting) {
            if (this.channelCrafting != channelCrafting) {
                // Unregister from the network
                if (craftingNetwork != null) {
                    craftingNetwork.removeCraftingInterface(this.channelCrafting, this);
                }

                // Update the channel
                this.channelCrafting = channelCrafting;

                // Re-register to the network
                if (craftingNetwork != null) {
                    craftingNetwork.addCraftingInterface(this.channelCrafting, this);
                }

                sendUpdate();
            }
        }

        public int getChannelCrafting() {
            return channelCrafting;
        }

        public void reloadRecipes() {
            this.currentRecipes.clear();
            this.recipeSlotMessages.clear();
            this.recipeSlotValidated.clear();
            variableEvaluators.clear();
            for (int i = 0; i < getInventoryVariables().getContainerSize(); i++) {
                int slot = i;
                variableEvaluators.add(new InventoryVariableEvaluator<ValueObjectTypeRecipe.ValueRecipe>(
                        getInventoryVariables(), slot, ValueTypes.OBJECT_RECIPE) {
                    @Override
                    public void onErrorsChanged() {
                        super.onErrorsChanged();
                        setLocalErrors(slot, getErrors());
                    }
                });
            }
            if (this.partNetwork != null) {
                for (int i = 0; i < getInventoryVariables().getContainerSize(); i++) {
                    reloadRecipe(i);
                }
            }
        }

        private void setLocalErrors(int slot, List<IFormattableTextComponent> errors) {
            if (errors.isEmpty()) {
                if (this.recipeSlotMessages.size() > slot) {
                    this.recipeSlotMessages.remove(slot);
                }
            } else {
                this.recipeSlotMessages.put(slot, errors.get(0));
            }
        }

        protected void reloadRecipe(int slot) {
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
                                    this.recipeSlotMessages.put(slot, new TranslationTextComponent("gui.integratedcrafting.partinterface.slot.message.valid"));
                                } else {
                                    this.recipeSlotMessages.put(slot, new TranslationTextComponent("gui.integratedcrafting.partinterface.slot.message.invalid"));
                                }
                            }
                        } else {
                            this.recipeSlotMessages.put(slot, new TranslationTextComponent("gui.integratedcrafting.partinterface.slot.message.norecipe"));
                        }
                    } catch (EvaluationException e) {
                        this.recipeSlotMessages.put(slot, e.getErrorMessage());
                    }
                } else {
                    this.recipeSlotMessages.put(slot, new TranslationTextComponent("gui.integratedcrafting.partinterface.slot.message.norecipe"));
                }

                try {
                    IPartNetwork partNetwork = NetworkHelpers.getPartNetworkChecked(network);
                    MinecraftForge.EVENT_BUS.post(new PartVariableDrivenVariableContentsUpdatedEvent<>(network,
                            partNetwork, getTarget(),
                            PartTypes.INTERFACE_CRAFTING, this, lastPlayer, variable,
                            variable != null ? variable.getValue() : null));
                } catch (EvaluationException e) {
                    // Ignore error
                }
            }
            sendUpdate();
        }

        public void setLastPlayer(PlayerEntity lastPlayer) {
            this.lastPlayer = lastPlayer;
        }

        private void delayedReloadRecipe(int slot) {
            this.delayedRecipeReloads.add(slot);
        }


        private boolean isValid(IRecipeDefinition recipe) {
            DimPos dimPos = getTarget().getTarget().getPos();
            Direction side = getTarget().getTarget().getSide();
            IRecipeHandler recipeHandler = TileHelpers.getCapability(dimPos.getWorld(true), dimPos.getBlockPos(), side, Capabilities.RECIPE_HANDLER).orElse(null);
            if (recipeHandler == null) {
                BlockState blockState = dimPos.getWorld(true).getBlockState(dimPos.getBlockPos());
                recipeHandler = BlockCapabilities.getInstance().getCapability(blockState, Capabilities.RECIPE_HANDLER,
                        dimPos.getWorld(true), dimPos.getBlockPos(), side)
                .orElse(null);
            }
            if (recipeHandler != null) {
                IMixedIngredients simulatedOutput = recipeHandler.simulate(MixedIngredients.fromRecipeInput(recipe));
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
                craftingNetwork.removeCraftingInterface(channelCrafting, this);
            }

            // Recalculate recipes
            if (getTarget() != null && !getTarget().getCenter().getPos().getWorld(true).isClientSide) {
                reloadRecipes();
            }

            // Re-register to the network, to force an update for all new recipes
            if (craftingNetwork != null) {
                craftingNetwork.addCraftingInterface(channelCrafting, this);
            }
        }

        public void setTarget(PartTarget target) {
            this.target = target;
        }

        public PartTarget getTarget() {
            return target;
        }

        public void setNetworks(@Nullable INetwork network, @Nullable ICraftingNetwork craftingNetwork,
                                @Nullable IPartNetwork partNetwork, int channel) {
            this.network = network;
            this.craftingNetwork = craftingNetwork;
            this.partNetwork = partNetwork;
            this.channel = channel;
            reloadRecipes();
            if (network != null) {
                this.getCraftingJobHandler().reRegisterObservers(network);
            }
        }

        public ICraftingNetwork getCraftingNetwork() {
            return craftingNetwork;
        }

        @Override
        public int getChannel() {
            return channel;
        }

        @Override
        public Collection<IRecipeDefinition> getRecipes() {
            return this.currentRecipes.values();
        }

        public Int2ObjectMap<IRecipeDefinition> getRecipesIndexed() {
            return currentRecipes;
        }

        @Override
        public boolean canScheduleCraftingJobs() {
            return getCraftingJobHandler().canScheduleCraftingJobs();
        }

        @Override
        public void scheduleCraftingJob(CraftingJob craftingJob) {
            getCraftingJobHandler().scheduleCraftingJob(craftingJob);
        }

        @Override
        public int getCraftingJobsCount() {
            return this.craftingJobHandler.getAllCraftingJobs().size();
        }

        @Override
        public Iterator<CraftingJob> getCraftingJobs() {
            return this.craftingJobHandler.getAllCraftingJobs().values().iterator();
        }

        @Override
        public Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> getPendingCraftingJobOutputs(int craftingJobId) {
            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pending = this.craftingJobHandler.getProcessingCraftingJobsPendingIngredients().get(craftingJobId);
            if (pending == null) {
                pending = Maps.newIdentityHashMap();
            }
            return pending;
        }

        @Override
        public CraftingJobStatus getCraftingJobStatus(ICraftingNetwork network, int channel, int craftingJobId) {
            return craftingJobHandler.getCraftingJobStatus(network, channel, craftingJobId);
        }

        @Override
        public void cancelCraftingJob(int channel, int craftingJobId) {
            craftingJobHandler.markCraftingJobFinished(craftingJobId);
        }

        @Override
        public PrioritizedPartPos getPosition() {
            return PrioritizedPartPos.of(getTarget().getCenter(), getPriority());
        }

        public CraftingJobHandler getCraftingJobHandler() {
            return craftingJobHandler;
        }

        public boolean shouldAddToCraftingNetwork() {
            return shouldAddToCraftingNetwork;
        }

        public void setShouldAddToCraftingNetwork(boolean shouldAddToCraftingNetwork) {
            this.shouldAddToCraftingNetwork = shouldAddToCraftingNetwork;
        }

        public List<IngredientInstanceWrapper<?, ?>> getInventoryOutputBuffer() {
            return inventoryOutputBuffer;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, INetwork network, IPartNetwork partNetwork, PartTarget target) {
            if (capability == CraftingInterfaceConfig.CAPABILITY) {
                return LazyOptional.of(() -> this).cast();
            }

            // Expose the whole storage
            if (this.network != null) {
                IngredientComponent<?, ?> ingredientComponent = IngredientComponent.getIngredientComponentForStorageCapability(capability);
                if (ingredientComponent != null) {
                    T cap = wrapStorageCapability(capability, ingredientComponent);
                    if (cap != null) {
                        return LazyOptional.of(() -> cap);
                    }
                }
            }

            return super.getCapability(capability, network, partNetwork, target);
        }

        protected <C, T, M> C wrapStorageCapability(Capability<C> capability, IngredientComponent<T, M> ingredientComponent) {
            IIngredientComponentStorage<T, M> storage = CraftingHelpers.getNetworkStorage(this.network, this.channelCrafting,
                    ingredientComponent, false);

            // Don't allow extraction, only insertion
            storage = new IngredientComponentStorageSlottedInsertProxy<>(storage);

            return ingredientComponent.getStorageWrapperHandler(capability).wrapStorage(storage);
        }

        @Override
        public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance) {
            this.getInventoryOutputBuffer().add(new IngredientInstanceWrapper<>(ingredientComponent, instance));
        }

        public void setIngredientComponentTargetSideOverride(IngredientComponent<?, ?> ingredientComponent, Direction side) {
            if (getTarget().getTarget().getSide() == side) {
                craftingJobHandler.setIngredientComponentTarget(ingredientComponent, null);
            } else {
                craftingJobHandler.setIngredientComponentTarget(ingredientComponent, side);
            }
            sendUpdate();
        }

        public Direction getIngredientComponentTargetSideOverride(IngredientComponent<?, ?> ingredientComponent) {
            Direction side = craftingJobHandler.getIngredientComponentTarget(ingredientComponent);
            if (side == null) {
                side = getTarget().getTarget().getSide();
            }
            return side;
        }

        public boolean isRecipeSlotValid(int slot) {
            return this.recipeSlotValidated.containsKey(slot);
        }

        @Nullable
        public IFormattableTextComponent getRecipeSlotUnlocalizedMessage(int slot) {
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
