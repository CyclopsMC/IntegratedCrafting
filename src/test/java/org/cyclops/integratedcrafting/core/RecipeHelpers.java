package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesList;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.ingredient.ComplexStack;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rubensworks
 */
public class RecipeHelpers {

    public static IRecipeDefinition newSimpleRecipe(List<ComplexStack> inputs, List<ComplexStack> outputs) {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> mapIn = Maps.newIdentityHashMap();
        mapIn.put(IngredientComponentStubs.COMPLEX, inputs
                        .stream()
                        .map(i -> new PrototypedIngredientAlternativesList<>(Lists.newArrayList(new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, i, ComplexStack.Match.EXACT))))
                        .collect(Collectors.toList()));
        Map<IngredientComponent<?, ?>, List<?>> mapOut = Maps.newIdentityHashMap();
        mapOut.put(IngredientComponentStubs.COMPLEX, outputs);
        return new RecipeDefinition(mapIn, new MixedIngredients(mapOut));
    }

}
