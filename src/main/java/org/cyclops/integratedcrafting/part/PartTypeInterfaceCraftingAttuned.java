package org.cyclops.integratedcrafting.part;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.api.capability.Capabilities;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeHandler;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfigCommon;
import org.cyclops.cyclopscore.helper.IModHelpers;
import org.cyclops.cyclopscore.helper.IModHelpersNeoForge;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.RecipeKey;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedOffsets;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedRecipes;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettings;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                PartTypeInterfaceCraftingAttuned.State partState = (PartTypeInterfaceCraftingAttuned.State) data.getLeft().getPartState(pos.getSide());
                return new ContainerPartInterfaceCraftingAttunedRecipes(id, playerInventory, new SimpleContainer(0),
                        data.getRight(), Optional.of(data.getLeft()), data.getMiddle(),
                        partState.getAllRecipes(), partState.getDisabledRecipes(), partState.getRecipesVersion());
            }
        });
    }

    @Override
    public void writeExtraGuiData(RegistryFriendlyByteBuf packetBuffer, PartPos pos, ServerPlayer player) {
        super.writeExtraGuiDataSettings(packetBuffer, pos, player); // Writes the part position and part type
        ContainerPartInterfaceCraftingAttunedRecipes.writeRecipes(packetBuffer,
                (PartTypeInterfaceCraftingAttuned.State) PartHelpers.getPartContainerChecked(pos).getPartState(pos.getSide()));
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
        });
    }

    @Override
    public Optional<MenuProvider> getContainerProviderOffsets(PartPos pos) {
        return Optional.of(new MenuProvider() {

            @Override
            public MutableComponent getDisplayName() {
                return Component.translatable(getTranslationKey());
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
                Triple<IPartContainer, PartTypeBase, PartTarget> data = PartHelpers.getContainerPartConstructionData(pos);
                return new ContainerPartInterfaceCraftingAttunedOffsets(id, playerInventory, new SimpleContainer(0),
                        data.getRight(), Optional.of(data.getLeft()), data.getMiddle());
            }
        });
    }

    @Override
    protected PartTypeInterfaceCraftingAttuned.State constructDefaultState() {
        return new PartTypeInterfaceCraftingAttuned.State();
    }

    @Override
    protected Block createBlock(BlockConfigCommon<?> blockConfig, BlockBehaviour.Properties properties) {
        return new IgnoredBlockStatus(properties);
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
    public void onBlockNeighborChange(INetwork network, IPartNetwork partNetwork, PartTarget target, State state, BlockGetter world, @Nullable Direction side) {
        boolean isNeighbourTarget = side == null || target.getCenter().getPos().getBlockPos().relative(side).equals(target.getTarget().getPos().getBlockPos());
        boolean hadValidTarget = false;
        if (isNeighbourTarget) {
            hadValidTarget = state.hasValidTarget();
            removeTargetFromNetwork(network, target.getTarget(), state);
        }

        super.onBlockNeighborChange(network, partNetwork, target, state, world, side);

        if (isNeighbourTarget) {
            addTargetToNetwork(network, target, state, false);

            // Only trigger block update if really necessary.
            state.isDirtyAndReset();
            if (hadValidTarget != state.hasValidTarget()) {
                state.markDirty();
                IModHelpers.get().getBlockHelpers().markForUpdate(target.getCenter().getPos().getLevel(true), target.getCenter().getPos().getBlockPos());
            }
        }
    }

    public static class State extends PartTypeInterfaceCraftingBase.State<PartTypeInterfaceCraftingAttuned, PartTypeInterfaceCraftingAttuned.State> {

        protected boolean hasValidTarget = false;
        /**
         * All recipes that the target exposes, in the order the target exposes them.
         */
        private List<IRecipeDefinition> recipes = Collections.emptyList();
        /**
         * The subset of {@link #recipes} that is exposed to the crafting network.
         * This must always be kept in sync with the recipes that are registered in the crafting network,
         * as the network unregisters a crafting interface by iterating over {@link #getRecipes()}.
         */
        private List<IRecipeDefinition> recipesEnabled = Collections.emptyList();
        private Map<IRecipeDefinition, RecipeKey> recipeKeys = Maps.newIdentityHashMap();
        /**
         * The keys of all recipes that the player has disabled.
         * Unknown keys are retained, so that recipes stay disabled across pack updates
         * that temporarily remove them.
         */
        private final Set<RecipeKey> disabledRecipes = Sets.newLinkedHashSet();
        private int recipesVersion = 0;

        protected Optional<IRecipeHandler> getTargetRecipeHandler() {
            PartPos target = getTarget().getTarget();
            return IModHelpersNeoForge.get().getCapabilityHelpers().getCapability(target.getPos(), target.getSide(), Capabilities.RecipeHandler.BLOCK);
        }

        @Override
        public void setNetworks(@org.jetbrains.annotations.Nullable INetwork network, @org.jetbrains.annotations.Nullable ICraftingNetwork craftingNetwork, @org.jetbrains.annotations.Nullable IPartNetwork partNetwork, int channel, @org.jetbrains.annotations.Nullable ValueDeseralizationContext valueDeseralizationContext, boolean initialize) {
            super.setNetworks(network, craftingNetwork, partNetwork, channel, valueDeseralizationContext, initialize);

            this.hasValidTarget = getTargetRecipeHandler().isPresent();
            reloadRecipeIndex(valueDeseralizationContext);
            markDirty();
        }

        /**
         * Re-read all recipes from the target, and re-apply the disabled recipes filter on them.
         *
         * This is a no-op without a lookup provider,
         * as recipes that are not backed by a built-in recipe can not be keyed without one.
         * The previously read recipes are then retained,
         * which keeps {@link #getRecipes()} in sync with what is registered in the crafting network.
         *
         * @param valueDeseralizationContext The deserialization context, may be null.
         */
        protected void reloadRecipeIndex(@Nullable ValueDeseralizationContext valueDeseralizationContext) {
            if (valueDeseralizationContext == null) {
                return;
            }
            HolderLookup.Provider lookupProvider = valueDeseralizationContext.holderLookupProvider();

            List<IRecipeDefinition> recipes = Lists.newArrayList(getTargetRecipeHandler()
                    .map(IRecipeHandler::getRecipes)
                    .orElse(Collections.emptyList()));
            Map<IRecipeDefinition, RecipeKey> recipeKeys = Maps.newIdentityHashMap();
            for (IRecipeDefinition recipe : recipes) {
                recipeKeys.put(recipe, RecipeKey.of(lookupProvider, recipe));
            }

            this.recipes = recipes;
            this.recipeKeys = recipeKeys;
            this.recipesEnabled = filterEnabledRecipes();
            this.recipesVersion++;
        }

        protected List<IRecipeDefinition> filterEnabledRecipes() {
            if (this.disabledRecipes.isEmpty()) {
                return this.recipes;
            }
            List<IRecipeDefinition> enabled = Lists.newArrayListWithCapacity(this.recipes.size());
            for (IRecipeDefinition recipe : this.recipes) {
                if (!this.disabledRecipes.contains(this.recipeKeys.get(recipe))) {
                    enabled.add(recipe);
                }
            }
            return enabled;
        }

        public boolean hasValidTarget() {
            return this.hasValidTarget;
        }

        @Override
        public void serialize(ValueOutput valueOutput) {
            super.serialize(valueOutput);
            valueOutput.putBoolean("hasValidTarget", hasValidTarget);
            if (!this.disabledRecipes.isEmpty()) {
                ValueOutput.ValueOutputList disabledRecipesTag = valueOutput.childrenList("disabledRecipes");
                for (RecipeKey disabledRecipe : this.disabledRecipes) {
                    disabledRecipe.serialize(disabledRecipesTag.addChild());
                }
            }
        }

        @Override
        public void deserialize(ValueInput valueInput) {
            super.deserialize(valueInput);
            this.hasValidTarget = valueInput.getBooleanOr("hasValidTarget", false);
            // The exposed recipes are deliberately not re-filtered here:
            // they are recomputed by setNetworks, which is what keeps them in sync
            // with the recipes that are registered in the crafting network.
            this.disabledRecipes.clear();
            for (ValueInput disabledRecipeTag : valueInput.childrenListOrEmpty("disabledRecipes")) {
                this.disabledRecipes.add(RecipeKey.deserialize(disabledRecipeTag));
            }
        }

        @Override
        public Collection<IRecipeDefinition> getRecipes() {
            return this.recipesEnabled;
        }

        /**
         * @return All recipes that the target exposes, including the disabled ones.
         */
        public List<IRecipeDefinition> getAllRecipes() {
            return Collections.unmodifiableList(this.recipes);
        }

        /**
         * @return The keys of all recipes that are currently disabled.
         */
        public Set<RecipeKey> getDisabledRecipes() {
            return Collections.unmodifiableSet(this.disabledRecipes);
        }

        /**
         * @param recipe One of {@link #getAllRecipes()}.
         * @return The key of the given recipe, or null if it is not exposed by the target.
         */
        @Nullable
        public RecipeKey getRecipeKey(IRecipeDefinition recipe) {
            return this.recipeKeys.get(recipe);
        }

        /**
         * A counter that is incremented every time the recipes are re-read from the target.
         *
         * This allows guis to detect that the recipe list they are showing has become stale.
         *
         * @return The current recipe list version.
         */
        public int getRecipesVersion() {
            return this.recipesVersion;
        }

        /**
         * @param key A recipe key.
         * @return If the recipe with the given key is exposed to the crafting network.
         */
        public boolean isRecipeEnabled(RecipeKey key) {
            return !this.disabledRecipes.contains(key);
        }

        /**
         * Enable or disable the recipes with the given keys.
         *
         * Keys that do not correspond to a recipe of the current target are still stored,
         * so that the player's choice survives a temporary disappearance of the recipe.
         *
         * @param keys The keys of the recipes to update.
         * @param enabled If the recipes should be exposed to the crafting network.
         * @return If anything changed.
         */
        public boolean setRecipesEnabled(Collection<RecipeKey> keys, boolean enabled) {
            Set<RecipeKey> changedKeys = Sets.newHashSet();
            for (RecipeKey key : keys) {
                if (enabled ? this.disabledRecipes.remove(key) : this.disabledRecipes.add(key)) {
                    changedKeys.add(key);
                }
            }
            if (changedKeys.isEmpty()) {
                return false;
            }

            List<IRecipeDefinition> changedRecipes = Lists.newArrayList();
            for (IRecipeDefinition recipe : this.recipes) {
                if (changedKeys.contains(this.recipeKeys.get(recipe))) {
                    changedRecipes.add(recipe);
                }
            }

            // The exposed recipes must be updated before the network is notified,
            // as the network unregisters this interface by iterating over them.
            this.recipesEnabled = filterEnabledRecipes();

            // Apply the change to the network incrementally,
            // so that the network's recipe index stays in sync with our exposed recipes.
            ICraftingNetwork craftingNetwork = getCraftingNetwork();
            if (craftingNetwork != null && !shouldAddToCraftingNetwork()) {
                for (IRecipeDefinition changedRecipe : changedRecipes) {
                    if (enabled) {
                        craftingNetwork.addCraftingInterfaceRecipe(getChannelCrafting(), this, changedRecipe);
                    } else {
                        craftingNetwork.removeCraftingInterfaceRecipe(getChannelCrafting(), this, changedRecipe);
                    }
                }
            }

            markDirty();
            return true;
        }
    }

}
