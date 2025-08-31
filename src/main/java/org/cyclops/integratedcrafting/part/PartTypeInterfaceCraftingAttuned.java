package org.cyclops.integratedcrafting.part;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeHandler;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;
import org.cyclops.cyclopscore.helper.BlockHelpers;
import org.cyclops.cyclopscore.modcompat.commoncapabilities.BlockCapabilitiesHelpers;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
import org.cyclops.integrateddynamics.Capabilities;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.block.IgnoredBlockStatus;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.part.PartTypeBase;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Interface for auto crafting that reads out all available target machine recipes.
 * @author rubensworks
 */
public class PartTypeInterfaceCraftingAttuned extends PartTypeInterfaceCraftingBase<PartTypeInterfaceCraftingAttuned, PartTypeInterfaceCraftingAttuned.State> {

    public PartTypeInterfaceCraftingAttuned(String name) {
        super(name);
    }

    @Override
    public int getConsumptionRate(PartTypeInterfaceCraftingAttuned.State state) {
        return state.getCraftingJobHandler().getProcessingCraftingJobs().size() * GeneralConfig.interfaceCraftingAttunedBaseConsumption;
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
                return new ContainerPartInterfaceCraftingSettings(id, playerInventory, new SimpleContainer(0),
                        data.getRight(), Optional.of(data.getLeft()), data.getMiddle());
            }
        });
    }

    @Override
    public void writeExtraGuiData(FriendlyByteBuf packetBuffer, PartPos pos, ServerPlayer player) {
        super.writeExtraGuiDataSettings(packetBuffer, pos, player); // We show the settings directly.
    }

    @Override
    public Optional<MenuProvider> getContainerProviderSettings(PartPos pos) {
        return Optional.empty();
    }

    @Override
    protected PartTypeInterfaceCraftingAttuned.State constructDefaultState() {
        return new PartTypeInterfaceCraftingAttuned.State();
    }

    @Override
    protected Block createBlock(BlockConfig blockConfig) {
        return new IgnoredBlockStatus();
    }

    protected IgnoredBlockStatus.Status getStatus(State state) {
        IgnoredBlockStatus.Status status = IgnoredBlockStatus.Status.INACTIVE;
        if (state != null) {
            if (state.hasValidTarget()) {
                status = IgnoredBlockStatus.Status.ACTIVE;
            } else {
                status = IgnoredBlockStatus.Status.ERROR;
            }
        }

        return status;
    }

    @Override
    public BlockState getBlockState(IPartContainer partContainer, Direction side) {
        IgnoredBlockStatus.Status status = this.getStatus(partContainer != null ? (State)partContainer.getPartState(side) : null);
        return super.getBlockState(partContainer, side)
                .setValue(IgnoredBlockStatus.STATUS, status);
    }

    @Override
    public void loadTooltip(State state, List<Component> lines) {
        super.loadTooltip(state, lines);

        if (!state.hasValidTarget()) {
            lines.add(Component.translatable("parttype.integratedcrafting.interface_crafting_attuned.unsupported").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public boolean shouldTriggerBlockRenderUpdate(@Nullable State oldPartState, @Nullable State newPartState) {
        return super.shouldTriggerBlockRenderUpdate(oldPartState, newPartState) || this.getStatus(oldPartState) != this.getStatus(newPartState);
    }

    @Override
    public void onBlockNeighborChange(INetwork network, IPartNetwork partNetwork, PartTarget target, State state, BlockGetter world, Block neighbourBlock, BlockPos neighbourBlockPos) {
        boolean hadValidTarget = state.hasValidTarget();
        removeTargetFromNetwork(network, target.getTarget(), state);

        super.onBlockNeighborChange(network, partNetwork, target, state, world, neighbourBlock, neighbourBlockPos);

        addTargetToNetwork(network, target, state, false);
        if (hadValidTarget != state.hasValidTarget()) {
            BlockHelpers.markForUpdate(target.getCenter().getPos().getLevel(true), target.getCenter().getPos().getBlockPos());
        }
    }

    public static class State extends PartTypeInterfaceCraftingBase.State<PartTypeInterfaceCraftingAttuned, PartTypeInterfaceCraftingAttuned.State> {

        protected boolean hasValidTarget = false;
        private Collection<IRecipeDefinition> recipes;

        protected LazyOptional<IRecipeHandler> getTargetRecipeHandler() {
            PartPos target = getTarget().getTarget();
            return BlockCapabilitiesHelpers.getTileOrBlockCapability(target.getPos(), target.getSide(), Capabilities.RECIPE_HANDLER);
        }

        @Override
        public void setNetworks(@org.jetbrains.annotations.Nullable INetwork network, @org.jetbrains.annotations.Nullable ICraftingNetwork craftingNetwork, @org.jetbrains.annotations.Nullable IPartNetwork partNetwork, int channel, @org.jetbrains.annotations.Nullable ValueDeseralizationContext valueDeseralizationContext, boolean initialize) {
            super.setNetworks(network, craftingNetwork, partNetwork, channel, valueDeseralizationContext, initialize);

            this.hasValidTarget = getTargetRecipeHandler().isPresent();
            this.recipes = getTargetRecipeHandler()
                    .map(IRecipeHandler::getRecipes)
                    .orElse(Collections.emptyList());
            markDirty();
        }

        public boolean hasValidTarget() {
            return this.hasValidTarget;
        }

        @Override
        public void writeToNBT(CompoundTag tag) {
            super.writeToNBT(tag);
            tag.putBoolean("hasValidTarget", hasValidTarget);
        }

        @Override
        public void readFromNBT(ValueDeseralizationContext valueDeseralizationContext, CompoundTag tag) {
            super.readFromNBT(valueDeseralizationContext, tag);
            this.hasValidTarget = tag.getBoolean("hasValidTarget");
        }

        @Override
        public Collection<IRecipeDefinition> getRecipes() {
            return this.recipes;
        }
    }

}
