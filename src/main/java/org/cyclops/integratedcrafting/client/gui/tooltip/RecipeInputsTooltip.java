package org.cyclops.integratedcrafting.client.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;

import java.util.List;

/**
 * A tooltip component holding the inputs that a recipe requires.
 *
 * Every entry in {@link #inputs()} represents a single required input,
 * which holds all ingredients that are valid alternatives for that input.
 *
 * @param inputs The required inputs, with their alternatives.
 * @author rubensworks
 */
public record RecipeInputsTooltip(List<List<IPrototypedIngredient<?, ?>>> inputs) implements TooltipComponent {
}
