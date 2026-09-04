package org.cyclops.integratedcrafting.client.gui.tooltip;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;

import java.util.List;
import java.util.Map;

/**
 * Collects the inputs that a recipe requires, for showing them in a gui.
 * @author rubensworks
 */
public class RecipeInputs {

    /**
     * Determine the inputs that the given recipe requires.
     *
     * Inputs that only differ in their quantity are grouped into a single entry
     * whose quantity is the sum of theirs,
     * so that a recipe that takes the same ingredient multiple times
     * is shown as one entry with a higher quantity.
     *
     * @param recipe A recipe.
     * @return The required inputs, each with all the alternatives that satisfy it.
     */
    public static List<List<IPrototypedIngredient<?, ?>>> getGroupedInputs(IRecipeDefinition recipe) {
        List<List<IPrototypedIngredient<?, ?>>> inputs = Lists.newArrayList();
        for (IngredientComponent<?, ?> inputComponent : recipe.getInputComponents()) {
            addInputs(recipe, inputComponent, inputs);
        }
        return group(inputs);
    }

    protected static <T, M> void addInputs(IRecipeDefinition recipe, IngredientComponent<T, M> inputComponent,
                                           List<List<IPrototypedIngredient<?, ?>>> inputs) {
        IIngredientMatcher<T, M> matcher = inputComponent.getMatcher();
        for (IPrototypedIngredientAlternatives<T, M> alternatives : recipe.getInputs(inputComponent)) {
            List<IPrototypedIngredient<?, ?>> nonEmptyAlternatives = Lists.newArrayList();
            for (IPrototypedIngredient<T, M> alternative : alternatives.getAlternatives()) {
                if (!matcher.isEmpty(alternative.getPrototype())) {
                    nonEmptyAlternatives.add(alternative);
                }
            }
            if (!nonEmptyAlternatives.isEmpty()) {
                inputs.add(nonEmptyAlternatives);
            }
        }
    }

    protected static List<List<IPrototypedIngredient<?, ?>>> group(List<List<IPrototypedIngredient<?, ?>>> inputs) {
        List<List<IPrototypedIngredient<?, ?>>> groupedInputs = Lists.newArrayList();
        // Inputs are keyed on their alternatives without quantities, so that only their quantities may differ.
        Map<List<IPrototypedIngredient<?, ?>>, Integer> groupIndexes = Maps.newHashMap();
        for (List<IPrototypedIngredient<?, ?>> alternatives : inputs) {
            List<IPrototypedIngredient<?, ?>> groupKey = withoutQuantities(alternatives);
            Integer groupIndex = groupIndexes.get(groupKey);
            if (groupIndex == null) {
                groupIndexes.put(groupKey, groupedInputs.size());
                groupedInputs.add(alternatives);
            } else {
                groupedInputs.set(groupIndex, addQuantities(groupedInputs.get(groupIndex), alternatives));
            }
        }
        return groupedInputs;
    }

    protected static List<IPrototypedIngredient<?, ?>> withoutQuantities(List<IPrototypedIngredient<?, ?>> alternatives) {
        List<IPrototypedIngredient<?, ?>> withoutQuantities = Lists.newArrayListWithCapacity(alternatives.size());
        for (IPrototypedIngredient<?, ?> alternative : alternatives) {
            withoutQuantities.add(withQuantity(alternative, 1));
        }
        return withoutQuantities;
    }

    protected static List<IPrototypedIngredient<?, ?>> addQuantities(List<IPrototypedIngredient<?, ?>> alternatives,
                                                                    List<IPrototypedIngredient<?, ?>> addedAlternatives) {
        List<IPrototypedIngredient<?, ?>> summed = Lists.newArrayListWithCapacity(alternatives.size());
        for (int i = 0; i < alternatives.size(); i++) {
            IPrototypedIngredient<?, ?> alternative = alternatives.get(i);
            summed.add(withQuantity(alternative,
                    getQuantity(alternative) + getQuantity(addedAlternatives.get(i))));
        }
        return summed;
    }

    protected static <T, M> long getQuantity(IPrototypedIngredient<T, M> ingredient) {
        return ingredient.getComponent().getMatcher().getQuantity(ingredient.getPrototype());
    }

    protected static <T, M> IPrototypedIngredient<T, M> withQuantity(IPrototypedIngredient<T, M> ingredient, long quantity) {
        IngredientComponent<T, M> component = ingredient.getComponent();
        return new PrototypedIngredient<>(component,
                component.getMatcher().withQuantity(ingredient.getPrototype(), quantity), ingredient.getCondition());
    }

}
