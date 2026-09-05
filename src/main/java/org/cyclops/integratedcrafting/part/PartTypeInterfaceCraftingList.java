package org.cyclops.integratedcrafting.part;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingVariableBase;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueTypeListProxy;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueHelpers;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.L10NValues;

import java.util.List;
import java.util.Optional;

/**
 * Interface for auto crafting that derives all its recipes from a single list variable.
 * @author rubensworks
 */
public class PartTypeInterfaceCraftingList extends PartTypeInterfaceCraftingVariableBase<PartTypeInterfaceCraftingList, PartTypeInterfaceCraftingList.State> {

    public static final int INVENTORY_SIZE = 1;

    public PartTypeInterfaceCraftingList(String name) {
        super(name);
    }

    @Override
    public int getConsumptionRate(State state) {
        return state.getCraftingJobHandler().getProcessingCraftingJobs().size() * GeneralConfig.interfaceCraftingListBaseConsumption;
    }

    @Override
    public IValueType<?> getSlotValueType() {
        return ValueTypes.LIST;
    }

    @Override
    protected PartTypeInterfaceCraftingList.State constructDefaultState() {
        return new PartTypeInterfaceCraftingList.State();
    }

    public static class State extends PartTypeInterfaceCraftingVariableBase.State<PartTypeInterfaceCraftingList, PartTypeInterfaceCraftingList.State> {

        // Slots for which the configured maximum truncated the list
        private final IntSet truncatedSlots = new IntArraySet();
        public State() {
            super(INVENTORY_SIZE);
        }

        @Override
        protected PartTypeInterfaceCraftingList getPartTypeInstance() {
            return PartTypes.INTERFACE_CRAFTING_LIST;
        }

        @Override
        protected int getDefaultUpdateInterval() {
            // Reading a whole list of recipes is more expensive than reading a single recipe,
            // and reader-backed list variables are invalidated on every reader tick.
            return GeneralConfig.minCraftingInterfaceListUpdateFreq;
        }

        @Override
        protected MutableComponent getRecipesValidMessage(int slot, int count) {
            if (this.truncatedSlots.contains(slot)) {
                return Component.translatable("gui.integratedcrafting.partinterface.slot.message.list.toolarge", count);
            }
            return super.getRecipesValidMessage(slot, count);
        }

        @Override
        protected List<IRecipeDefinition> extractRecipes(int slot, IValue value) throws EvaluationException {
            this.truncatedSlots.remove(slot);
            IValueTypeListProxy<IValueType<IValue>, IValue> list = ((ValueTypeList.ValueList) value).getRawValue();

            // Infinite lists can never be indexed into the crafting network.
            if (list.isInfinite()) {
                throw new EvaluationException(Component.translatable(
                        "gui.integratedcrafting.partinterface.slot.message.list.infinite"));
            }

            // An ANY-typed list is still allowed here, but then each element is checked separately below.
            IValueType<?> elementType = list.getValueType();
            if (!ValueHelpers.correspondsTo(elementType, ValueTypes.OBJECT_RECIPE)) {
                throw new EvaluationException(Component.translatable(L10NValues.VALUETYPE_ERROR_INVALIDLISTVALUETYPE,
                        Component.translatable(ValueTypes.OBJECT_RECIPE.getTranslationKey()),
                        Component.translatable(elementType.getTranslationKey())));
            }

            int length = list.getLength();
            int max = GeneralConfig.maxCraftingInterfaceListRecipes;
            if (max > 0 && length > max) {
                length = max;
                this.truncatedSlots.add(slot);
            }

            // Materialize the list only once: list proxies can be lazy views on remote positions,
            // for which each element access is a capability lookup.
            List<IRecipeDefinition> recipes = Lists.newArrayListWithExpectedSize(length);
            for (int i = 0; i < length; i++) {
                IValue element = list.get(i);
                if (element.getType() != ValueTypes.OBJECT_RECIPE) {
                    throw new EvaluationException(Component.translatable(L10NValues.VALUETYPE_ERROR_INVALIDLISTELEMENT,
                            Component.translatable(ValueTypes.OBJECT_RECIPE.getTranslationKey()),
                            Component.translatable(element.getType().getTranslationKey())));
                }
                Optional<IRecipeDefinition> recipe = ((ValueObjectTypeRecipe.ValueRecipe) element).getRawValue();
                recipe.ifPresent(recipes::add);
            }

            return deduplicate(recipes);
        }

    }
}
