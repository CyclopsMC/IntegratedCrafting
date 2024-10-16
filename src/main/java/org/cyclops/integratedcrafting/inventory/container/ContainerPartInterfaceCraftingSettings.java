package org.cyclops.integratedcrafting.inventory.container;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.integratedcrafting.RegistryEntries;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerPartSettings;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * @author rubensworks
 */
public class ContainerPartInterfaceCraftingSettings extends ContainerPartSettings {

    private final int lastChannelInterfaceCraftingValueId;
    private final Map<IngredientComponent<?, ?>, Integer> targetSideOverrideValueIds;
    private final int lastDisableCraftingCheckValueId;
    private final int lastBlockingModeValueId;

    public ContainerPartInterfaceCraftingSettings(int id, Inventory playerInventory, RegistryFriendlyByteBuf packetBuffer) {
        this(id, playerInventory, new SimpleContainer(0), PartHelpers.readPartTarget(packetBuffer), Optional.empty(), PartHelpers.readPart(packetBuffer));
    }

    public ContainerPartInterfaceCraftingSettings(int id, Inventory playerInventory, Container inventory,
                                                  PartTarget target, Optional<IPartContainer> partContainer, IPartType partType) {
        super(RegistryEntries.CONTAINER_INTERFACE_CRAFTING_SETTINGS.get(), id, playerInventory, inventory, target, partContainer, partType);
        lastChannelInterfaceCraftingValueId = getNextValueId();
        targetSideOverrideValueIds = Maps.newIdentityHashMap();
        for (ResourceLocation key : Sets.newTreeSet(IngredientComponent.REGISTRY.keySet())) { // Consistently order keys
            IngredientComponent<?, ?> ingredientComponent = IngredientComponent.REGISTRY.get(key);
            targetSideOverrideValueIds.put(ingredientComponent, getNextValueId());
        }
        lastDisableCraftingCheckValueId = getNextValueId();
        lastBlockingModeValueId = getNextValueId();
    }

    @Override
    protected int getPlayerInventoryOffsetY() {
        return 174;
    }

    @Override
    protected void initializeValues() {
        super.initializeValues();
        ValueNotifierHelpers.setValue(this, lastChannelInterfaceCraftingValueId, ((PartTypeInterfaceCrafting.State) getPartState()).getChannelCrafting());
        for (IngredientComponent<?, ?> ingredientComponent : IngredientComponent.REGISTRY.stream().toList()) {
            ValueNotifierHelpers.setValue(this, getTargetSideOverrideValueId(ingredientComponent),
                    ((PartTypeInterfaceCrafting.State) getPartState()).getIngredientComponentTargetSideOverride(ingredientComponent).ordinal());
        }
        ValueNotifierHelpers.setValue(this, lastDisableCraftingCheckValueId, ((PartTypeInterfaceCrafting.State) getPartState()).isDisableCraftingCheck());
        ValueNotifierHelpers.setValue(this, lastBlockingModeValueId, ((PartTypeInterfaceCrafting.State) getPartState()).getCraftingJobHandler().isBlockingJobsMode());
    }

    public int getLastChannelInterfaceCraftingValueId() {
        return lastChannelInterfaceCraftingValueId;
    }

    public int getLastChannelInterfaceValue() {
        return ValueNotifierHelpers.getValueInt(this, lastChannelInterfaceCraftingValueId);
    }

    public int getTargetSideOverrideValueId(IngredientComponent<?, ?> ingredientComponent) {
        return targetSideOverrideValueIds.get(ingredientComponent);
    }

    @Nullable
    public Direction getTargetSideOverrideValue(IngredientComponent<?, ?> ingredientComponent) {
        int i = ValueNotifierHelpers.getValueInt(this, getTargetSideOverrideValueId(ingredientComponent));
        if (i < 0) {
            return getTarget().getTarget().getSide();
        }
        return Direction.values()[i];
    }

    public int getLastDisableCraftingCheckValueId() {
        return lastDisableCraftingCheckValueId;
    }

    public int getLastBlockingModeValueId() {
        return lastBlockingModeValueId;
    }

    public boolean getLastDisableCraftingCheckValue() {
        return ValueNotifierHelpers.getValueBoolean(this, lastDisableCraftingCheckValueId);
    }

    public boolean getLastBlockingModeValue() {
        return ValueNotifierHelpers.getValueBoolean(this, lastBlockingModeValueId);
    }

    public void setLastDisableCraftingCheckValue(boolean value) {
        ValueNotifierHelpers.setValue(this, lastDisableCraftingCheckValueId, value);
    }

    public void setLastBlockingModeValue(boolean value) {
        ValueNotifierHelpers.setValue(this, lastBlockingModeValueId, value);
    }

    @Override
    protected void updatePartSettings() {
        super.updatePartSettings();
        ((PartTypeInterfaceCrafting.State) getPartState()).setChannelCrafting(getLastChannelInterfaceValue());
        for (IngredientComponent<?, ?> ingredientComponent : IngredientComponent.REGISTRY.stream().toList()) {
            ((PartTypeInterfaceCrafting.State) getPartState()).setIngredientComponentTargetSideOverride(ingredientComponent,
                    getTargetSideOverrideValue(ingredientComponent));
        }
        ((PartTypeInterfaceCrafting.State) getPartState()).setDisableCraftingCheck(getLastDisableCraftingCheckValue());
        if (((PartTypeInterfaceCrafting.State) getPartState()).getCraftingJobHandler().setBlockingJobsMode(getLastBlockingModeValue())) {
            ((PartTypeInterfaceCrafting.State) getPartState()).sendUpdate();
            ((PartTypeInterfaceCrafting.State) getPartState()).onDirty();
        }
    }
}
