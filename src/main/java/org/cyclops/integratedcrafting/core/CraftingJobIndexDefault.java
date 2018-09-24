package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.datastructure.MultitransformIterator;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientMapMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientHashMap;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.recipe.ICraftingJobIndex;
import org.cyclops.integratedcrafting.api.recipe.ICraftingJobIndexModifiable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * A default implementation of {@link ICraftingJobIndex} and {@link ICraftingJobIndexModifiable}.
 * @author rubensworks
 */
public class CraftingJobIndexDefault implements ICraftingJobIndexModifiable {

    private final Map<IngredientComponent<?, ?>, IIngredientMapMutable<?, ?, Collection<CraftingJob>>> recipeComponentIndexes;
    private final Collection<CraftingJob> craftingJobs;

    public CraftingJobIndexDefault() {
        this.recipeComponentIndexes = Maps.newIdentityHashMap();
        this.craftingJobs = Sets.newIdentityHashSet();
    }

    @Override
    public Collection<CraftingJob> getCraftingJobs() {
        return Collections.unmodifiableCollection(craftingJobs);
    }

    @Override
    public <T, M> Iterator<CraftingJob> getCraftingJobs(IngredientComponent<T, M> outputType, T output, M matchCondition) {
        IIngredientMapMutable<?, ?, Collection<CraftingJob>> index = recipeComponentIndexes.get(outputType);
        if (index == null) {
            return Iterators.forArray();
        }
        return MultitransformIterator.flattenIterableIterator(
                Iterators.transform(((IIngredientMapMutable<T, M, Collection<CraftingJob>>) index)
                        .iterator(output, matchCondition), (entry) -> entry.getValue()));
    }

    @Nullable
    protected <T, M> IIngredientMapMutable<T, M, Collection<CraftingJob>> initializeIndex(IngredientComponent<T, M> recipeComponent) {
        return new IngredientHashMap<>(recipeComponent);
    }

    @Override
    public void addCraftingJob(CraftingJob craftingJob) {
        craftingJobs.add(craftingJob);
        for (IngredientComponent<?, ?> recipeComponent : craftingJob.getRecipe().getRecipe().getOutput().getComponents()) {
            IIngredientMapMutable<?, ?, Collection<CraftingJob>> index = recipeComponentIndexes.computeIfAbsent(recipeComponent, this::initializeIndex);
            if (index != null) {
                addCraftingJobForComponent(index, craftingJob);
            }
        }
    }

    protected <T, M> void addCraftingJobForComponent(IIngredientMapMutable<T, M, Collection<CraftingJob>> index,
                                                     CraftingJob craftingJob) {
        for (T instance : craftingJob.getRecipe().getRecipe().getOutput().getInstances(index.getComponent())) {
            Collection<CraftingJob> set = index.get(instance);
            if (set == null) {
                set = Sets.newIdentityHashSet();
                index.put(instance, set);
            }
            set.add(craftingJob);
        }
    }

    @Override
    public void removeCraftingJob(CraftingJob craftingJob) {
        craftingJobs.remove(craftingJob);
        for (IngredientComponent<?, ?> recipeComponent : craftingJob.getRecipe().getRecipe().getOutput().getComponents()) {
            IIngredientMapMutable<?, ?, Collection<CraftingJob>> index = recipeComponentIndexes.get(recipeComponent);
            if (index != null) {
                removeCraftingJobForComponent(index, craftingJob);
            }
        }
    }

    protected <T, M> void removeCraftingJobForComponent(IIngredientMapMutable<T, M, Collection<CraftingJob>> index,
                                                        CraftingJob craftingJob) {
        for (T instance : craftingJob.getRecipe().getRecipe().getOutput().getInstances(index.getComponent())) {
            Collection<CraftingJob> set = index.get(instance);
            if (set != null) {
                if (set.remove(craftingJob)) {
                    if (set.isEmpty()) {
                        index.remove(instance);
                    }
                }
            }
        }
    }

}
