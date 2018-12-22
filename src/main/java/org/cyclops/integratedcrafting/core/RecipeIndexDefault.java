package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.datastructure.DistinctIterator;
import org.cyclops.cyclopscore.datastructure.MultitransformIterator;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientMapMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientHashMap;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndexModifiable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A default implementation of {@link IRecipeIndex} and {@link IRecipeIndexModifiable}.
 * @author rubensworks
 */
public class RecipeIndexDefault implements IRecipeIndexModifiable {

    private final Map<IngredientComponent<?, ?>, IIngredientMapMutable<?, ?, Set<IRecipeDefinition>>> recipeComponentIndexes;
    private final Set<IRecipeDefinition> recipes;

    public RecipeIndexDefault() {
        this.recipeComponentIndexes = Maps.newIdentityHashMap();
        this.recipes = Sets.newHashSet();
    }

    @Override
    public Set<IRecipeDefinition> getRecipes() {
        return Collections.unmodifiableSet(recipes);
    }

    @Override
    public <T, M> Iterator<IRecipeDefinition> getRecipes(IngredientComponent<T, M> outputType, T output, M matchCondition) {
        IIngredientMapMutable<?, ?, Set<IRecipeDefinition>> index = recipeComponentIndexes.get(outputType);
        if (index == null) {
            return Iterators.forArray();
        }
        return new DistinctIterator<>(MultitransformIterator.flattenIterableIterator(
                Iterators.transform(((IIngredientMapMutable<T, M, Set<IRecipeDefinition>>) index)
                        .iterator(output, matchCondition), (entry) -> entry.getValue())), true);
    }

    @Nullable
    protected <T, M> IIngredientMapMutable<T, M, Set<IRecipeDefinition>> initializeIndex(IngredientComponent<T, M> recipeComponent) {
        return new IngredientHashMap<>(recipeComponent);
    }

    @Override
    public void addRecipe(IRecipeDefinition prioritizedRecipe) {
        recipes.add(prioritizedRecipe);
        for (IngredientComponent<?, ?> recipeComponent : prioritizedRecipe.getOutput().getComponents()) {
            IIngredientMapMutable<?, ?, Set<IRecipeDefinition>> index = recipeComponentIndexes.computeIfAbsent(recipeComponent, this::initializeIndex);
            if (index != null) {
                addRecipeForComponent(index, prioritizedRecipe);
            }
        }
    }

    protected <T, M> void addRecipeForComponent(IIngredientMapMutable<T, M, Set<IRecipeDefinition>> index,
                                                IRecipeDefinition prioritizedRecipe) {
        for (T instance : prioritizedRecipe.getOutput().getInstances(index.getComponent())) {
            Set<IRecipeDefinition> set = index.get(instance);
            if (set == null) {
                set = Sets.newHashSet();
                index.put(instance, set);
            }
            set.add(prioritizedRecipe);
        }
    }

    @Override
    public void removeRecipe(IRecipeDefinition prioritizedRecipe) {
        recipes.remove(prioritizedRecipe);
        for (IngredientComponent<?, ?> recipeComponent : prioritizedRecipe.getOutput().getComponents()) {
            IIngredientMapMutable<?, ?, Set<IRecipeDefinition>> index = recipeComponentIndexes.get(recipeComponent);
            if (index != null) {
                removeRecipeForComponent(index, prioritizedRecipe);
            }
        }
    }

    protected <T, M> void removeRecipeForComponent(IIngredientMapMutable<T, M, Set<IRecipeDefinition>> index,
                                                   IRecipeDefinition prioritizedRecipe) {
        for (T instance : prioritizedRecipe.getOutput().getInstances(index.getComponent())) {
            Set<IRecipeDefinition> set = index.get(instance);
            if (set != null) {
                if (set.remove(prioritizedRecipe)) {
                    if (set.isEmpty()) {
                        index.remove(instance);
                    }
                }
            }
        }
    }

}
