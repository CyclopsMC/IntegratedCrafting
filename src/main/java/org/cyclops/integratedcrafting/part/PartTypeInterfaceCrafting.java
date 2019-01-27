package org.cyclops.integratedcrafting.part;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
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
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.TileHelpers;
import org.cyclops.cyclopscore.ingredient.storage.IngredientStorageHelpers;
import org.cyclops.cyclopscore.inventory.IGuiContainerProvider;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobStatus;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.capability.network.CraftingInterfaceConfig;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkConfig;
import org.cyclops.integratedcrafting.client.gui.GuiPartInterfaceCrafting;
import org.cyclops.integratedcrafting.client.gui.GuiPartInterfaceCraftingSettings;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.core.CraftingJobHandler;
import org.cyclops.integratedcrafting.core.CraftingProcessOverrides;
import org.cyclops.integratedcrafting.core.part.PartTypeCraftingBase;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.PrioritizedPartPos;
import org.cyclops.integrateddynamics.capability.network.PositionedAddonsNetworkIngredientsHandlerConfig;
import org.cyclops.integrateddynamics.core.client.gui.ExtendedGuiHandler;
import org.cyclops.integrateddynamics.core.evaluate.InventoryVariableEvaluator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.part.PartStateBase;
import org.cyclops.integrateddynamics.core.part.PartTypeConfigurable;
import org.cyclops.integrateddynamics.core.part.event.PartVariableDrivenVariableContentsUpdatedEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Interface for item handlers.
 * @author rubensworks
 */
public class PartTypeInterfaceCrafting extends PartTypeCraftingBase<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    private final IGuiContainerProvider settingsGuiProvider;

    public PartTypeInterfaceCrafting(String name) {
        super(name);
        getModGui().getGuiHandler().registerGUI((settingsGuiProvider = new PartTypeConfigurable.GuiProviderSettings(
                Helpers.getNewId(getModGui(), Helpers.IDType.GUI), getModGui()) {
            @Override
            public Class<? extends Container> getContainer() {
                return ContainerPartInterfaceCraftingSettings.class;
            }

            @Override
            public Class<? extends GuiScreen> getGui() {
                return GuiPartInterfaceCraftingSettings.class;
            }
        }), ExtendedGuiHandler.PART);
    }

    @Override
    public int getConsumptionRate(State state) {
        return state.getCraftingJobHandler().getProcessingCraftingJobs().size() * 5;
    }

    public IGuiContainerProvider getSettingsGuiProvider() {
        return settingsGuiProvider;
    }

    @Override
    protected PartTypeInterfaceCrafting.State constructDefaultState() {
        return new PartTypeInterfaceCrafting.State();
    }

    @Override
    public Class<? super PartTypeInterfaceCrafting> getPartTypeClass() {
        return PartTypeInterfaceCrafting.class;
    }

    @Override
    public Class<? extends GuiScreen> getGui() {
        return GuiPartInterfaceCrafting.class;
    }

    @Override
    public Class<? extends Container> getContainer() {
        return ContainerPartInterfaceCrafting.class;
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
        if (network.hasCapability(getNetworkCapability())) {
            int channelCrafting = state.getChannelCrafting();
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability());
            state.setTarget(pos);
            state.setNetworks(network, craftingNetwork, NetworkHelpers.getPartNetwork(network), channelCrafting);
            state.setShouldAddToCraftingNetwork(true);
        }
    }

    protected void removeTargetFromNetwork(INetwork network, PartPos pos, PartTypeInterfaceCrafting.State state) {
        ICraftingNetwork craftingNetwork = state.getCraftingNetwork();
        if (craftingNetwork != null && network.hasCapability(getNetworkCapability())) {
            network.getCapability(getNetworkCapability()).removeCraftingInterface(state.getChannelCrafting(), state);
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
        int channel = state.getChannelCrafting();

        // Update the network data in the part state
        if (state.shouldAddToCraftingNetwork()) {
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability());
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
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability());
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
                .getStorage(network);
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
        super.addDrops(target, state, itemStacks, dropMainElement, saveState);

        // Drop any remaining output ingredients (only items)
        for (IngredientInstanceWrapper<?, ?> ingredientInstanceWrapper : state.getInventoryOutputBuffer()) {
            if (ingredientInstanceWrapper.getComponent() == IngredientComponent.ITEMSTACK) {
                itemStacks.add((ItemStack) ingredientInstanceWrapper.getInstance());
            }
        }
        state.getInventoryOutputBuffer().clear();

        // Drop the stored variables
        for(int i = 0; i < state.getInventoryVariables().getSizeInventory(); i++) {
            ItemStack itemStack = state.getInventoryVariables().getStackInSlot(i);
            if(!itemStack.isEmpty()) {
                itemStacks.add(itemStack);
            }
        }
        state.getInventoryVariables().clear();
    }

    public static class State extends PartStateBase<PartTypeInterfaceCrafting>
            implements ICraftingInterface, ICraftingResultsSink {

        private final CraftingJobHandler craftingJobHandler;
        private final SimpleInventory inventoryVariables;
        private final List<InventoryVariableEvaluator<ValueObjectTypeRecipe.ValueRecipe>> variableEvaluators;
        private final List<IngredientInstanceWrapper<?, ?>> inventoryOutputBuffer;
        private final Int2ObjectMap<L10NHelpers.UnlocalizedString> recipeSlotMessages;
        private final Int2BooleanMap recipeSlotValidated;
        private final IntSet delayedRecipeReloads;
        private final Map<IVariable, Boolean> variableListeners;
        private int channelCrafting = 0;

        private final Int2ObjectMap<IRecipeDefinition> currentRecipes;
        private PartTarget target = null;
        private INetwork network = null;
        private ICraftingNetwork craftingNetwork = null;
        private IPartNetwork partNetwork = null;
        private int channel = -1;
        private boolean shouldAddToCraftingNetwork = false;
        private EntityPlayer lastPlayer;

        public State() {
            this.craftingJobHandler = new CraftingJobHandler(1,
                    CraftingProcessOverrides.REGISTRY.getCraftingProcessOverrides(), this);
            this.inventoryVariables = new SimpleInventory(9, "variables", 1);
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
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            inventoryVariables.writeToNBT(tag);

            NBTTagList instanceTags = new NBTTagList();
            for (IngredientInstanceWrapper instanceWrapper : inventoryOutputBuffer) {
                NBTTagCompound instanceTag = new NBTTagCompound();
                instanceTag.setString("component", instanceWrapper.getComponent().getRegistryName().toString());
                instanceTag.setTag("instance", instanceWrapper.getComponent().getSerializer().serializeInstance(instanceWrapper.getInstance()));
            }
            tag.setTag("inventoryOutputBuffer", instanceTags);

            this.craftingJobHandler.writeToNBT(tag);
            tag.setInteger("channelCrafting", channelCrafting);

            NBTTagCompound recipeSlotErrorsTag = new NBTTagCompound();
            for (Int2ObjectMap.Entry<L10NHelpers.UnlocalizedString> entry : this.recipeSlotMessages.int2ObjectEntrySet()) {
                recipeSlotErrorsTag.setTag(String.valueOf(entry.getIntKey()), entry.getValue().toNBT());
            }
            tag.setTag("recipeSlotMessages", recipeSlotErrorsTag);

            NBTTagCompound recipeSlotValidatedTag = new NBTTagCompound();
            for (Int2BooleanMap.Entry entry : this.recipeSlotValidated.int2BooleanEntrySet()) {
                recipeSlotValidatedTag.setBoolean(String.valueOf(entry.getIntKey()), entry.getBooleanValue());
            }
            tag.setTag("recipeSlotValidated", recipeSlotValidatedTag);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            inventoryVariables.readFromNBT(tag);

            this.inventoryOutputBuffer.clear();
            for (NBTBase instanceTagRaw : tag.getTagList("inventoryOutputBuffer", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound instanceTag = (NBTTagCompound) instanceTagRaw;
                String componentName = instanceTag.getString("component");
                IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.getValue(new ResourceLocation(componentName));
                this.inventoryOutputBuffer.add(new IngredientInstanceWrapper(component,
                        component.getSerializer().deserializeCondition(instanceTag.getTag("instance"))));
            }

            this.craftingJobHandler.readFromNBT(tag);
            this.channelCrafting = tag.getInteger("channelCrafting");

            this.recipeSlotMessages.clear();
            NBTTagCompound recipeSlotErrorsTag = tag.getCompoundTag("recipeSlotMessages");
            for (String slot : recipeSlotErrorsTag.getKeySet()) {
                L10NHelpers.UnlocalizedString unlocalizedString = new L10NHelpers.UnlocalizedString();
                unlocalizedString.fromNBT(recipeSlotErrorsTag.getCompoundTag(slot));
                this.recipeSlotMessages.put(Integer.parseInt(slot), unlocalizedString);
            }

            this.recipeSlotValidated.clear();
            NBTTagCompound recipeSlotValidatedTag = tag.getCompoundTag("recipeSlotValidated");
            for (String slot : recipeSlotValidatedTag.getKeySet()) {
                this.recipeSlotValidated.put(Integer.parseInt(slot), recipeSlotValidatedTag.getBoolean(slot));
            }
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
            for (int i = 0; i < getInventoryVariables().getSizeInventory(); i++) {
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
                for (int i = 0; i < getInventoryVariables().getSizeInventory(); i++) {
                    reloadRecipe(i);
                }
            }
        }

        private void setLocalErrors(int slot, List<L10NHelpers.UnlocalizedString> errors) {
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
                                if (isValid(recipe)) {
                                    this.currentRecipes.put(slot, recipe);
                                    this.recipeSlotValidated.put(slot, true);
                                    this.recipeSlotMessages.put(slot, new L10NHelpers.UnlocalizedString("gui.integratedcrafting.partinterface.slot.message.valid"));
                                } else {
                                    this.recipeSlotMessages.put(slot, new L10NHelpers.UnlocalizedString("gui.integratedcrafting.partinterface.slot.message.invalid"));
                                }
                            }
                        } else {
                            this.recipeSlotMessages.put(slot, new L10NHelpers.UnlocalizedString("gui.integratedcrafting.partinterface.slot.message.norecipe"));
                        }
                    } catch (EvaluationException e) {
                        this.recipeSlotMessages.put(slot, new L10NHelpers.UnlocalizedString(e.getLocalizedMessage()));
                    }
                } else {
                    this.recipeSlotMessages.put(slot, new L10NHelpers.UnlocalizedString("gui.integratedcrafting.partinterface.slot.message.norecipe"));
                }

                try {
                    IPartNetwork partNetwork = NetworkHelpers.getPartNetwork(network);
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

        public void setLastPlayer(EntityPlayer lastPlayer) {
            this.lastPlayer = lastPlayer;
        }

        private void delayedReloadRecipe(int slot) {
            this.delayedRecipeReloads.add(slot);
        }


        private boolean isValid(IRecipeDefinition recipe) {
            DimPos dimPos = getTarget().getTarget().getPos();
            EnumFacing side = getTarget().getTarget().getSide();
            IRecipeHandler recipeHandler = TileHelpers.getCapability(dimPos.getWorld(), dimPos.getBlockPos(), side, Capabilities.RECIPE_HANDLER);
            if (recipeHandler == null) {
                IBlockState blockState = dimPos.getWorld().getBlockState(dimPos.getBlockPos());
                recipeHandler = BlockCapabilities.getInstance().getCapability(blockState, Capabilities.RECIPE_HANDLER,
                        dimPos.getWorld(), dimPos.getBlockPos(), side);
            }
            if (recipeHandler != null) {
                IMixedIngredients simulatedOutput = recipeHandler.simulate(MixedIngredients.fromRecipeInput(recipe));
                if (simulatedOutput != null) {
                    return simulatedOutput.equals(recipe.getOutput());
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
            if (getTarget() != null && !getTarget().getCenter().getPos().getWorld().isRemote) {
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
        public boolean hasCapability(Capability<?> capability, IPartNetwork network, PartTarget target) {
            if (this.network != null) {
                IngredientComponent<?, ?> ingredientComponent = IngredientComponent.getIngredientComponentForStorageCapability(capability);
                if (ingredientComponent != null) {
                    if (CraftingHelpers.getNetworkStorage(this.network, this.channelCrafting,
                            ingredientComponent, false) != null) {
                        return true;
                    }
                }
            }
            return capability == CraftingInterfaceConfig.CAPABILITY || super.hasCapability(capability, network, target);
        }

        @Override
        public <T> T getCapability(Capability<T> capability, IPartNetwork network, PartTarget target) {
            if (capability == CraftingInterfaceConfig.CAPABILITY) {
                return CraftingInterfaceConfig.CAPABILITY.cast(this);
            }

            // Expose the whole storage
            if (this.network != null) {
                IngredientComponent<?, ?> ingredientComponent = IngredientComponent.getIngredientComponentForStorageCapability(capability);
                if (ingredientComponent != null) {
                    T cap = wrapStorageCapability(capability, ingredientComponent);
                    if (cap != null) {
                        return cap;
                    }
                }
            }

            return super.getCapability(capability, network, target);
        }

        protected <C, T, M> C wrapStorageCapability(Capability<C> capability, IngredientComponent<T, M> ingredientComponent) {
            IIngredientComponentStorage<T, M> storage = CraftingHelpers.getNetworkStorage(this.network, this.channelCrafting,
                    ingredientComponent, false);

            // Don't allow extraction, only insertion
            storage = IngredientStorageHelpers.wrapStorage(storage, true, true, false);

            return ingredientComponent.getStorageWrapperHandler(capability).wrapStorage(storage);
        }

        @Override
        public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance) {
            this.getInventoryOutputBuffer().add(new IngredientInstanceWrapper<>(ingredientComponent, instance));
        }

        public void setIngredientComponentTargetSideOverride(IngredientComponent<?, ?> ingredientComponent, EnumFacing side) {
            if (getTarget().getTarget().getSide() == side) {
                craftingJobHandler.setIngredientComponentTarget(ingredientComponent, null);
            } else {
                craftingJobHandler.setIngredientComponentTarget(ingredientComponent, side);
            }
            sendUpdate();
        }

        public EnumFacing getIngredientComponentTargetSideOverride(IngredientComponent<?, ?> ingredientComponent) {
            EnumFacing side = craftingJobHandler.getIngredientComponentTarget(ingredientComponent);
            if (side == null) {
                side = getTarget().getTarget().getSide();
            }
            return side;
        }

        public boolean isRecipeSlotValid(int slot) {
            return this.recipeSlotValidated.containsKey(slot);
        }

        @Nullable
        public L10NHelpers.UnlocalizedString getRecipeSlotUnlocalizedMessage(int slot) {
            return this.recipeSlotMessages.get(slot);
        }

        public IntSet getDelayedRecipeReloads() {
            return delayedRecipeReloads;
        }
    }
}
