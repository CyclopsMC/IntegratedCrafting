package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.integratedcrafting.api.crafting.ICraftingResultsSink;
import org.cyclops.integratedcrafting.ingredient.ComplexStack;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for the partial-insertion accounting in {@link CraftingHelpers#insertIngredients}.
 *
 * Storages such as drawers and barrels have a fixed capacity per item type,
 * so they regularly accept only part of an inserted instance.
 *
 * @author rubensworks
 */
public class TestCraftingHelpersInsertIngredients {

    private static final ComplexStack CA010_ = new ComplexStack(ComplexStack.Group.A, 0, 10, null);

    /**
     * A storage that accepts at most a fixed quantity, and returns the rest as remainder.
     */
    private static class CappedStorage implements IIngredientComponentStorage<ComplexStack, Integer> {

        private int freeSpace;
        private int accepted = 0;

        public CappedStorage(int freeSpace) {
            this.freeSpace = freeSpace;
        }

        public int getAccepted() {
            return accepted;
        }

        @Override
        public IngredientComponent<ComplexStack, Integer> getComponent() {
            return IngredientComponentStubs.COMPLEX;
        }

        @Override
        public Iterator<ComplexStack> iterator() {
            return Lists.<ComplexStack>newArrayList().iterator();
        }

        @Override
        public Iterator<ComplexStack> iterator(@Nonnull ComplexStack instance, Integer matchCondition) {
            return iterator();
        }

        @Override
        public long getMaxQuantity() {
            return freeSpace;
        }

        @Override
        public ComplexStack insert(@Nonnull ComplexStack instance, boolean simulate) {
            int toAccept = Math.min(instance.getAmount(), freeSpace);
            if (!simulate) {
                freeSpace -= toAccept;
                accepted += toAccept;
            }
            int remainder = instance.getAmount() - toAccept;
            return remainder == 0 ? null : instance.withAmount(remainder);
        }

        @Override
        public ComplexStack extract(@Nonnull ComplexStack instance, Integer matchCondition, boolean simulate) {
            return null;
        }

        @Override
        public ComplexStack extract(long maxQuantity, boolean simulate) {
            return null;
        }
    }

    private static IMixedIngredients ingredientsOf(ComplexStack instance) {
        Map<IngredientComponent<?, ?>, List<?>> ingredientsRaw = Maps.newIdentityHashMap();
        ingredientsRaw.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(instance));
        return new MixedIngredients(ingredientsRaw);
    }

    @Test
    public void testPartialInsertionReportsOnlyTheRemainderAsRemaining() {
        // A storage with room for 4, receiving an instance of 10
        CappedStorage storage = new CappedStorage(4);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = component -> storage;

        IMixedIngredients remaining = CraftingHelpers.insertIngredients(ingredientsOf(CA010_), storageGetter, false);

        assertThat("the storage accepted 4", storage.getAccepted(), equalTo(4));
        assertThat("6 are reported as remaining",
                remaining.getInstances(IngredientComponentStubs.COMPLEX),
                equalTo(Lists.newArrayList(CA010_.withAmount(6))));
    }

    @Test
    public void testFullInsertionReportsNothingAsRemaining() {
        CappedStorage storage = new CappedStorage(10);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = component -> storage;

        IMixedIngredients remaining = CraftingHelpers.insertIngredients(ingredientsOf(CA010_), storageGetter, false);

        assertThat("the storage accepted everything", storage.getAccepted(), equalTo(10));
        assertThat("nothing is reported as remaining", remaining.isEmpty(), equalTo(true));
    }

    @Test
    public void testRefusedInsertionReportsEverythingAsRemaining() {
        CappedStorage storage = new CappedStorage(0);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = component -> storage;

        IMixedIngredients remaining = CraftingHelpers.insertIngredients(ingredientsOf(CA010_), storageGetter, false);

        assertThat("the storage accepted nothing", storage.getAccepted(), equalTo(0));
        assertThat("everything is reported as remaining",
                remaining.getInstances(IngredientComponentStubs.COMPLEX),
                equalTo(Lists.newArrayList(CA010_)));
    }

    /**
     * Whatever the storage refuses ends up in the failure sink,
     * so the accepted part plus the sunk part must add up to what went in.
     */
    @Test
    public void testPartialInsertionDoesNotDuplicateThroughTheFailureSink() {
        CappedStorage storage = new CappedStorage(4);
        Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter = component -> storage;

        List<Object> sunk = Lists.newArrayList();
        CraftingHelpers.insertIngredientsGuaranteed(ingredientsOf(CA010_), storageGetter, new ICraftingResultsSink() {
            @Override
            public <T, M> void addResult(IngredientComponent<T, M> ingredientComponent, T instance) {
                sunk.add(instance);
            }
        });

        int sunkAmount = sunk.stream().mapToInt(instance -> ((ComplexStack) instance).getAmount()).sum();
        assertThat("no ingredients are created out of thin air",
                storage.getAccepted() + sunkAmount, equalTo(10));
    }

}
