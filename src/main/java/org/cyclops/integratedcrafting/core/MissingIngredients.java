package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;

import java.util.List;
import java.util.Map;

/**
 * A list with missing ingredients (non-slot-based).
 * @param <T> The instance type.
 * @param <M> The matching condition parameter, may be Void.
 * @author rubensworks
 */
public class MissingIngredients<T, M> {

    private final List<MissingIngredients.Element<T, M>> elements;

    public MissingIngredients(List<MissingIngredients.Element<T, M>> elements) {
        this.elements = elements;
    }

    public List<Element<T, M>> getElements() {
        return elements;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MissingIngredients && this.getElements().equals(((MissingIngredients) obj).getElements());
    }

    @Override
    public String toString() {
        return getElements().toString();
    }

    /**
     * Deserialize ingredients to NBT.
     * @param ingredients Ingredients.
     * @return An NBT representation of the given ingredients.
     */
    public static CompoundNBT serialize(Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> ingredients) {
        CompoundNBT tag = new CompoundNBT();
        for (Map.Entry<IngredientComponent<?, ?>, MissingIngredients<?, ?>> entry : ingredients.entrySet()) {
            ListNBT missingIngredientsTag = new ListNBT();
            for (Element<?, ?> element : entry.getValue().getElements()) {
                ListNBT elementsTag = new ListNBT();
                for (PrototypedWithRequested<?, ?> alternative : element.getAlternatives()) {
                    CompoundNBT alternativeTag = new CompoundNBT();
                    alternativeTag.put("requestedPrototype", IPrototypedIngredient.serialize(alternative.getRequestedPrototype()));
                    alternativeTag.putLong("quantityMissing", alternative.getQuantityMissing());
                    elementsTag.add(alternativeTag);
                }
                missingIngredientsTag.add(elementsTag);
            }
            tag.put(entry.getKey().getName().toString(), missingIngredientsTag);
        }
        return tag;
    }

    /**
     * Deserialize ingredients from NBT
     * @param tag An NBT tag.
     * @return A new mixed ingredients instance.
     * @throws IllegalArgumentException If the given tag is invalid or does not contain data on the given ingredients.
     */
    public static Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> deserialize(CompoundNBT tag)
            throws IllegalArgumentException {
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> map = Maps.newIdentityHashMap();
        for (String componentName : tag.getAllKeys()) {
            IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.getValue(new ResourceLocation(componentName));
            if (component == null) {
                throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
            }

            List<MissingIngredients.Element<?, ?>> elements = Lists.newArrayList();

            ListNBT missingIngredientsTag = tag.getList(componentName, Constants.NBT.TAG_LIST);
            for (int i = 0; i < missingIngredientsTag.size(); i++) {
                ListNBT elementsTag = (ListNBT) missingIngredientsTag.get(i);
                List<MissingIngredients.PrototypedWithRequested<?, ?>> alternatives = Lists.newArrayList();
                for (int j = 0; j < elementsTag.size(); j++) {
                    CompoundNBT alternativeTag = elementsTag.getCompound(j);
                    IPrototypedIngredient<?, ?> requestedPrototype = IPrototypedIngredient.deserialize(alternativeTag.getCompound("requestedPrototype"));
                    long quantityMissing = alternativeTag.getLong("quantityMissing");
                    alternatives.add(new PrototypedWithRequested<>(requestedPrototype, quantityMissing));
                }
                elements.add(new Element(alternatives));
            }

            MissingIngredients<?, ?> missingIngredients = new MissingIngredients(elements);
            map.put(component, missingIngredients);
        }
        return map;
    }

    /**
     * A list of alternatives for the given element.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter, may be Void.
     */
    public static class Element<T, M> {

        private final List<MissingIngredients.PrototypedWithRequested<T, M>> alternatives;

        public Element(List<MissingIngredients.PrototypedWithRequested<T, M>> alternatives) {
            this.alternatives = alternatives;
        }

        public List<PrototypedWithRequested<T, M>> getAlternatives() {
            return alternatives;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Element && this.getAlternatives().equals(((Element) obj).getAlternatives());
        }

        @Override
        public String toString() {
            return getAlternatives().toString();
        }
    }

    /**
     * A prototype with a missing quantity,
     * together with the total requested quantity.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter, may be Void.
     */
    public static class PrototypedWithRequested<T, M> {

        private final IPrototypedIngredient<T, M> requestedPrototype;
        private final long quantityMissing;

        public PrototypedWithRequested(IPrototypedIngredient<T, M> requestedPrototype, long quantityMissing) {
            this.requestedPrototype = requestedPrototype;
            this.quantityMissing = quantityMissing;
        }

        public IPrototypedIngredient<T, M> getRequestedPrototype() {
            return requestedPrototype;
        }

        public long getQuantityMissing() {
            return quantityMissing;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PrototypedWithRequested
                    && this.getRequestedPrototype().equals(((PrototypedWithRequested) obj).getRequestedPrototype())
                    && this.getQuantityMissing() == ((PrototypedWithRequested) obj).getQuantityMissing();
        }

        @Override
        public String toString() {
            return String.format("[Prototype: %s; missing: %s]", getRequestedPrototype(), getQuantityMissing());
        }
    }

}
