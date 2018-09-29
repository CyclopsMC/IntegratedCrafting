package org.cyclops.integratedcrafting.part;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.inventory.IGuiContainerProvider;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.capability.network.CraftingInterfaceConfig;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkConfig;
import org.cyclops.integratedcrafting.client.gui.GuiPartInterfaceCrafting;
import org.cyclops.integratedcrafting.core.CraftingJobHandler;
import org.cyclops.integratedcrafting.core.part.PartTypeCraftingBase;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCrafting;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.item.IVariableFacade;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.client.gui.ExtendedGuiHandler;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueHelpers;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.part.PartStateBase;
import org.cyclops.integrateddynamics.core.part.PartTypeConfigurable;
import org.cyclops.integrateddynamics.item.ItemVariable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
                Helpers.getNewId(getModGui(), Helpers.IDType.GUI), getModGui())), ExtendedGuiHandler.PART);
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
        return ContainerPartInterfaceCrafting.class; // TODO: allow setting channel in part settings
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
            state.setNetworks(network, craftingNetwork, NetworkHelpers.getPartNetwork(network), channelCrafting);
            state.setTarget(pos);
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
    public void update(INetwork network, IPartNetwork partNetwork, PartTarget target, State state) {
        super.update(network, partNetwork, target, state);
        int channel = state.getChannelCrafting();

        if (state.shouldAddToCraftingNetwork()) {
            ICraftingNetwork craftingNetwork = network.getCapability(getNetworkCapability());
            craftingNetwork.addCraftingInterface(channel, state);
            state.setShouldAddToCraftingNetwork(false);
        }

        PartPos targetPos = state.getTarget().getTarget();
        state.getCraftingJobHandler().update(network, channel, targetPos);
    }

    public static class State extends PartStateBase<PartTypeInterfaceCrafting> implements ICraftingInterface {

        private final CraftingJobHandler craftingJobHandler;
        private final SimpleInventory inventory;
        private int channelCrafting = 0;

        private final List<PrioritizedRecipe> currentRecipes;
        private PartTarget target = null;
        private ICraftingNetwork craftingNetwork = null;
        private IPartNetwork partNetwork = null;
        private int channel = -1;
        private boolean shouldAddToCraftingNetwork = false;

        public State() {
            this.craftingJobHandler = new CraftingJobHandler(1);
            this.inventory = new SimpleInventory(9, "variables", 1);
            this.inventory.addDirtyMarkListener(this);
            this.currentRecipes = Lists.newArrayList();
        }

        /**
         * @return The inner inventory
         */
        public SimpleInventory getInventory() {
            return this.inventory;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            inventory.writeToNBT(tag);
            this.craftingJobHandler.writeToNBT(tag);
            tag.setInteger("channelCrafting", channelCrafting);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            inventory.readFromNBT(tag);
            this.craftingJobHandler.readFromNBT(tag);
            this.channelCrafting = tag.getInteger("channelCrafting");
        }

        public void setChannelCrafting(int channelCrafting) {
            this.channelCrafting = channelCrafting;
            sendUpdate();
        }

        public int getChannelCrafting() {
            return channelCrafting;
        }

        public void reloadRecipes() {
            this.currentRecipes.clear();
            if (this.partNetwork != null) {
                SimpleInventory inventory = getInventory();
                for (int i = 0; i < inventory.getSizeInventory(); i++) {
                    ItemStack itemStack = inventory.getStackInSlot(i);
                    if (!itemStack.isEmpty()) {
                        IVariableFacade variableFacade = ItemVariable.getInstance().getVariableFacade(itemStack);
                        if (ValueHelpers.correspondsTo(variableFacade.getOutputType(), ValueTypes.OBJECT_RECIPE)) {
                            try {
                                IValue value = variableFacade.getVariable(this.partNetwork).getValue();
                                if (value.getType() == ValueTypes.OBJECT_RECIPE) {
                                    Optional<IRecipeDefinition> recipeWrapper = ((ValueObjectTypeRecipe.ValueRecipe) value).getRawValue();
                                    if (recipeWrapper.isPresent()) {
                                        IRecipeDefinition recipe = recipeWrapper.get();

                                        // First priority is the part priority, after that, the index inside this inventory.
                                        this.currentRecipes.add(new PrioritizedRecipe(recipe, getPriority(), i));
                                    }

                                }
                            } catch (EvaluationException e) {
                                // Ignore erroring recipes
                            }
                        }
                    }
                }
            }
            // TODO: show an error symbol for all variables that somehow failed?
        }

        @Override
        public void onDirty() {
            super.onDirty();

            // Unregister from the network, when all old recipes are still in place
            if (craftingNetwork != null) {
                craftingNetwork.removeCraftingInterface(channelCrafting, this);
            }

            // Recalculate recipes
            reloadRecipes();

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
        public Collection<PrioritizedRecipe> getRecipes() {
            return this.currentRecipes;
        }

        @Override
        public void scheduleCraftingJob(CraftingJob craftingJob) {
            getCraftingJobHandler().scheduleCraftingJob(craftingJob);
        }

        @Override
        public Iterator<CraftingJob> getCraftingJobs() {
            return Iterators.concat(
                    this.craftingJobHandler.getProcessingCraftingJobs().iterator(),
                    this.craftingJobHandler.getPendingCraftingJobs().iterator()
            );
        }

        @Override
        public Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> getPendingCraftingJobOutputs(CraftingJob craftingJob) {
            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pending = this.craftingJobHandler.getProcessingCraftingJobsPendingIngredients().get(craftingJob);
            if (pending == null) {
                pending = Maps.newIdentityHashMap();
            }
            return pending;
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

        @Override
        public boolean hasCapability(Capability<?> capability, IPartNetwork network, PartTarget target) {
            return capability == CraftingInterfaceConfig.CAPABILITY || super.hasCapability(capability, network, target);
        }

        @Override
        public <T> T getCapability(Capability<T> capability, IPartNetwork network, PartTarget target) {
            if (capability == CraftingInterfaceConfig.CAPABILITY) {
                return CraftingInterfaceConfig.CAPABILITY.cast(this);
            }
            return super.getCapability(capability, network, target);
        }
    }
}
