package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IngredientComponentStorageEmpty;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.cyclopscore.ingredient.storage.IngredientComponentStorageCollectionWrapper;
import org.cyclops.integratedcrafting.ingredient.ComplexStack;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author rubensworks
 */
public class TestCraftingHelpers {

    private static final ComplexStack CA01_ = new ComplexStack(ComplexStack.Group.A, 0, 1, null);
    private static final ComplexStack CA02_ = new ComplexStack(ComplexStack.Group.A, 0, 2, null);
    private static final ComplexStack CA03_ = new ComplexStack(ComplexStack.Group.A, 0, 3, null);
    private static final ComplexStack CA04_ = new ComplexStack(ComplexStack.Group.A, 0, 4, null);
    private static final ComplexStack CA05_ = new ComplexStack(ComplexStack.Group.A, 0, 5, null);
    private static final ComplexStack CA06_ = new ComplexStack(ComplexStack.Group.A, 0, 6, null);
    private static final ComplexStack CA07_ = new ComplexStack(ComplexStack.Group.A, 0, 7, null);
    private static final ComplexStack CA08_ = new ComplexStack(ComplexStack.Group.A, 0, 8, null);
    private static final ComplexStack CA09_ = new ComplexStack(ComplexStack.Group.A, 0, 9, null);
    private static final ComplexStack CA010_ = new ComplexStack(ComplexStack.Group.A, 0, 10, null);

    private static final ComplexStack CB02_ = new ComplexStack(ComplexStack.Group.B, 0, 2, null);
    private static final ComplexStack CA91B = new ComplexStack(ComplexStack.Group.A, 9, 1, ComplexStack.Tag.B);
    private static final ComplexStack CA01B = new ComplexStack(ComplexStack.Group.A, 0, 1, ComplexStack.Tag.B);

    private IIngredientComponentStorage<ComplexStack, Integer> storageEmpty;
    private IIngredientComponentStorage<ComplexStack, Integer> storageValid;
    private IIngredientComponentStorage<ComplexStack, Integer> storageValidB;
    private IIngredientComponentStorage<ComplexStack, Integer> storageValidMore;
    private IIngredientComponentStorage<ComplexStack, Integer> storageValidMany;

    private IRecipeDefinition recipeEmpty;
    private IRecipeDefinition recipeSimple1;
    private IRecipeDefinition recipeSimple3;
    private IRecipeDefinition recipeSimple1Alt;
    private IRecipeDefinition recipeComplex;
    private IRecipeDefinition recipeEquals;

    @Before
    public void beforeEach() {
        storageEmpty = new IngredientComponentStorageEmpty<>(IngredientComponentStubs.COMPLEX);

        storageValid = new IngredientComponentStorageCollectionWrapper<>(
                new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storageValid.insert(CA01_, false);
        storageValid.insert(CB02_, false);
        storageValid.insert(CA91B, false);

        storageValidB = new IngredientComponentStorageCollectionWrapper<>(
                new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storageValidB.insert(CB02_, false);

        storageValidMore = new IngredientComponentStorageCollectionWrapper<>(
                new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storageValidMore.insert(CA03_, false);

        storageValidMany = new IngredientComponentStorageCollectionWrapper<>(
                new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storageValidMany.insert(CA09_, false);

        recipeEmpty = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(Maps.newIdentityHashMap()));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapSimple1 = Maps.newIdentityHashMap();
        mapSimple1.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                )
        ));
        recipeSimple1 = new RecipeDefinition(mapSimple1, new MixedIngredients(Maps.newIdentityHashMap()));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapSimple3 = Maps.newIdentityHashMap();
        mapSimple3.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA91B, ComplexStack.Match.EXACT)
                )
        ));
        recipeSimple3 = new RecipeDefinition(mapSimple3, new MixedIngredients(Maps.newIdentityHashMap()));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapSimple1Alt = Maps.newIdentityHashMap();
        mapSimple1Alt.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT)
                )
        ));
        recipeSimple1Alt = new RecipeDefinition(mapSimple1Alt, new MixedIngredients(Maps.newIdentityHashMap()));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapComplex = Maps.newIdentityHashMap();
        mapComplex.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, null, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA09_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA91B, ComplexStack.Match.EXACT)
                )
        ));
        recipeComplex = new RecipeDefinition(mapComplex, new MixedIngredients(Maps.newIdentityHashMap()));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapEquals = Maps.newIdentityHashMap();
        mapEquals.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA02_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                )
        ));
        recipeEquals = new RecipeDefinition(mapEquals, new MixedIngredients(Maps.newIdentityHashMap()));
    }

    /* ------------ getIngredientRecipeInputs ------------ */

    @Test
    public void testGetIngredientRecipeInputsEmptyStorageEmptyRecipe() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageEmpty, IngredientComponentStubs.COMPLEX, recipeEmpty, true),
                nullValue());
    }

    @Test
    public void testGetIngredientRecipeInputsEmptyStorageSimpleRecipe1() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageEmpty, IngredientComponentStubs.COMPLEX, recipeSimple1, true),
                nullValue());
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipe1() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValid, IngredientComponentStubs.COMPLEX, recipeSimple1, true),
                equalTo(Lists.newArrayList(CA01_)));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipe3() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValid, IngredientComponentStubs.COMPLEX, recipeSimple3, true),
                equalTo(Lists.newArrayList(CA01_, CB02_, CA91B)));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipe1Alt() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValidB, IngredientComponentStubs.COMPLEX, recipeSimple1Alt, true),
                equalTo(Lists.newArrayList(CB02_)));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipeComplexInvalid() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValidB, IngredientComponentStubs.COMPLEX, recipeComplex, true),
                nullValue());
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipeComplexValid() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValid, IngredientComponentStubs.COMPLEX, recipeComplex, true),
                equalTo(Lists.newArrayList(null, CB02_, CA01_)));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipeEqualInputsInvalid() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValidMore, IngredientComponentStubs.COMPLEX, recipeEquals, true),
                nullValue());
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipeEqualInputsVvalid() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValidMany, IngredientComponentStubs.COMPLEX, recipeEquals, true),
                equalTo(Lists.newArrayList(CA01_, CA02_, CA01_, CA01_)));
    }

    @Test
    public void testGetIngredientRecipeInputsEmptyStorageEmptyRecipeActual() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageEmpty, IngredientComponentStubs.COMPLEX, recipeEmpty, false),
                nullValue());
        assertThat(Sets.newHashSet(storageEmpty.iterator()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testGetIngredientRecipeInputsEmptyStorageSimpleRecipe1Actual() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageEmpty, IngredientComponentStubs.COMPLEX, recipeSimple1, false),
                nullValue());
        assertThat(Sets.newHashSet(storageEmpty.iterator()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipe1Actual() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValid, IngredientComponentStubs.COMPLEX, recipeSimple1, false),
                equalTo(Lists.newArrayList(CA01_)));
        assertThat(Sets.newHashSet(storageValid.iterator()), equalTo(Sets.newHashSet(CB02_, CA91B)));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipe3Actual() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValid, IngredientComponentStubs.COMPLEX, recipeSimple3, false),
                equalTo(Lists.newArrayList(CA01_, CB02_, CA91B)));
        assertThat(Sets.newHashSet(storageValid.iterator()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipe1AltActual() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValidB, IngredientComponentStubs.COMPLEX, recipeSimple1Alt, false),
                equalTo(Lists.newArrayList(CB02_)));
        assertThat(Sets.newHashSet(storageValidB.iterator()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipeComplexInvalidActual() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValidB, IngredientComponentStubs.COMPLEX, recipeComplex, false),
                nullValue());
        assertThat(Sets.newHashSet(storageValidB.iterator()), equalTo(Sets.newHashSet(CB02_)));
    }

    @Test
    public void testGetIngredientRecipeInputsValidStorageSimpleRecipeComplexValidActual() {
        assertThat(CraftingHelpers.getIngredientRecipeInputs(storageValid, IngredientComponentStubs.COMPLEX, recipeComplex, false),
                equalTo(Lists.newArrayList(null, CB02_, CA01_)));
        assertThat(Sets.newHashSet(storageValid.iterator()), equalTo(Sets.newHashSet(CA91B)));
    }

    /* ------------ getCompressedIngredients ------------ */

    @Test
    public void testGetCompressedIngredientsEmpty() {
        assertThat(CraftingHelpers.getCompressedIngredients(IngredientComponentStubs.COMPLEX, new MixedIngredients(Maps.newIdentityHashMap())),
                equalTo(Lists.newArrayList()));
    }

    @Test
    public void testGetCompressedIngredientsEmpty2() {
        Map<IngredientComponent<?, ?>, List<?>> map = Maps.newIdentityHashMap();
        map.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList());
        assertThat(CraftingHelpers.getCompressedIngredients(IngredientComponentStubs.COMPLEX, new MixedIngredients(map)),
                equalTo(Lists.newArrayList()));
    }

    @Test
    public void testGetCompressedIngredientsSimple() {
        Map<IngredientComponent<?, ?>, List<?>> map = Maps.newIdentityHashMap();
        map.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                CA01_
        ));
        Integer condition = IngredientComponentStubs.COMPLEX.getMatcher().getExactMatchNoQuantityCondition();
        assertThat(CraftingHelpers.getCompressedIngredients(IngredientComponentStubs.COMPLEX, new MixedIngredients(map)),
                equalTo(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, condition)
                )));
    }

    @Test
    public void testGetCompressedIngredientsSimpleEq() {
        Map<IngredientComponent<?, ?>, List<?>> map = Maps.newIdentityHashMap();
        map.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                CA01_,
                CA02_,
                CA03_
        ));
        Integer condition = IngredientComponentStubs.COMPLEX.getMatcher().getExactMatchNoQuantityCondition();
        assertThat(CraftingHelpers.getCompressedIngredients(IngredientComponentStubs.COMPLEX, new MixedIngredients(map)),
                equalTo(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA06_, condition)
                )));
    }

    @Test
    public void testGetCompressedIngredientsMultiple() {
        Map<IngredientComponent<?, ?>, List<?>> map = Maps.newIdentityHashMap();
        map.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                CA01_,
                CB02_,
                CA03_,
                CA01B
        ));
        Integer condition = IngredientComponentStubs.COMPLEX.getMatcher().getExactMatchNoQuantityCondition();
        assertThat(CraftingHelpers.getCompressedIngredients(IngredientComponentStubs.COMPLEX, new MixedIngredients(map)),
                equalTo(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA04_, condition),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, condition),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01B, condition)
                )));
    }

    @Test
    public void testGetCompressedIngredientsMixed() {
        Map<IngredientComponent<?, ?>, List<?>> map = Maps.newIdentityHashMap();
        map.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                CA01_,
                CB02_,
                CA03_,
                CA01B
        ));
        map.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(
                1, 2, 3
        ));
        Integer condition = IngredientComponentStubs.COMPLEX.getMatcher().getExactMatchNoQuantityCondition();
        assertThat(CraftingHelpers.getCompressedIngredients(IngredientComponentStubs.COMPLEX, new MixedIngredients(map)),
                equalTo(Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA04_, condition),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, condition),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01B, condition)
                )));
    }

    /* ------------ getRecipeOutputs ------------ */

    @Test
    public void testGetRecipeOutputs() {
        Map<IngredientComponent<?, ?>, List<?>> map = Maps.newIdentityHashMap();
        map.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                CA01_,
                CB02_,
                CA03_,
                CA01B
        ));
        map.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(
                1, 2, 3
        ));
        Integer condition = IngredientComponentStubs.COMPLEX.getMatcher().getExactMatchNoQuantityCondition();
        Map<IngredientComponent<?, ?>, List<?>> expectedMap = Maps.newIdentityHashMap();
        expectedMap.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA04_, condition),
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, condition),
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01B, condition)
        ));
        expectedMap.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.SIMPLE, 6, false)
        ));
        assertThat(CraftingHelpers.getRecipeOutputs(new RecipeDefinition(null, new MixedIngredients(map))),
                equalTo(expectedMap));
    }

}
