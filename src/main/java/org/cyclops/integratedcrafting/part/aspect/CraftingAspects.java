package org.cyclops.integratedcrafting.part.aspect;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.Level;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integrateddynamics.api.part.aspect.IAspectRead;
import org.cyclops.integrateddynamics.api.part.aspect.IAspectWrite;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeFluidStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeIngredients;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.part.aspect.read.AspectReadBuilders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rubensworks
 */
public class CraftingAspects {

    public static void load() {}

    public static final class Write {

        public static final IAspectWrite<ValueObjectTypeRecipe.ValueRecipe, ValueObjectTypeRecipe> RECIPE_CRAFT =
                CraftingAspectWriteBuilders.BUILDER_RECIPE
                        .withProperties(CraftingAspectWriteBuilders.PROPERTIES_CRAFTING_RECIPE)
                        .handle(CraftingAspectWriteBuilders.PROP_CRAFT_RECIPE)
                        .buildWrite();
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
                                    data.getRight().ifPresent(craftingNetwork -> {
                                        Iterator<CraftingJob> it = craftingNetwork.getCraftingJobs(channel);
                                        while (it.hasNext()) {
                                            recipes.add(ValueObjectTypeRecipe.ValueRecipe.of(it.next().getRecipe()));
                                        }
                                    });
                                }
                                return ValueTypeList.ValueList.ofList(ValueTypes.OBJECT_RECIPE, recipes);
                            })
                            .appendKind("craftingjobs")
                            .buildRead();

            public static final IAspectRead<ValueTypeList.ValueList, ValueTypeList> CRAFTING_INGREDIENTS =
                    CraftingAspectReadBuilders.CraftingNetwork.BUILDER_LIST
                            .handle((data) -> {
                                List<ValueObjectTypeIngredients.ValueIngredients> ingredients = Lists.newArrayList();
                                if (data.getRight() != null) {
                                    int channel = data.getLeft().getValue(AspectReadBuilders.Network.PROPERTY_CHANNEL).getRawValue();
                                    data.getRight().ifPresent(craftingNetwork -> {
                                        Iterator<CraftingJob> it = craftingNetwork.getCraftingJobs(channel);
                                        while (it.hasNext()) {
                                            CraftingJob crafingJob = it.next();
                                            ICraftingInterface craftingInterface = craftingNetwork.getCraftingJobInterface(crafingJob.getChannel(), crafingJob.getId());
                                            if (craftingInterface == null) {
                                                IntegratedCrafting.clog(Level.WARN, "Removed a zombie crafting job");
                                                it.remove();
                                                continue;
                                            }
                                            Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> pendingPrototypes = craftingInterface
                                                    .getPendingCraftingJobOutputs(crafingJob.getId());

                                            if (pendingPrototypes.isEmpty()) {
                                                continue;
                                            }

                                            Map<IngredientComponent<?, ?>, List<?>> pendingIngredients = Maps.newIdentityHashMap();
                                            for (IngredientComponent<?, ?> ingredientComponent : pendingPrototypes.keySet()) {
                                                pendingIngredients.put(ingredientComponent, pendingPrototypes
                                                        .get(ingredientComponent).stream()
                                                        .map(IPrototypedIngredient::getPrototype)
                                                        .collect(Collectors.toList()));
                                            }

                                            ingredients.add(ValueObjectTypeIngredients.ValueIngredients.of(new MixedIngredients(pendingIngredients)));
                                        }
                                    });
                                }
                                return ValueTypeList.ValueList.ofList(ValueTypes.OBJECT_INGREDIENTS, ingredients);
                            })
                            .appendKind("craftingingredients")
                            .buildRead();

            public static final IAspectRead<ValueTypeList.ValueList, ValueTypeList> RECIPES =
                    CraftingAspectReadBuilders.CraftingNetwork.BUILDER_LIST
                            .handle((data) -> {
                                List<ValueObjectTypeRecipe.ValueRecipe> ingredients = Lists.newArrayList();
                                if (data.getRight() != null) {
                                    int channel = data.getLeft().getValue(AspectReadBuilders.Network.PROPERTY_CHANNEL).getRawValue();
                                    data.getRight().ifPresent(craftingNetwork -> {
                                        IRecipeIndex recipeIndex = craftingNetwork.getRecipeIndex(channel);
                                        for (IRecipeDefinition recipe : recipeIndex.getRecipes()) {
                                            ingredients.add(ValueObjectTypeRecipe.ValueRecipe.of(recipe));
                                        }
                                    });
                                }
                                return ValueTypeList.ValueList.ofList(ValueTypes.OBJECT_RECIPE, ingredients);
                            })
                            .appendKind("recipes")
                            .buildRead();

        }

    }

}
