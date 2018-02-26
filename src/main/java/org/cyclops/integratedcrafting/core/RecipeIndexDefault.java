package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndexModifiable;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.core.recipe.IngredientComponentIndexTypes;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A default implementation of {@link IRecipeIndex} and {@link IRecipeIndexModifiable}.
 * @author rubensworks
 */
public class RecipeIndexDefault implements IRecipeIndexModifiable {

    private final Map<IngredientComponent<?, ?>, IIngredientComponentRecipeIndex<?, ?>> recipeComponentIndexes;
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
    public <T, M> Set<PrioritizedRecipe> getRecipes(IngredientComponent<T, M> outputType, T output, M matchCondition, int limit) {
        IIngredientComponentRecipeIndex<?, ?> index = recipeComponentIndexes.get(outputType);
        if (index == null) {
            return Collections.emptySet();
        }
        return ((IIngredientComponentRecipeIndex<T, M>) index).getRecipes(output, matchCondition, limit);
    }

    @Nullable
    protected <T, M> IIngredientComponentRecipeIndex<T, M> initializeIndex(IngredientComponent<T, M> recipeComponent) {
        IIngredientComponentRecipeIndex.IFactory<T, M> factory = IngredientComponentIndexTypes.REGISTRY.getFactory(recipeComponent);
        if (factory != null) {
            return factory.newIndex();
        }
        return null;
    }

    @Override
    public void addRecipe(PrioritizedRecipe prioritizedRecipe) {
        recipes.add(prioritizedRecipe);
        for (IngredientComponent<?, ?> recipeComponent : prioritizedRecipe.getRecipe().getOutput().getComponents()) {
            IIngredientComponentRecipeIndex<?, ?> index = recipeComponentIndexes.computeIfAbsent(recipeComponent, this::initializeIndex);
            if (index != null) {
                index.addRecipe(prioritizedRecipe);
            }
        }
    }

    @Override
    public void removeRecipe(PrioritizedRecipe prioritizedRecipe) {
        recipes.remove(prioritizedRecipe);
        for (IngredientComponent<?, ?> recipeComponent : prioritizedRecipe.getRecipe().getOutput().getComponents()) {
            IIngredientComponentRecipeIndex<?, ?> index = recipeComponentIndexes.get(recipeComponent);
            if (index != null) {
                index.removeRecipe(prioritizedRecipe);
            }
        }
    }

}
