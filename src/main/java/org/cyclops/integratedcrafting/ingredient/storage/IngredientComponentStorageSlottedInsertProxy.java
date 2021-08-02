package org.cyclops.integratedcrafting.ingredient.storage;

import com.google.common.collect.Iterators;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorageSlotted;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * An ingredient component storage that only accepts insertions,
 * and immediately forwards all insertions to another storage.
 *
 * Externally, this storage is exposed as inventory with a single empty slot.
 *
 * @author rubensworks
 */
public class IngredientComponentStorageSlottedInsertProxy<T, M> implements IIngredientComponentStorageSlotted<T, M> {

    private final IIngredientComponentStorage<T, M> storage;

    public IngredientComponentStorageSlottedInsertProxy(IIngredientComponentStorage<T, M> storage) {
        this.storage = storage;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public T getSlotContents(int slot) {
        return getComponent().getMatcher().getEmptyInstance();
    }

    @Override
    public long getMaxQuantity(int slot) {
        return this.storage.getMaxQuantity();
    }

    @Override
    public T insert(int slot, @Nonnull T instance, boolean simulate) {
        return storage.insert(instance, simulate);
    }

    @Override
    public T extract(int slot, long quantity, boolean simulate) {
        return getComponent().getMatcher().getEmptyInstance();
    }

    @Override
    public IngredientComponent<T, M> getComponent() {
        return this.storage.getComponent();
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.forArray(getComponent().getMatcher().getEmptyInstance());
    }

    @Override
    public Iterator<T> iterator(@Nonnull T instance, M matchCondition) {
        return iterator();
    }

    @Override
    public long getMaxQuantity() {
        return this.storage.getMaxQuantity();
    }

    @Override
    public T insert(@Nonnull T instance, boolean simulate) {
        return storage.insert(instance, simulate);
    }

    @Override
    public T extract(@Nonnull T instance, M matchCondition, boolean simulate) {
        return getComponent().getMatcher().getEmptyInstance();
    }

    @Override
    public T extract(long quantity, boolean simulate) {
        return getComponent().getMatcher().getEmptyInstance();
    }
}
