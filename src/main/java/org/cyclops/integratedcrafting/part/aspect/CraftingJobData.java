package org.cyclops.integratedcrafting.part.aspect;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.aspect.property.IAspectProperties;

/**
 * @author rubensworks
 */
public class CraftingJobData<T, M> {

    private final IAspectProperties properties;
    private final IngredientComponent<T, M> ingredientComponent;
    private final T instance;
    private final PartPos center;
    private final INetwork network;
    private final ICraftingNetwork craftingNetwork;

    public CraftingJobData(IAspectProperties properties, IngredientComponent<T, M> ingredientComponent, T instance, PartPos center) {
        this.properties = properties;
        this.ingredientComponent = ingredientComponent;
        this.instance = instance;
        this.center = center;
        this.network = CraftingHelpers.getNetworkChecked(center);
        this.craftingNetwork = CraftingHelpers.getCraftingNetwork(network);
    }

    public IAspectProperties getProperties() {
        return properties;
    }

    public IngredientComponent<T, M> getIngredientComponent() {
        return ingredientComponent;
    }

    public T getInstance() {
        return instance;
    }

    public PartPos getCenter() {
        return center;
    }

    public INetwork getNetwork() {
        return network;
    }

    public ICraftingNetwork getCraftingNetwork() {
        return craftingNetwork;
    }
}
