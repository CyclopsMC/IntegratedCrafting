package org.cyclops.integratedcrafting.ingredient;

import net.minecraft.network.chat.MutableComponent;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;

public class IngredientMatcherSimple implements IIngredientMatcher<Long, Boolean> {
    @Override
    public boolean isInstance(Object object) {
        return object instanceof Integer;
    }

    @Override
    public Boolean getAnyMatchCondition() {
        return false;
    }

    @Override
    public Boolean getExactMatchCondition() {
        return true;
    }

    @Override
    public Boolean getExactMatchNoQuantityCondition() {
        return false;
    }

    @Override
    public Boolean withCondition(Boolean matchCondition, Boolean with) {
        return matchCondition || with;
    }

    @Override
    public Boolean withoutCondition(Boolean matchCondition, Boolean without) {
        return matchCondition == without ? false : matchCondition;
    }

    @Override
    public boolean hasCondition(Boolean matchCondition, Boolean searchCondition) {
        return matchCondition == searchCondition;
    }

    @Override
    public boolean matches(Long a, Long b, Boolean matchCondition) {
        return !matchCondition || a.longValue() == b.longValue();
    }

    @Override
    public Long getEmptyInstance() {
        return 0L;
    }

    @Override
    public boolean isEmpty(Long instance) {
        return instance == 0L;
    }

    @Override
    public int hash(Long instance) {
        return (int) (long) instance;
    }

    @Override
    public Long copy(Long instance) {
        return instance;
    }

    @Override
    public long getQuantity(Long instance) {
        return instance;
    }

    @Override
    public Long withQuantity(Long instance, long quantity) throws ArithmeticException {
        return quantity;
    }

    @Override
    public long getMaximumQuantity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int conditionCompare(Boolean a, Boolean b) {
        return (a ? 1 : 0) - (b ? 1 : 0);
    }

    @Override
    public String localize(Long instance) {
        return String.valueOf(instance);
    }

    @Override
    public MutableComponent getDisplayName(Long aLong) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(Long aLong) {
        return aLong.toString();
    }

    @Override
    public int compare(Long o1, Long o2) {
        return (int) (o1 - o2);
    }
}
