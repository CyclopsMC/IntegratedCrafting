package org.cyclops.integratedcrafting.core.part;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.cyclopscore.init.ModBaseNeoForge;
import org.cyclops.cyclopscore.network.PacketCodecs;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integrateddynamics.api.part.*;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerPartSettings;
import org.cyclops.integrateddynamics.core.part.PartTypeBase;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Base part for a crafting part.
 * @author rubensworks
 */
public abstract class PartTypeCraftingBase<P extends IPartType<P, S>, S extends IPartState<P>> extends PartTypeBase<P, S> {

    public PartTypeCraftingBase(String name) {
        super(name, new PartRenderPosition(0.1875F, 0.1875F, 0.625F, 0.625F));
    }

    @Override
    public ModBaseNeoForge<?> getMod() {
        return IntegratedCrafting._instance;
    }

    @Override
    public Optional<MenuProvider> getContainerProvider(PartPos pos) {
        return Optional.of(new MenuProvider() {

            @Override
            public Component getDisplayName() {
                return Component.translatable(getTranslationKey());
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
                Triple<IPartContainer, PartTypeBase, PartTarget> data = PartHelpers.getContainerPartConstructionData(pos);
                return new ContainerPartSettings(id, playerInventory, new SimpleContainer(0),
                        data.getRight(), Optional.of(data.getLeft()), data.getMiddle());
            }
        });
    }

    @Override
    public void writeExtraGuiDataSettings(RegistryFriendlyByteBuf packetBuffer, PartPos pos, ServerPlayer player) {
        PacketCodecs.write(packetBuffer, pos);
        packetBuffer.writeUtf(this.getUniqueName().toString());
    }

}
