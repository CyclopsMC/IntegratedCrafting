package org.cyclops.integratedcrafting.core;

import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;

import java.util.List;

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
