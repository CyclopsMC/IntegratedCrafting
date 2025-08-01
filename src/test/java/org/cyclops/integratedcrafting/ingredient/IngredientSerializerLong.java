package org.cyclops.integratedcrafting.ingredient;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientSerializer;

public class IngredientSerializerLong implements IIngredientSerializer<Long, Boolean> {

    @Override
    public void serializeInstance(ValueOutput valueOutput, Long instance) {
        valueOutput.putLong("v", instance);
    }

    @Override
    public Long deserializeInstance(ValueInput valueInput) throws IllegalArgumentException {
        return valueInput.getLong("v").orElseThrow();
    }

    @Override
    public Tag serializeCondition(Boolean matchCondition) {
        return ByteTag.valueOf((byte) (matchCondition ? 1 : 0));
    }

    @Override
    public Boolean deserializeCondition(Tag tag) throws IllegalArgumentException {
        if (!(tag instanceof ByteTag)) {
            throw new IllegalArgumentException("This deserializer only accepts NBTTagByte");
        }
        return ((ByteTag) tag).byteValue() == 1;
    }

}
