package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
     *
     * @param valueOutput
     * @param ingredients Ingredients.
     */
    public static void serialize(ValueOutput valueOutput, Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> ingredients) {
        ValueOutput.ValueOutputList missingIngredientsTagList = valueOutput.childrenList("v");
        for (Map.Entry<IngredientComponent<?, ?>, MissingIngredients<?, ?>> entry : ingredients.entrySet()) {
            ValueOutput missingIngredientsTag = missingIngredientsTagList.addChild();
            missingIngredientsTag.putString("component", entry.getKey().toString());
            ValueOutput.ValueOutputList elements = missingIngredientsTag.childrenList("elements");
            for (Element<?, ?> element : entry.getValue().getElements()) {
                ValueOutput elementsTag = elements.addChild();
                ValueOutput.ValueOutputList alternatives = elementsTag.childrenList("alternatives");
                for (PrototypedWithRequested<?, ?> alternative : element.getAlternatives()) {
                    ValueOutput alternativeTag = alternatives.addChild();
                    IPrototypedIngredient.serialize(alternativeTag.child("requestedPrototype"), alternative.getRequestedPrototype());
                    alternativeTag.putLong("quantityMissing", alternative.getQuantityMissing());
                }
                elementsTag.putBoolean("inputReusable", element.isInputReusable());
            }
        }
    }

    /**
     * Deserialize ingredients from NBT
     *
     * @param valueInput An NBT tag.
     * @return A new mixed ingredients instance.
     * @throws IllegalArgumentException If the given tag is invalid or does not contain data on the given ingredients.
     */
    public static Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> deserialize(ValueInput valueInput)
            throws IllegalArgumentException {
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> map = Maps.newIdentityHashMap();
        ValueInput.ValueInputList missingIngredientsTagList = valueInput.childrenList("v").orElseThrow();
        for (ValueInput input : missingIngredientsTagList) {
            String componentName = input.getString("component").orElseThrow();
            IngredientComponent<?, ?> component = IngredientComponent.REGISTRY.getValue(Identifier.parse(componentName));
            if (component == null) {
                throw new IllegalArgumentException("Could not find the ingredient component type " + componentName);
            }

            List<MissingIngredients.Element<?, ?>> elements = Lists.newArrayList();

            ValueInput.ValueInputList missingIngredientsTag = input.childrenList("elements").orElseThrow();
            for (ValueInput elementsTag : missingIngredientsTag) {
                List<MissingIngredients.PrototypedWithRequested<?, ?>> alternatives = Lists.newArrayList();
                for (ValueInput alternativeTag : elementsTag.childrenList("alternatives").orElseThrow()) {
                    IPrototypedIngredient<?, ?> requestedPrototype = IPrototypedIngredient.deserialize(alternativeTag.child("requestedPrototype").orElseThrow());
                    long quantityMissing = alternativeTag.getLong("quantityMissing").orElseThrow();
                    alternatives.add(new PrototypedWithRequested<>(requestedPrototype, quantityMissing));
                }
                boolean inputReusable = elementsTag.getBooleanOr("inputReusable", false);
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
        public int hashCode() {
            int result = alternatives.hashCode();
            result = 31 * result + Boolean.hashCode(inputReusable);
            return result;
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
        private long quantityMissing;

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

        public void setQuantityMissing(long quantityMissing) {
            this.quantityMissing = quantityMissing;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PrototypedWithRequested
                    && this.getRequestedPrototype().equals(((PrototypedWithRequested) obj).getRequestedPrototype())
                    && this.getQuantityMissing() == ((PrototypedWithRequested) obj).getQuantityMissing();
        }

        @Override
        public int hashCode() {
            int result = requestedPrototype.hashCode();
            result = 31 * result + Long.hashCode(quantityMissing);
            return result;
        }

        @Override
        public String toString() {
            return String.format("[Prototype: %s; missing: %s]", getRequestedPrototype(), getQuantityMissing());
        }
    }

}
