package org.cyclops.integratedcrafting.core.recipe;

import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentIndex;
import org.cyclops.integratedcrafting.api.recipe.IIngredientComponentIndexTypeRegistry;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Default implementation of {@link IIngredientComponentIndexTypeRegistry}.
 * @author rubensworks
 */
public class IngredientComponentIndexTypeRegistry implements IIngredientComponentIndexTypeRegistry {

    private static final IngredientComponentIndexTypeRegistry INSTANCE = new IngredientComponentIndexTypeRegistry();

    private final Map<IngredientComponent<?, ?, ?>, IIngredientComponentIndex.IFactory<?, ?>> FACTORIES = Maps.newIdentityHashMap();


    private IngredientComponentIndexTypeRegistry() {

    }

    public static IngredientComponentIndexTypeRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public <T, M> IIngredientComponentIndex.IFactory<T, M> register(IngredientComponent<T, ?, M> recipeComponent, IIngredientComponentIndex.IFactory<T, M> indexFactory) {
        FACTORIES.put(recipeComponent, indexFactory);
        return indexFactory;
    }

    @Nullable
    @Override
    public <T, M> IIngredientComponentIndex.IFactory<T, M> getFactory(IngredientComponent<T, ?, M> recipeComponent) {
        return (IIngredientComponentIndex.IFactory<T, M>) FACTORIES.get(recipeComponent);
    }
}
