package org.cyclops.integratedcrafting.part.aspect;

import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.integratedcrafting.Capabilities;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.aspect.property.IAspectProperties;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.part.aspect.build.AspectBuilder;
import org.cyclops.integrateddynamics.core.part.aspect.build.IAspectValuePropagator;
import org.cyclops.integrateddynamics.part.aspect.read.AspectReadBuilders;

import java.util.Optional;

/**
 * @author rubensworks
 */
public class CraftingAspectReadBuilders {

    public static final class CraftingNetwork {

        public static final IAspectValuePropagator<Pair<PartTarget, IAspectProperties>, Pair<IAspectProperties, Optional<ICraftingNetwork>>> PROP_GET_CRAFTING_NETWORK = input -> {
            DimPos dimPos = input.getLeft().getTarget().getPos();
            INetwork network = NetworkHelpers.getNetwork(dimPos.getLevel(true), dimPos.getBlockPos(), input.getLeft().getTarget().getSide()).orElse(null);
            return Pair.of(input.getRight(), network != null ? network.getCapability(Capabilities.CraftingNetwork.NETWORK) : Optional.empty());
        };

        public static final AspectBuilder<ValueTypeList.ValueList, ValueTypeList, Pair<IAspectProperties, Optional<ICraftingNetwork>>>
                BUILDER_LIST = AspectReadBuilders.BUILDER_LIST
                .byMod(IntegratedCrafting._instance)
                .withProperties(AspectReadBuilders.Network.PROPERTIES)
                .handle(PROP_GET_CRAFTING_NETWORK, "network");

    }



}
