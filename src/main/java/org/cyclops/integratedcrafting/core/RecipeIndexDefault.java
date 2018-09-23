package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.datastructure.MultitransformIterator;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientMapMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientHashMap;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndexModifiable;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;

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

    private final Map<IngredientComponent<?, ?>, IIngredientMapMutable<?, ?, Set<PrioritizedRecipe>>> recipeComponentIndexes;
    private final Set<PrioritizedRecipe> recipes;

    public RecipeIndexDefault() {
        this.recipeComponentIndexes = Maps.newIdentityHashMap();
        this.recipes = PrioritizedRecipe.newOutputSortedSet();
    }

    @Override
    public Set<PrioritizedRecipe> getRecipes() {
        return Collections.unmodifiableSet(recipes);
    }

    @Override
    public <T, M> Iterator<PrioritizedRecipe> getRecipes(IngredientComponent<T, M> outputType, T output, M matchCondition) {
        IIngredientMapMutable<?, ?, Set<PrioritizedRecipe>> index = recipeComponentIndexes.get(outputType);
        if (index == null) {
            return Iterators.forArray();
        }
        return MultitransformIterator.flattenIterableIterator(
                Iterators.transform(((IIngredientMapMutable<T, M, Set<PrioritizedRecipe>>) index)
                        .iterator(output, matchCondition), (entry) -> entry.getValue()));
    }

    @Nullable
    protected <T, M> IIngredientMapMutable<T, M, Set<PrioritizedRecipe>> initializeIndex(IngredientComponent<T, M> recipeComponent) {
        return new IngredientHashMap<>(recipeComponent);
    }

    @Override
    public void addRecipe(PrioritizedRecipe prioritizedRecipe) {
        recipes.add(prioritizedRecipe);
        for (IngredientComponent<?, ?> recipeComponent : prioritizedRecipe.getRecipe().getOutput().getComponents()) {
            IIngredientMapMutable<?, ?, Set<PrioritizedRecipe>> index = recipeComponentIndexes.computeIfAbsent(recipeComponent, this::initializeIndex);
            if (index != null) {
                addRecipeForComponent(index, prioritizedRecipe);
            }
        }
    }

    protected <T, M> void addRecipeForComponent(IIngredientMapMutable<T, M, Set<PrioritizedRecipe>> index,
                                                PrioritizedRecipe prioritizedRecipe) {
        for (T instance : prioritizedRecipe.getRecipe().getOutput().getInstances(index.getComponent())) {
            Set<PrioritizedRecipe> set = index.get(instance);
            if (set == null) {
                set = PrioritizedRecipe.newOutputSortedSet();
                index.put(instance, set);
            }
            set.add(prioritizedRecipe);
        }
    }

    @Override
    public void removeRecipe(PrioritizedRecipe prioritizedRecipe) {
        recipes.remove(prioritizedRecipe);
        for (IngredientComponent<?, ?> recipeComponent : prioritizedRecipe.getRecipe().getOutput().getComponents()) {
            IIngredientMapMutable<?, ?, Set<PrioritizedRecipe>> index = recipeComponentIndexes.get(recipeComponent);
            if (index != null) {
                removeRecipeForComponent(index, prioritizedRecipe);
            }
        }
    }

    protected <T, M> void removeRecipeForComponent(IIngredientMapMutable<T, M, Set<PrioritizedRecipe>> index,
                                                   PrioritizedRecipe prioritizedRecipe) {
        for (T instance : prioritizedRecipe.getRecipe().getOutput().getInstances(index.getComponent())) {
            Set<PrioritizedRecipe> set = index.get(instance);
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
