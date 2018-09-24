package org.cyclops.integratedcrafting.part.aspect;

import org.cyclops.integrateddynamics.api.part.aspect.IAspectWrite;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeFluidStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;

/**
 * @author rubensworks
 */
public class CraftingAspects {

    public static void load() {}

    public static final class Write {

        public static final IAspectWrite<ValueObjectTypeItemStack.ValueItemStack, ValueObjectTypeItemStack> ITEMSTACK_CRAFT =
                CraftingAspectWriteBuilders.BUILDER_ITEMSTACK
                        .withProperties(CraftingAspectWriteBuilders.PROPERTIES_CRAFTING)
                        .handle(CraftingAspectWriteBuilders.PROP_ITEMSTACK_CRAFTINGDATA)
                        .handle(CraftingAspectWriteBuilders.PROP_CRAFT())
                        .buildWrite();
        public static final IAspectWrite<ValueObjectTypeFluidStack.ValueFluidStack, ValueObjectTypeFluidStack> FLUIDSTACK_CRAFT =
                CraftingAspectWriteBuilders.BUILDER_FLUIDSTACK
                        .withProperties(CraftingAspectWriteBuilders.PROPERTIES_CRAFTING)
                        .handle(CraftingAspectWriteBuilders.PROP_FLUIDSTACK_CRAFTINGDATA)
                        .handle(CraftingAspectWriteBuilders.PROP_CRAFT())
                        .buildWrite();
        public static final IAspectWrite<ValueTypeInteger.ValueInteger, ValueTypeInteger> ENERGY_CRAFT =
                CraftingAspectWriteBuilders.BUILDER_INTEGER
                        .withProperties(CraftingAspectWriteBuilders.PROPERTIES_CRAFTING)
                        .handle(CraftingAspectWriteBuilders.PROP_ENERGY_CRAFTINGDATA)
                        .handle(CraftingAspectWriteBuilders.PROP_CRAFT())
                        .buildWrite();

    }

}
