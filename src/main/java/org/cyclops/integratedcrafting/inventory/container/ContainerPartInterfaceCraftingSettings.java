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
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;
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
        PartTypeInterfaceCraftingBase.State<?, ?> partState = (PartTypeInterfaceCraftingBase.State<?, ?>) getPartState();
        ValueNotifierHelpers.setValue(this, lastChannelInterfaceCraftingValueId, partState.getChannelCrafting());
        for (IngredientComponent<?, ?> ingredientComponent : IngredientComponent.REGISTRY.stream().toList()) {
            ValueNotifierHelpers.setValue(this, getTargetSideOverrideValueId(ingredientComponent),
                    partState.getIngredientComponentTargetSideOverride(ingredientComponent).ordinal());
        }
        if (partState instanceof PartTypeInterfaceCrafting.State stateNormal) {
            ValueNotifierHelpers.setValue(this, lastDisableCraftingCheckValueId, stateNormal.isDisableCraftingCheck());
        }
        ValueNotifierHelpers.setValue(this, lastBlockingModeValueId, partState.getCraftingJobHandler().isBlockingJobsMode());
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
        PartTypeInterfaceCraftingBase.State<?, ?> partState = (PartTypeInterfaceCraftingBase.State<?, ?>) getPartState();
        partState.setChannelCrafting(getLastChannelInterfaceValue());
        for (IngredientComponent<?, ?> ingredientComponent : IngredientComponent.REGISTRY.stream().toList()) {
            partState.setIngredientComponentTargetSideOverride(ingredientComponent,
                    getTargetSideOverrideValue(ingredientComponent));
        }
        if (partState instanceof PartTypeInterfaceCrafting.State stateNormal) {
            stateNormal.setDisableCraftingCheck(getLastDisableCraftingCheckValue());
        }
        if (partState.getCraftingJobHandler().setBlockingJobsMode(getLastBlockingModeValue())) {
            partState.sendUpdate();
            partState.onDirty();
        }
    }
}
