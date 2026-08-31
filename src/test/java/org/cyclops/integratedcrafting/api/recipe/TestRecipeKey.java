package org.cyclops.integratedcrafting.api.recipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IPrototypedIngredientAlternatives;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.PrototypedIngredientAlternativesList;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author rubensworks
 */
public class TestRecipeKey {

    protected static IRecipeDefinition newRecipe(long input, long output, ResourceLocation recipeId) {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredientAlternatives<?, ?>>> inputs = Maps.newIdentityHashMap();
        inputs.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(
                new PrototypedIngredientAlternativesList<>(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.SIMPLE, input, true)))));
        Map<IngredientComponent<?, ?>, List<?>> outputs = Maps.newIdentityHashMap();
        outputs.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(output));
        return new RecipeDefinition(inputs, new MixedIngredients(outputs), recipeId);
    }

    /**
     * A structural tag as {@link IRecipeDefinition#serialize} produces it for recipes without a recipe id.
     */
    protected static CompoundTag newStructureTag(long output) {
        CompoundTag tag = new CompoundTag();
        CompoundTag input = new CompoundTag();
        input.put("cyclopscore:simple", new ListTag());
        tag.put("input", input);
        tag.put("inputReusable", new CompoundTag());
        CompoundTag outputTag = new CompoundTag();
        outputTag.putLong("cyclopscore:simple", output);
        tag.put("output", outputTag);
        return tag;
    }

    @Test
    public void testRecipeIdKeyRoundTrip() {
        RecipeKey key = RecipeKey.of(null, newRecipe(1, 2, ResourceLocation.parse("minecraft:chest")));
        assertThat(key.getRecipeId(), is("minecraft:chest"));
        assertThat(key.isStructural(), is(false));

        RecipeKey restored = RecipeKey.deserialize(key.serialize());
        assertThat(restored, is(key));
        assertThat(restored.hashCode(), is(key.hashCode()));
        assertThat(restored.getRecipeId(), is("minecraft:chest"));
        assertThat(restored.isStructural(), is(false));
    }

    @Test
    public void testRecipeIdKeysAreCompared() {
        RecipeKey key = RecipeKey.of(null, newRecipe(1, 2, ResourceLocation.parse("minecraft:chest")));

        // Recipes with the same id are the same key, even if their contents differ.
        assertThat(RecipeKey.of(null, newRecipe(3, 4, ResourceLocation.parse("minecraft:chest"))), is(key));

        assertThat(RecipeKey.of(null, newRecipe(1, 2, ResourceLocation.parse("minecraft:trapped_chest"))), is(not(key)));
    }

    @Test
    public void testStructuralKeyRoundTrip() {
        RecipeKey key = RecipeKey.deserialize(newStructureTag(2));
        assertThat(key.getRecipeId(), is(nullValue()));
        assertThat(key.isStructural(), is(true));

        RecipeKey restored = RecipeKey.deserialize(key.serialize());
        assertThat(restored, is(key));
        assertThat(restored.hashCode(), is(key.hashCode()));
        assertThat(restored.isStructural(), is(true));
    }

    @Test
    public void testStructuralKeysAreCompared() {
        assertThat(RecipeKey.deserialize(newStructureTag(2)), is(RecipeKey.deserialize(newStructureTag(2))));
        assertThat(RecipeKey.deserialize(newStructureTag(2)), is(not(RecipeKey.deserialize(newStructureTag(3)))));
    }

    @Test
    public void testStructuralKeyIsNeverEqualToRecipeIdKey() {
        assertThat(RecipeKey.deserialize(newStructureTag(2)), is(not(RecipeKey.ofRecipeId("minecraft:chest"))));
        assertThat(RecipeKey.ofRecipeId("minecraft:chest"), is(not(RecipeKey.deserialize(newStructureTag(2)))));
    }

    /**
     * Keys of recipes that no longer exist must stay usable,
     * as resolving them would throw, and losing them would silently re-enable a disabled recipe.
     */
    @Test
    public void testMissingRecipeKeyRoundTrip() {
        RecipeKey key = RecipeKey.ofRecipeId("somemod:removed_recipe");

        RecipeKey restored = RecipeKey.deserialize(key.serialize());
        assertThat(restored, is(key));
        assertThat(restored.getRecipeId(), is("somemod:removed_recipe"));
        assertThat(restored, is(not(RecipeKey.ofRecipeId("somemod:other_recipe"))));
    }

    @Test
    public void testSerializedKeyIsIndependentOfTheKey() {
        RecipeKey key = RecipeKey.deserialize(newStructureTag(2));
        CompoundTag serialized = key.serialize();
        serialized.putString("injected", "value");
        assertThat(RecipeKey.deserialize(key.serialize()), is(key));
    }

}
