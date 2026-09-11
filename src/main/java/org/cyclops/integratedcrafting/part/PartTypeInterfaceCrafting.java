package org.cyclops.integratedcrafting.part;

import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingVariableBase;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeRecipe;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;

import java.util.Collections;
import java.util.List;

/**
 * Interface for auto crafting, with one recipe variable per slot.
 * @author rubensworks
 */
public class PartTypeInterfaceCrafting extends PartTypeInterfaceCraftingVariableBase<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

    public static final int INVENTORY_SIZE = 9;

    public PartTypeInterfaceCrafting(String name) {
        super(name);
    }

    @Override
    public int getConsumptionRate(State state) {
        return state.getCraftingJobHandler().getProcessingCraftingJobs().size() * GeneralConfig.interfaceCraftingBaseConsumption;
    }

    @Override
    public IValueType<?> getSlotValueType() {
        return ValueTypes.OBJECT_RECIPE;
    }

    @Override
    protected PartTypeInterfaceCrafting.State constructDefaultState() {
        return new PartTypeInterfaceCrafting.State();
    }

    public static class State extends PartTypeInterfaceCraftingVariableBase.State<PartTypeInterfaceCrafting, PartTypeInterfaceCrafting.State> {

        public State() {
            super(INVENTORY_SIZE);
        }

        @Override
        protected PartTypeInterfaceCrafting getPartTypeInstance() {
            return PartTypes.INTERFACE_CRAFTING;
        }

        @Override
        protected List<IRecipeDefinition> extractRecipes(int slot, IValue value) {
            return ((ValueObjectTypeRecipe.ValueRecipe) value).getRawValue()
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }

    }
}
