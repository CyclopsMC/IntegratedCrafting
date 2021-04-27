package org.cyclops.integratedcrafting.ingredient;

import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.LongNBT;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientSerializer;

public class IngredientSerializerLong implements IIngredientSerializer<Long, Boolean> {

    @Override
    public INBT serializeInstance(Long instance) {
        return LongNBT.valueOf(instance);
    }

    @Override
    public Long deserializeInstance(INBT tag) throws IllegalArgumentException {
        if (!(tag instanceof LongNBT)) {
            throw new IllegalArgumentException("This deserializer only accepts NBTTagInt");
        }
        return ((LongNBT) tag).getLong();
    }

    @Override
    public INBT serializeCondition(Boolean matchCondition) {
        return ByteNBT.valueOf((byte) (matchCondition ? 1 : 0));
    }

    @Override
    public Boolean deserializeCondition(INBT tag) throws IllegalArgumentException {
        if (!(tag instanceof ByteNBT)) {
            throw new IllegalArgumentException("This deserializer only accepts NBTTagByte");
        }
        return ((ByteNBT) tag).getByte() == 1;
    }

}
