package org.cyclops.integratedcrafting.part.aspect;

import com.google.common.collect.Lists;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integrateddynamics.api.part.aspect.IAspectRead;
import org.cyclops.integrateddynamics.api.part.aspect.IAspectWrite;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeFluidStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.part.aspect.read.AspectReadBuilders;

import java.util.Iterator;
import java.util.List;

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

    public static final class Read {

        public static final class Network {

            public static final IAspectRead<ValueTypeList.ValueList, ValueTypeList> CRAFTING_JOBS =
                    CraftingAspectReadBuilders.CraftingNetwork.BUILDER_LIST
                            .handle((data) -> {
                                List<ValueObjectTypeRecipe.ValueRecipe> recipes = Lists.newArrayList();
                                if (data.getRight() != null) {
                                    int channel = data.getLeft().getValue(AspectReadBuilders.Network.PROPERTY_CHANNEL).getRawValue();
                                    Iterator<CraftingJob> it = data.getRight().getCraftingJobs(channel);
                                    while (it.hasNext()) {
                                        recipes.add(ValueObjectTypeRecipe.ValueRecipe.of(it.next().getRecipe().getRecipe()));
                                    }
                                }
                                return ValueTypeList.ValueList.ofList(ValueTypes.OBJECT_RECIPE, recipes);
                            })
                            .appendKind("craftingjobs")
                            .buildRead();

        }

    }

}
