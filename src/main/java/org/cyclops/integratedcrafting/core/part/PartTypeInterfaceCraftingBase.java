package org.cyclops.integratedcrafting.core.part;

import com.google.common.collect.Lists;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.IngredientInstanceWrapper;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobStatus;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.core.CraftingJobHandler;
import org.cyclops.integratedcrafting.core.CraftingProcessOverrides;
import org.cyclops.integratedcrafting.ingredient.storage.IngredientComponentStorageSlottedInsertProxy;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.network.NetworkCapability;
import org.cyclops.integrateddynamics.api.part.PartCapability;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.PrioritizedPartPos;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.part.PartStateBase;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * Base logic for parts that do crafting interfacing.
 * @author rubensworks
 */
public abstract class PartTypeInterfaceCraftingBase<P extends PartTypeInterfaceCraftingBase<P, S>, S extends PartTypeInterfaceCraftingBase.State<P, S>> extends PartTypeCraftingBase<P, S> {

    public PartTypeInterfaceCraftingBase(String name) {
        super(name);
    }

    @Override
    public void afterNetworkReAlive(INetwork network, IPartNetwork partNetwork, PartTarget target, S state) {
        super.afterNetworkReAlive(network, partNetwork, target, state);
        addTargetToNetwork(network, target, state, true);
    }

    @Override
    public void onNetworkRemoval(INetwork network, IPartNetwork partNetwork, PartTarget target, S state) {
        super.onNetworkRemoval(network, partNetwork, target, state);
        removeTargetFromNetwork(network, target.getTarget(), state);
    }

    @Override
    public void onNetworkAddition(INetwork network, IPartNetwork partNetwork, PartTarget target, S state) {
        super.onNetworkAddition(network, partNetwork, target, state);
        addTargetToNetwork(network, target, state, true);
    }

    @Override
    public void setPriorityAndChannel(INetwork network, IPartNetwork partNetwork, PartTarget target, S state, int priority, int channel) {
        // We need to do this because the crafting network is not automagically aware of the priority changes,
        // so we have to re-add it.
        removeTargetFromNetwork(network, target.getTarget(), state);
        super.setPriorityAndChannel(network, partNetwork, target, state, priority, channel);
        addTargetToNetwork(network, target, state, false);
    }

    protected NetworkCapability<ICraftingNetwork> getNetworkCapability() {
        return Capabilities.CraftingNetwork.NETWORK;
    }

    protected void addTargetToNetwork(INetwork network, PartTarget pos, S state, boolean initialize) {
        network.getCapability(getNetworkCapability())
                .ifPresent(craftingNetwork -> {
                    int channelCrafting = state.getChannelCrafting();
                    state.setTarget(pos);
                    state.setNetworks(network, craftingNetwork, NetworkHelpers.getPartNetworkChecked(network), channelCrafting, ValueDeseralizationContext.of(pos.getCenter().getPos().getLevel(true)), initialize);
                    state.setShouldAddToCraftingNetwork(true);
                });
    }

    protected void removeTargetFromNetwork(INetwork network, PartPos pos, S state) {
        ICraftingNetwork craftingNetwork = state.getCraftingNetwork();
        if (craftingNetwork != null) {
            network.getCapability(getNetworkCapability())
                    .ifPresent(n -> n.removeCraftingInterface(state.getChannelCrafting(), state));
        }
        state.setNetworks(null, null, null, -1, null, false);
        state.setTarget(null);
    }

    @Override
    public boolean isUpdate(S state) {
        return true;
    }

    @Override
    public int getMinimumUpdateInterval(S state) {
        return state.getDefaultUpdateInterval();
    }

    @Nullable
    protected static <T, M> IngredientInstanceWrapper<T, M> insertIntoNetwork(IngredientInstanceWrapper<T, M> wrapper,
                                                                              INetwork network, int channel) {
        IPositionedAddonsNetworkIngredients<T, M> storageNetwork = wrapper.getComponent()
                .getCapability(org.cyclops.integrateddynamics.Capabilities.PositionedAddonsNetworkIngredientsHandler.INGREDIENT)
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
    public void update(INetwork network, IPartNetwork partNetwork, PartTarget target, S state) {
        super.update(network, partNetwork, target, state);

        // Init network data in part state if it has not been done yet.
        // This can occur when the part chunk is being reloaded.
        if (state.getCraftingNetwork() == null) {
            addTargetToNetwork(network, target, state, false);
        }

        int channel = state.getChannelCrafting();

        // Update the network data in the part state
        if (state.shouldAddToCraftingNetwork()) {
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability()).orElse(null);
            craftingNetwork.addCraftingInterface(channel, state);
            state.setShouldAddToCraftingNetwork(false);
        }

        // Push any pending output ingredients into the network
        state.flushInventoryOutputBuffer(network);

        // Block job ticking if there still are outputs in our crafting result buffer.
        if (state.getInventoryOutputBuffer().isEmpty()) {
            // Tick the job handler
            PartPos targetPos = state.getTarget().getTarget();
            state.getCraftingJobHandler().update(network, channel, targetPos);
        }
    }

    @Override
    public void addDrops(PartTarget target, S state, List<ItemStack> itemStacks, boolean dropMainElement, boolean saveState) {
        // Drop any remaining output ingredients (only items)
        for (IngredientInstanceWrapper<?, ?> ingredientInstanceWrapper : state.getInventoryOutputBuffer()) {
            if (ingredientInstanceWrapper.getComponent() == IngredientComponent.ITEMSTACK) {
                itemStacks.add((ItemStack) ingredientInstanceWrapper.getInstance());
            }
        }
        state.getInventoryOutputBuffer().clear();

        // Drop buffered items from running crafting jobs (only items)
        for (CraftingJob craftingJob : state.getCraftingJobHandler().getAllCraftingJobs().values()) {
            for (ItemStack instance : craftingJob.getIngredientsStorageBuffer().getInstances(IngredientComponent.ITEMSTACK)) {
                itemStacks.add(instance);
            }
        }

        super.addDrops(target, state, itemStacks, dropMainElement, saveState);
    }

    public static abstract class State<P extends PartTypeInterfaceCraftingBase<P, S>, S extends PartTypeInterfaceCraftingBase.State<P, S>>
            extends PartStateBase<P> implements ICraftingInterface, ICraftingResultsSink {

        private final CraftingJobHandler craftingJobHandler;
        private final List<IngredientInstanceWrapper<?, ?>> inventoryOutputBuffer;

        private int channelCrafting = 0;
        private PartTarget target = null;
        protected INetwork network = null;
        protected IPartNetwork partNetwork = null;
        protected ICraftingNetwork craftingNetwork = null;
        private int channel = -1;
        protected ValueDeseralizationContext valueDeseralizationContext;
        private boolean shouldAddToCraftingNetwork = false;
        protected Player lastPlayer;

        public State() {
            this.craftingJobHandler = new CraftingJobHandler(1, true,
                    CraftingProcessOverrides.REGISTRY.getCraftingProcessOverrides(), this);
            this.inventoryOutputBuffer = Lists.newArrayList();
        }

        @Override
        public void writeToNBT(ValueDeseralizationContext valueDeseralizationContext, CompoundTag tag) {
            super.writeToNBT(valueDeseralizationContext, tag);

            ListTag instanceTags = new ListTag();
            for (IngredientInstanceWrapper instanceWrapper : inventoryOutputBuffer) {
                CompoundTag instanceTag = new CompoundTag();
                instanceTag.putString("component", IngredientComponent.REGISTRY.getKey(instanceWrapper.getComponent()).toString());
                instanceTag.put("instance", instanceWrapper.getComponent().getSerializer().serializeInstance(valueDeseralizationContext.holderLookupProvider(), instanceWrapper.getInstance()));
                instanceTags.add(instanceTag);
            }
            tag.put("inventoryOutputBuffer", instanceTags);

            this.craftingJobHandler.writeToNBT(valueDeseralizationContext.holderLookupProvider(), tag);
            tag.putInt("channelCrafting", channelCrafting);
        }

        @Override
        public void readFromNBT(ValueDeseralizationContext valueDeseralizationContext, CompoundTag tag) {
            super.readFromNBT(valueDeseralizationContext, tag);

            this.inventoryOutputBuffer.clear();
            for (Tag instanceTagRaw : tag.getList("inventoryOutputBuffer", Tag.TAG_COMPOUND)) {
                CompoundTag instanceTag = (CompoundTag) instanceTagRaw;
                String componentName = instanceTag.getString("component");
                IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.get(ResourceLocation.parse(componentName));
                this.inventoryOutputBuffer.add(new IngredientInstanceWrapper(component,
                        component.getSerializer().deserializeInstance(valueDeseralizationContext.holderLookupProvider(), instanceTag.get("instance"))));
            }

            this.craftingJobHandler.readFromNBT(valueDeseralizationContext.holderLookupProvider(), tag);
            this.channelCrafting = tag.getInt("channelCrafting");
        }

        @Override
        protected int getDefaultUpdateInterval() {
            return GeneralConfig.minCraftingInterfaceUpdateFreq;
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

        public void setTarget(PartTarget target) {
            this.target = target;
        }

        public PartTarget getTarget() {
            return target;
        }

        public void setNetworks(@Nullable INetwork network, @Nullable ICraftingNetwork craftingNetwork,
                                @Nullable IPartNetwork partNetwork, int channel,
                                @Nullable ValueDeseralizationContext valueDeseralizationContext,
                                boolean initialize) {
            this.network = network;
            this.craftingNetwork = craftingNetwork;
            this.partNetwork = partNetwork;
            this.channel = channel;
            this.valueDeseralizationContext = valueDeseralizationContext;
            reloadRecipes(initialize);
        }

        public void reloadRecipes(boolean initialize) {
            // Do nothing
        }

        public void setLastPlayer(Player lastPlayer) {
            this.lastPlayer = lastPlayer;
        }

        public ICraftingNetwork getCraftingNetwork() {
            return craftingNetwork;
        }

        @Override
        public int getChannel() {
            return channel;
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
        public void fillCraftingJobBufferFromStorage(CraftingJob craftingJob, Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter) {
            getCraftingJobHandler().fillCraftingJobBufferFromStorage(craftingJob, storageGetter);
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
        public List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> getPendingCraftingJobOutputs(int craftingJobId) {
            List<Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>>> pending = this.craftingJobHandler.getProcessingCraftingJobsPendingIngredients().get(craftingJobId);
            if (pending == null) {
                pending = Lists.newArrayList();
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
        public <T> Optional<T> getCapability(P partType, PartCapability<T> capability, INetwork network, IPartNetwork partNetwork, PartTarget target) {
            if (capability == Capabilities.CraftingInterface.PART) {
                return Optional.of((T) this);
            }

            // Expose the whole storage
            if (this.network != null) {
                IngredientComponent<?, ?> ingredientComponent = IngredientComponent.getIngredientComponentForStorageCapability(capability);
                if (ingredientComponent != null) {
                    T cap = wrapStorageCapability(capability, ingredientComponent);
                    if (cap != null) {
                        return Optional.of(cap);
                    }
                }
            }

            return super.getCapability(partType, capability, network, partNetwork, target);
        }

        protected <C, T, M> C wrapStorageCapability(PartCapability<C> capability, IngredientComponent<T, M> ingredientComponent) {
            IIngredientComponentStorage<T, M> storage = CraftingHelpers.getNetworkStorage(this.network, this.channelCrafting,
                    ingredientComponent, false);

            // Don't allow extraction, only insertion
            storage = new IngredientComponentStorageSlottedInsertProxy<>(storage);

            return ingredientComponent.getStorageWrapperHandler(capability).wrapStorage(storage);
        }

        @Override
        public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance) {
            this.getInventoryOutputBuffer().add(new IngredientInstanceWrapper<>(ingredientComponent, instance));

            // Try to flush buffer immediately
            if (this.network != null) {
                this.flushInventoryOutputBuffer(this.network);
            }
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

        public void flushInventoryOutputBuffer(INetwork network) {
            // Try to insert each ingredient in the buffer into the network.
            ListIterator<IngredientInstanceWrapper<?, ?>> outputBufferIt = this.getInventoryOutputBuffer().listIterator();
            while (outputBufferIt.hasNext()) {
                IngredientInstanceWrapper<?, ?> remainingInstance = outputBufferIt.next();

                // First try to give the ingredients to pending crafting jobs.
                remainingInstance = getCraftingJobHandler().beforeFlushIngredientToNetwork(remainingInstance, channelCrafting);

                // If none of the jobs need it, dump it into the network.
                remainingInstance = insertIntoNetwork(remainingInstance,
                        network, this.getChannelCrafting());
                if (remainingInstance == null) {
                    outputBufferIt.remove();
                } else {
                    outputBufferIt.set(remainingInstance);
                }
            }
        }
    }

}
