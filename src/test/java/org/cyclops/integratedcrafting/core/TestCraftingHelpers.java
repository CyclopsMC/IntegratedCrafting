package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IngredientComponentStorageEmpty;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.cyclopscore.ingredient.storage.IngredientComponentStorageCollectionWrapper;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.RecursiveCraftingRecipeException;
import org.cyclops.integratedcrafting.api.crafting.UnknownCraftingRecipeException;
import org.cyclops.integratedcrafting.api.recipe.PrioritizedRecipe;
import org.cyclops.integratedcrafting.ingredient.ComplexStack;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

    private static final ComplexStack CB01_ = new ComplexStack(ComplexStack.Group.B, 0, 1, null);
    private static final ComplexStack CB02_ = new ComplexStack(ComplexStack.Group.B, 0, 2, null);
    private static final ComplexStack CB03_ = new ComplexStack(ComplexStack.Group.B, 0, 3, null);
    private static final ComplexStack CB0110_ = new ComplexStack(ComplexStack.Group.B, 0, 110, null);
    private static final ComplexStack CC01_ = new ComplexStack(ComplexStack.Group.C, 0, 1, null);
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
        Map<IngredientComponent<?, ?>, List<?>> mapSimple1Output = Maps.newIdentityHashMap();
        mapSimple1Output.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeSimple1 = new RecipeDefinition(mapSimple1, new MixedIngredients(mapSimple1Output));

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

    /* ------------ multiplyRecipeOutputs ------------ */

    @Test
    public void testMultiplyRecipeOutputsOne() {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> input = Maps.newIdentityHashMap();
        input.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, 0),
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB01_, 0)
        ));
        input.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.SIMPLE, 2, false)
        ));

        assertThat(CraftingHelpers.multiplyRecipeOutputs(input, 1),
                equalTo(input));
    }

    @Test
    public void testMultiplyRecipeOutputsTwo() {
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> input = Maps.newIdentityHashMap();
        input.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, 0),
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB01_, 0)
        ));
        input.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.SIMPLE, 2, false)
        ));
        Map<IngredientComponent<?, ?>, List<IPrototypedIngredient<?, ?>>> output = Maps.newIdentityHashMap();
        output.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA02_, 0),
                new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, 0)
        ));
        output.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(
                new PrototypedIngredient<>(IngredientComponentStubs.SIMPLE, 4, false)
        ));

        // Wrap in a HashMap because our IdentityMap does not like equals...
        assertThat(Maps.newHashMap(CraftingHelpers.multiplyRecipeOutputs(input, 2)),
                equalTo(Maps.newHashMap(output)));
    }

    /* ------------ calculateCraftingJobs ------------ */

    private IRecipeDefinition recipeB;
    private IRecipeDefinition recipeBAlt;
    private IRecipeDefinition recipeBAlt2;
    private IRecipeDefinition recipeB2;
    private IRecipeDefinition recipeB2Alt;
    private IRecipeDefinition recipeB3;
    private IRecipeDefinition recipeBBatch;
    private IRecipeDefinition recipeBRecursive;
    private IRecipeDefinition recipeA;
    private IRecipeDefinition recipeAMultiple;
    private IRecipeDefinition recipeAB;
    private IRecipeDefinition recipeC;
    private Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetterEmpty;
    private Function<IngredientComponent<?, ?>, IIngredientComponentStorage> storageGetter;
    private Map<IngredientComponent<?, ?>, IngredientCollectionPrototypeMap<?, ?>> simulatedExtractionMemory;
    private CraftingHelpers.IIdentifierGenerator identifierGenerator;
    private CraftingJobDependencyGraph craftingJobDependencyGraph;
    private Set<IPrototypedIngredient> parentDependencies;

    @Before
    public void beforeEachCalculateCraftingJobs() {
        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapB = Maps.newIdentityHashMap();
        mapB.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapBOutput = Maps.newIdentityHashMap();
        mapBOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeB = new RecipeDefinition(mapB, new MixedIngredients(mapBOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapBAlt = Maps.newIdentityHashMap();
        mapBAlt.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01B, ComplexStack.Match.EXACT),
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapBAltOutput = Maps.newIdentityHashMap();
        mapBAltOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeBAlt = new RecipeDefinition(mapBAlt, new MixedIngredients(mapBAltOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapBAlt2 = Maps.newIdentityHashMap();
        mapBAlt2.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA91B, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapBAlt2Output = Maps.newIdentityHashMap();
        mapBAlt2Output.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeBAlt2 = new RecipeDefinition(mapBAlt2, new MixedIngredients(mapBAlt2Output));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapB2 = Maps.newIdentityHashMap();
        mapB2.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01B, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapB2Output = Maps.newIdentityHashMap();
        mapB2Output.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeB2 = new RecipeDefinition(mapB2, new MixedIngredients(mapB2Output));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapB2Alt = Maps.newIdentityHashMap();
        mapB2Alt.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CC01_, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapB2AltOutput = Maps.newIdentityHashMap();
        mapB2AltOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeB2Alt = new RecipeDefinition(mapB2Alt, new MixedIngredients(mapB2AltOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapB3 = Maps.newIdentityHashMap();
        mapB3.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01B, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA91B, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapB3Output = Maps.newIdentityHashMap();
        mapB3Output.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeB3 = new RecipeDefinition(mapB3, new MixedIngredients(mapB3Output));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapBRecursive = Maps.newIdentityHashMap();
        mapBRecursive.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapBRecursiveOutput = Maps.newIdentityHashMap();
        mapBRecursiveOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeBRecursive = new RecipeDefinition(mapBRecursive, new MixedIngredients(mapBRecursiveOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapBBatch = Maps.newIdentityHashMap();
        mapBBatch.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA02_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                ),
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapBBatchOutput = Maps.newIdentityHashMap();
        mapBBatchOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CB02_));
        recipeBBatch = new RecipeDefinition(mapBBatch, new MixedIngredients(mapBBatchOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapA = Maps.newIdentityHashMap();
        mapA.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA91B, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapAOutput = Maps.newIdentityHashMap();
        mapAOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CA01_));
        recipeA = new RecipeDefinition(mapA, new MixedIngredients(mapAOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapAMultiple = Maps.newIdentityHashMap();
        mapAMultiple.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA91B, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapAMultipleOutput = Maps.newIdentityHashMap();
        mapAMultipleOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CA04_));
        recipeAMultiple = new RecipeDefinition(mapAMultiple, new MixedIngredients(mapAMultipleOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapAB = Maps.newIdentityHashMap();
        mapAB.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapABOutput = Maps.newIdentityHashMap();
        mapABOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CA01B));
        recipeAB = new RecipeDefinition(mapAB, new MixedIngredients(mapABOutput));

        Map<IngredientComponent<?, ?>, List<List<IPrototypedIngredient<?, ?>>>> mapC = Maps.newIdentityHashMap();
        mapC.put(IngredientComponentStubs.COMPLEX, Lists.<List<IPrototypedIngredient<?, ?>>>newArrayList(
                Lists.newArrayList(
                        new PrototypedIngredient<>(IngredientComponentStubs.COMPLEX, CA01B, ComplexStack.Match.EXACT)
                )
        ));
        Map<IngredientComponent<?, ?>, List<?>> mapCOutput = Maps.newIdentityHashMap();
        mapCOutput.put(IngredientComponentStubs.COMPLEX, Lists.newArrayList(CC01_));
        recipeC = new RecipeDefinition(mapC, new MixedIngredients(mapCOutput));


        storageGetterEmpty = IngredientComponentStorageEmpty::new;

        simulatedExtractionMemory = Maps.newIdentityHashMap();

        Wrapper<Integer> id = new Wrapper<>(0);
        identifierGenerator = () -> {
            int last = id.get();
            id.set(last + 1);
            return last;
        };

        craftingJobDependencyGraph = new CraftingJobDependencyGraph();

        parentDependencies = Sets.newHashSet();
    }

    @Test(expected = UnknownCraftingRecipeException.class)
    public void testCalculateCraftingJobsEmpty() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        CraftingHelpers.calculateCraftingJobs(new RecipeIndexDefault(), 0, storageGetterEmpty,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);
    }

    @Test(expected = UnknownCraftingRecipeException.class)
    public void testCalculateCraftingJobsUnknown() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));

        CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetterEmpty,
                IngredientComponentStubs.COMPLEX, CA01_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);
    }

    @Test
    public void testCalculateCraftingJobsSingleOneAvailable() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));

        // Single crafting recipe with one available dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA01_, false);
        storageGetter = (c) -> storage;

        CraftingJob j = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j.getId(), equalTo(0));
        assertThat(j.getChannel(), equalTo(0));
        assertThat(j.getAmount(), equalTo(1));
        assertThat(j.getRecipe().getRecipe(), equalTo(recipeB));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test
    public void testCalculateCraftingJobsSingleOneAvailableLowerRequested() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));

        // Single crafting recipe with one available dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA01_, false);
        storageGetter = (c) -> storage;

        CraftingJob j = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB01_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j.getId(), equalTo(0));
        assertThat(j.getChannel(), equalTo(0));
        assertThat(j.getAmount(), equalTo(1));
        assertThat(j.getRecipe().getRecipe(), equalTo(recipeB));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test
    public void testCalculateCraftingJobsSingleMoreAvailable() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));

        // Single crafting recipe with one available dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA05_, false);
        storageGetter = (c) -> storage;

        CraftingJob j = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j.getId(), equalTo(0));
        assertThat(j.getChannel(), equalTo(0));
        assertThat(j.getAmount(), equalTo(1));
        assertThat(j.getRecipe().getRecipe(), equalTo(recipeB));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test
    public void testCalculateCraftingJobsSingleOneAvailableNoCraftMissing() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));

        // Single crafting recipe with one available dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA01_, false);
        storageGetter = (c) -> storage;

        CraftingJob j = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, false,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j.getId(), equalTo(0));
        assertThat(j.getChannel(), equalTo(0));
        assertThat(j.getAmount(), equalTo(1));
        assertThat(j.getRecipe().getRecipe(), equalTo(recipeB));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test(expected = UnknownCraftingRecipeException.class)
    public void testCalculateCraftingJobsDoubleOneMissingNoCraftMissing() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeA));

        // Single crafting recipe with one missing but craftable dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA91B, false);
        storageGetter = (c) -> storage;

        CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, false,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);
    }

    @Test
    public void testCalculateCraftingJobsDoubleOneMissing() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeA));

        // Single crafting recipe with one missing but craftable dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA91B, false);
        storageGetter = (c) -> storage;

        CraftingJob j1 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getChannel(), equalTo(0));
        assertThat(j1.getAmount(), equalTo(1));
        assertThat(j1.getRecipe().getRecipe(), equalTo(recipeB));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(2));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(j1), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(0));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
    }

    @Test
    public void testCalculateCraftingJobsSingleAlternativeAvailable() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeBAlt));

        // Single crafting recipe with one missing, but one other available alternative
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA01B, false);
        storageGetter = (c) -> storage;

        CraftingJob j0 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getChannel(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(1));
        assertThat(j0.getRecipe().getRecipe(), equalTo(recipeBAlt));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test
    public void testCalculateCraftingJobsDoubleOneMissingOneAvailable() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB2));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeA));

        // Single crafting recipe with one missing but craftable dependent (as an alternative), and one available dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA91B, false);
        storage.insert(CA01B, false);
        storageGetter = (c) -> storage;

        CraftingJob j1 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getChannel(), equalTo(0));
        assertThat(j1.getAmount(), equalTo(1));
        assertThat(j1.getRecipe().getRecipe(), equalTo(recipeB2));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(2));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(j1), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(0));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
    }

    @Test
    public void testCalculateCraftingJobsDoubleThreeAvailableOverlapping() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB3));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeA));

        // Single crafting recipe with three requirements
        // Two items are available, one is missing, but craftable
        // For that craftable item, one requirements is available (and equal to one of the three initial requirements).
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        // For A
        storage.insert(CA91B, false);

        // For B (also requires A)
        storage.insert(CA91B, false);
        storage.insert(CA01B, false);

        storageGetter = (c) -> storage;

        CraftingJob j1 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j1.getId(), equalTo(1));
        assertThat(j1.getChannel(), equalTo(0));
        assertThat(j1.getAmount(), equalTo(1));
        assertThat(j1.getRecipe().getRecipe(), equalTo(recipeB3));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(2));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(j1), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j1).size(), equalTo(0));

        CraftingJob j0 = Iterables.getFirst(craftingJobDependencyGraph.getDependencies(j1), null);
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependents(j0).contains(j1), equalTo(true));
    }

    @Test(expected = UnknownCraftingRecipeException.class)
    public void testCalculateCraftingJobsDoubleThreeAvailableOverlappingPartial() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB3));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeA));

        // Single crafting recipe with three requirements
        // Two items are available, one is missing, but craftable
        // For that craftable item, one requirements is not available (and equal to one of the three initial requirements).
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        // For A
        storage.insert(CA91B, false);

        // For B (also requires A)
        //storage.insert(CA91B, false); // Not available
        storage.insert(CA01B, false);

        storageGetter = (c) -> storage;

        CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);
    }

    @Test(expected = RecursiveCraftingRecipeException.class)
    public void testCalculateCraftingJobsRecursive() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeBRecursive));

        // A recipe with infinite recursion

        CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetterEmpty,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);
    }

    @Test
    public void testCalculateCraftingJobsSingleMultipleRecipes1() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeBAlt2));

        // Single crafting recipe with one missing, but one other available alternative
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA91B, false);
        storageGetter = (c) -> storage;

        CraftingJob j0 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getChannel(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(1));
        assertThat(j0.getRecipe().getRecipe(), equalTo(recipeBAlt2));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test
    public void testCalculateCraftingJobsSingleMultipleRecipes2() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeBAlt2));

        // Single crafting recipe with one missing, but one other available alternative
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA01_, false);
        storageGetter = (c) -> storage;

        CraftingJob j0 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getChannel(), equalTo(0));
        assertThat(j0.getAmount(), equalTo(1));
        assertThat(j0.getRecipe().getRecipe(), equalTo(recipeB));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test
    public void testCalculateCraftingJobsTriple() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB2Alt));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeA));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeAB));
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeC));

        // Complex recipe tree depth of 3, where the root has 2 branches
        // recipeB2Alt (4)
        //   recipeA (0)
        //     *CA91B
        //   recipeC (3)
        //     recipeAB (2)
        //       recipeA (1)
        //         *CA91B
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA91B, false);
        storage.insert(CA91B, false);
        storageGetter = (c) -> storage;

        CraftingJob j4 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB02_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j4.getId(), equalTo(4));
        assertThat(j4.getChannel(), equalTo(0));
        assertThat(j4.getAmount(), equalTo(1));
        assertThat(j4.getRecipe().getRecipe(), equalTo(recipeB2Alt));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(5));
        assertThat(craftingJobDependencyGraph.getCraftingJobs().contains(j4), equalTo(true));
        assertThat(craftingJobDependencyGraph.getDependencies(j4).size(), equalTo(2));
        assertThat(craftingJobDependencyGraph.getDependents(j4).size(), equalTo(0));

        CraftingJob j0 = craftingJobDependencyGraph.getCraftingJobs().stream().filter(j -> j.getId() == 0).findFirst().get();
        CraftingJob j1 = craftingJobDependencyGraph.getCraftingJobs().stream().filter(j -> j.getId() == 1).findFirst().get();
        CraftingJob j2 = craftingJobDependencyGraph.getCraftingJobs().stream().filter(j -> j.getId() == 2).findFirst().get();
        CraftingJob j3 = craftingJobDependencyGraph.getCraftingJobs().stream().filter(j -> j.getId() == 3).findFirst().get();

        assertThat(j0.getChannel(), equalTo(0));
        assertThat(j0.getRecipe().getRecipe(), equalTo(recipeA));
        assertThat(j0.getAmount(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0), equalTo(Lists.newArrayList(j4)));

        assertThat(j1.getChannel(), equalTo(0));
        assertThat(j1.getRecipe().getRecipe(), equalTo(recipeA));
        assertThat(j1.getAmount(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependencies(j1).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j1), equalTo(Lists.newArrayList(j2)));

        assertThat(j2.getChannel(), equalTo(0));
        assertThat(j2.getRecipe().getRecipe(), equalTo(recipeAB));
        assertThat(j2.getAmount(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependencies(j2), equalTo(Lists.newArrayList(j1)));
        assertThat(craftingJobDependencyGraph.getDependents(j2), equalTo(Lists.newArrayList(j3)));

        assertThat(j3.getChannel(), equalTo(0));
        assertThat(j3.getRecipe().getRecipe(), equalTo(recipeC));
        assertThat(j3.getAmount(), equalTo(1));
        assertThat(craftingJobDependencyGraph.getDependencies(j3), equalTo(Lists.newArrayList(j2)));
        assertThat(craftingJobDependencyGraph.getDependents(j3), equalTo(Lists.newArrayList(j4)));
    }

    @Test
    public void testCalculateCraftingJobsSingleOneAvailableHigherRequested() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));

        // Single crafting recipe with one available dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA01_, false);
        storageGetter = (c) -> storage;

        CraftingJob j0 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB03_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getChannel(), equalTo(0));
        assertThat(j0.getRecipe().getRecipe(), equalTo(recipeB));
        assertThat(j0.getAmount(), equalTo(2));
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(0));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

    @Test
    public void testCalculateCraftingJobsSingleOneAvailableALotHigherRequested() throws UnknownCraftingRecipeException, RecursiveCraftingRecipeException {
        RecipeIndexDefault recipeIndex = new RecipeIndexDefault();
        recipeIndex.addRecipe(new PrioritizedRecipe(recipeB));

        // Single crafting recipe with one available dependent
        IngredientComponentStorageCollectionWrapper<ComplexStack, Integer> storage = new IngredientComponentStorageCollectionWrapper<>(new IngredientCollectionPrototypeMap<>(IngredientComponentStubs.COMPLEX));
        storage.insert(CA01_, false);
        storageGetter = (c) -> storage;

        CraftingJob j0 = CraftingHelpers.calculateCraftingJobs(recipeIndex, 0, storageGetter,
                IngredientComponentStubs.COMPLEX, CB0110_, ComplexStack.Match.EXACT, true,
                simulatedExtractionMemory, identifierGenerator, craftingJobDependencyGraph, parentDependencies);

        assertThat(j0.getId(), equalTo(0));
        assertThat(j0.getChannel(), equalTo(0));
        assertThat(j0.getRecipe().getRecipe(), equalTo(recipeB));
        assertThat(j0.getAmount(), equalTo(55));
        assertThat(craftingJobDependencyGraph.getDependencies(j0).size(), equalTo(0));
        assertThat(craftingJobDependencyGraph.getDependents(j0).size(), equalTo(0));

        assertThat(craftingJobDependencyGraph.getCraftingJobs().size(), equalTo(0));
    }

}
