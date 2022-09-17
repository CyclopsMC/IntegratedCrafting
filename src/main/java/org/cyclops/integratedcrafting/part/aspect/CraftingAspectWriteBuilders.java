package org.cyclops.integratedcrafting.part.aspect;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeCraftingWriter;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.aspect.property.IAspectProperties;
import org.cyclops.integrateddynamics.api.part.aspect.property.IAspectPropertyTypeInstance;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeFluidStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.part.aspect.build.AspectBuilder;
import org.cyclops.integrateddynamics.core.part.aspect.build.IAspectValuePropagator;
import org.cyclops.integrateddynamics.core.part.aspect.property.AspectProperties;
import org.cyclops.integrateddynamics.core.part.aspect.property.AspectPropertyTypeInstance;
import org.cyclops.integrateddynamics.part.aspect.write.AspectWriteBuilders;

/**
 * @author rubensworks
 */
public class CraftingAspectWriteBuilders {

    public static final IAspectPropertyTypeInstance<ValueTypeInteger, ValueTypeInteger.ValueInteger> PROP_CHANNEL =
            new AspectPropertyTypeInstance<>(ValueTypes.INTEGER, "aspect.aspecttypes.integrateddynamics.integer.channel");
    public static final IAspectPropertyTypeInstance<ValueTypeBoolean, ValueTypeBoolean.ValueBoolean> PROP_IGNORE_STORAGE =
            new AspectPropertyTypeInstance<>(ValueTypes.BOOLEAN, "aspect.aspecttypes.integratedcrafting.boolean.ignorestorage");
    public static final IAspectPropertyTypeInstance<ValueTypeBoolean, ValueTypeBoolean.ValueBoolean> PROP_IGNORE_CRAFTING =
            new AspectPropertyTypeInstance<>(ValueTypes.BOOLEAN, "aspect.aspecttypes.integratedcrafting.boolean.ignorecrafting");
    public static final IAspectPropertyTypeInstance<ValueTypeBoolean, ValueTypeBoolean.ValueBoolean> PROP_CRAFT_MISSING =
            new AspectPropertyTypeInstance<>(ValueTypes.BOOLEAN, "aspect.aspecttypes.integratedcrafting.boolean.craftmissing");
    public static final IAspectPropertyTypeInstance<ValueTypeInteger, ValueTypeInteger.ValueInteger> PROP_CRAFT_AMOUNT =
            new AspectPropertyTypeInstance<>(ValueTypes.INTEGER, "aspect.aspecttypes.integratedcrafting.integer.craftamount");
    public static final IAspectPropertyTypeInstance<ValueTypeInteger, ValueTypeInteger.ValueInteger> PROP_CRAFT_DELAY =
            new AspectPropertyTypeInstance<>(ValueTypes.INTEGER, "aspect.aspecttypes.integratedcrafting.integer.craftdelay");
    public static final IAspectProperties PROPERTIES_CRAFTING_RECIPE = new AspectProperties(ImmutableList.<IAspectPropertyTypeInstance>of(
            PROP_CHANNEL,
            PROP_IGNORE_CRAFTING,
            PROP_CRAFT_MISSING,
            PROP_CRAFT_AMOUNT
    ));
    public static final IAspectProperties PROPERTIES_CRAFTING = new AspectProperties(ImmutableList.<IAspectPropertyTypeInstance>of(
            PROP_CHANNEL,
            PROP_IGNORE_STORAGE,
            PROP_IGNORE_CRAFTING,
            PROP_CRAFT_MISSING,
            PROP_CRAFT_DELAY
    ));
    static {
        PROPERTIES_CRAFTING_RECIPE.setValue(PROP_CHANNEL, ValueTypeInteger.ValueInteger.of(IPositionedAddonsNetworkIngredients.DEFAULT_CHANNEL));
        PROPERTIES_CRAFTING_RECIPE.setValue(PROP_IGNORE_CRAFTING, ValueTypeBoolean.ValueBoolean.of(false));
        PROPERTIES_CRAFTING_RECIPE.setValue(PROP_CRAFT_MISSING, ValueTypeBoolean.ValueBoolean.of(true));
        PROPERTIES_CRAFTING_RECIPE.setValue(PROP_CRAFT_AMOUNT, ValueTypeInteger.ValueInteger.of(1));

        PROPERTIES_CRAFTING.setValue(PROP_CHANNEL, ValueTypeInteger.ValueInteger.of(IPositionedAddonsNetworkIngredients.DEFAULT_CHANNEL));
        PROPERTIES_CRAFTING.setValue(PROP_IGNORE_STORAGE, ValueTypeBoolean.ValueBoolean.of(false));
        PROPERTIES_CRAFTING.setValue(PROP_IGNORE_CRAFTING, ValueTypeBoolean.ValueBoolean.of(false));
        PROPERTIES_CRAFTING.setValue(PROP_CRAFT_MISSING, ValueTypeBoolean.ValueBoolean.of(true));
        PROPERTIES_CRAFTING.setValue(PROP_CRAFT_DELAY, ValueTypeInteger.ValueInteger.of(0));
    }

    public static final AspectBuilder<ValueObjectTypeRecipe.ValueRecipe, ValueObjectTypeRecipe, Triple<PartTarget, IAspectProperties, IRecipeDefinition>>
            BUILDER_RECIPE = AspectWriteBuilders.BUILDER_RECIPE.byMod(IntegratedCrafting._instance)
            .appendKind("craft").handle(AspectWriteBuilders.PROP_GET_RECIPE);
    public static final AspectBuilder<ValueObjectTypeItemStack.ValueItemStack, ValueObjectTypeItemStack, Triple<PartTarget, IAspectProperties, ItemStack>>
            BUILDER_ITEMSTACK = AspectWriteBuilders.BUILDER_ITEMSTACK.byMod(IntegratedCrafting._instance)
            .appendKind("craft").handle(AspectWriteBuilders.PROP_GET_ITEMSTACK);
    public static final AspectBuilder<ValueObjectTypeFluidStack.ValueFluidStack, ValueObjectTypeFluidStack, Triple<PartTarget, IAspectProperties, FluidStack>>
            BUILDER_FLUIDSTACK = AspectWriteBuilders.BUILDER_FLUIDSTACK.byMod(IntegratedCrafting._instance)
            .appendKind("craft").handle(AspectWriteBuilders.PROP_GET_FLUIDSTACK);
    public static final AspectBuilder<ValueTypeInteger.ValueInteger, ValueTypeInteger, Triple<PartTarget, IAspectProperties, Integer>>
            BUILDER_INTEGER = AspectWriteBuilders.BUILDER_INTEGER.byMod(IntegratedCrafting._instance)
            .appendKind("craft").handle(AspectWriteBuilders.PROP_GET_INTEGER);

    public static final IAspectValuePropagator<Triple<PartTarget, IAspectProperties, ItemStack>, CraftingJobData<ItemStack, Integer>>
            PROP_ITEMSTACK_CRAFTINGDATA = input -> {
        PartTarget partTarget = input.getLeft();
        IAspectProperties properties = input.getMiddle();
        ItemStack instance = input.getRight();
        IngredientComponent<ItemStack, Integer> ingredientComponent = IngredientComponent.ITEMSTACK;
        return new CraftingJobData<>(properties, ingredientComponent, instance, partTarget.getCenter());
    };

    public static final IAspectValuePropagator<Triple<PartTarget, IAspectProperties, FluidStack>, CraftingJobData<FluidStack, Integer>>
            PROP_FLUIDSTACK_CRAFTINGDATA = input -> {
        PartTarget partTarget = input.getLeft();
        IAspectProperties properties = input.getMiddle();
        FluidStack instance = input.getRight();
        IngredientComponent<FluidStack, Integer> ingredientComponent = IngredientComponent.FLUIDSTACK;
        return new CraftingJobData<>(properties, ingredientComponent, instance, partTarget.getCenter());
    };

    public static final IAspectValuePropagator<Triple<PartTarget, IAspectProperties, Integer>, CraftingJobData<Long, Boolean>>
            PROP_ENERGY_CRAFTINGDATA = input -> {
        PartTarget partTarget = input.getLeft();
        IAspectProperties properties = input.getMiddle();
        Integer instance = input.getRight();
        IngredientComponent<Long, Boolean> ingredientComponent = IngredientComponent.ENERGY;
        // TODO: in next breaking change, migrate all of this to long-based values
        return new CraftingJobData<>(properties, ingredientComponent, (long) instance, partTarget.getCenter());
    };

    public static final IAspectValuePropagator<Triple<PartTarget, IAspectProperties, IRecipeDefinition>, Void> PROP_CRAFT_RECIPE = input -> {
        PartPos center = input.getLeft().getCenter();
        IAspectProperties properties = input.getMiddle();
        IRecipeDefinition recipe = input.getRight();
        if (recipe != null) {
            INetwork network = CraftingHelpers.getNetworkChecked(center);
            ICraftingNetwork craftingNetwork = CraftingHelpers.getCraftingNetworkChecked(network);
            if (craftingNetwork != null) {
                int channel = properties.getValue(PROP_CHANNEL).getRawValue();
                int amount = properties.getValue(PROP_CRAFT_AMOUNT).getRawValue();
                boolean ignoreCrafting = properties.getValue(PROP_IGNORE_CRAFTING).getRawValue();
                boolean craftMissing = properties.getValue(PROP_CRAFT_MISSING).getRawValue();

                if ((ignoreCrafting || !CraftingHelpers.isCrafting(craftingNetwork, channel, recipe))) {
                    CraftingHelpers.calculateAndScheduleCraftingJob(network, channel,
                            recipe, amount, craftMissing, true,
                            CraftingHelpers.getGlobalCraftingJobIdentifier(), null);
                }
            }
        }
        return null;
    };

    public static <T, M> IAspectValuePropagator<CraftingJobData<T, M>, Void> PROP_CRAFT() {
        return input -> {
            IngredientComponent<T, M> ingredientComponent = input.getIngredientComponent();
            IAspectProperties properties = input.getProperties();
            T instance = input.getInstance();

            M matchCondition = ingredientComponent.getMatcher().getExactMatchNoQuantityCondition();
            if (!ingredientComponent.getMatcher().isEmpty(instance)) {
                INetwork network = input.getNetwork();
                ICraftingNetwork craftingNetwork = input.getCraftingNetwork();
                if (craftingNetwork != null) {
                    int channel = properties.getValue(PROP_CHANNEL).getRawValue();
                    boolean ignoreStorage = properties.getValue(PROP_IGNORE_STORAGE).getRawValue();
                    boolean ignoreCrafting = properties.getValue(PROP_IGNORE_CRAFTING).getRawValue();
                    boolean craftMissing = properties.getValue(PROP_CRAFT_MISSING).getRawValue();
                    int craftDelay = properties.getValue(PROP_CRAFT_DELAY).getRawValue();

                    if ((ignoreStorage || !CraftingHelpers.hasStorageInstance(network, channel, ingredientComponent,
                            instance, ingredientComponent.getMatcher().getExactMatchCondition()))
                            && (ignoreCrafting || !CraftingHelpers.isCrafting(craftingNetwork, channel,
                                ingredientComponent, instance, matchCondition))) {
                        // Handle craft delay (only if we are checking storage)
                        boolean allowCraft;
                        if (craftDelay > 0 && !ignoreStorage) {
                            PartTypeCraftingWriter.State partState = (PartTypeCraftingWriter.State) PartHelpers.getPart(input.getCenter()).getState();
                            long initialTick = partState.getInitialTickCraftingTrigger();
                            long currentTick = input.getCenter().getPos().getLevel(true).getGameTime();
                            if (initialTick >= 0) {
                                if (currentTick - initialTick >= craftDelay) {
                                    partState.setInitialTickCraftingTrigger(-1);
                                    allowCraft = true;
                                } else {
                                    allowCraft = false;
                                }
                            } else {
                                partState.setInitialTickCraftingTrigger(currentTick);
                                allowCraft = false;
                            }
                        } else {
                            allowCraft = true;
                        }

                        // If delay check passed, trigger a new crafting job
                        if (allowCraft) {
                            CraftingHelpers.calculateAndScheduleCraftingJob(network, channel,
                                    ingredientComponent, instance, matchCondition, craftMissing, true,
                                    CraftingHelpers.getGlobalCraftingJobIdentifier(), null);
                        }
                    } else {
                        // Reset initial tick crafting trigger
                        if (craftDelay > 0) {
                            ((PartTypeCraftingWriter.State) PartHelpers.getPart(input.getCenter()).getState())
                                    .setInitialTickCraftingTrigger(-1);
                        }
                    }
                }
            }
            return null;
        };
    }

}
