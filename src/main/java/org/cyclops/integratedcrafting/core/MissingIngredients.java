package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
    public static CompoundTag serialize(Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> ingredients) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<IngredientComponent<?, ?>, MissingIngredients<?, ?>> entry : ingredients.entrySet()) {
            ListTag missingIngredientsTag = new ListTag();
            for (Element<?, ?> element : entry.getValue().getElements()) {
                ListTag elementsTag = new ListTag();
                for (PrototypedWithRequested<?, ?> alternative : element.getAlternatives()) {
                    CompoundTag alternativeTag = new CompoundTag();
                    alternativeTag.put("requestedPrototype", IPrototypedIngredient.serialize(alternative.getRequestedPrototype()));
                    alternativeTag.putLong("quantityMissing", alternative.getQuantityMissing());
                    alternativeTag.putBoolean("inputReusable", element.isInputReusable()); // Hack, should actually be one level higher, but this is for backwards-compat
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
    public static Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> deserialize(CompoundTag tag)
            throws IllegalArgumentException {
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> map = Maps.newIdentityHashMap();
        for (String componentName : tag.getAllKeys()) {
            IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.get(new ResourceLocation(componentName));
            if (component == null) {
                throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
            }

            List<MissingIngredients.Element<?, ?>> elements = Lists.newArrayList();

            ListTag missingIngredientsTag = tag.getList(componentName, Tag.TAG_LIST);
            for (int i = 0; i < missingIngredientsTag.size(); i++) {
                ListTag elementsTag = (ListTag) missingIngredientsTag.get(i);
                List<MissingIngredients.PrototypedWithRequested<?, ?>> alternatives = Lists.newArrayList();
                boolean inputReusable = false;
                for (int j = 0; j < elementsTag.size(); j++) {
                    CompoundTag alternativeTag = elementsTag.getCompound(j);
                    IPrototypedIngredient<?, ?> requestedPrototype = IPrototypedIngredient.deserialize(alternativeTag.getCompound("requestedPrototype"));
                    long quantityMissing = alternativeTag.getLong("quantityMissing");
                    inputReusable = alternativeTag.getBoolean("inputReusable");
                    alternatives.add(new PrototypedWithRequested<>(requestedPrototype, quantityMissing));
                }
                elements.add(new Element(alternatives, inputReusable));
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
        private final boolean inputReusable;

        public Element(List<MissingIngredients.PrototypedWithRequested<T, M>> alternatives, boolean inputReusable) {
            this.alternatives = alternatives;
            this.inputReusable = inputReusable;
        }

        public List<PrototypedWithRequested<T, M>> getAlternatives() {
            return alternatives;
        }

        public boolean isInputReusable() {
            return inputReusable;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Element
                    && this.getAlternatives().equals(((Element) obj).getAlternatives())
                    && this.isInputReusable() == ((Element) obj).isInputReusable();
        }

        @Override
        public String toString() {
            return getAlternatives().toString() + "::" + isInputReusable();
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
