package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cyclops.commoncapabilities.api.ingredient.IPrototypedIngredient;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.PrototypedIngredient;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link MissingIngredients} serialization/deserialization.
 */
public class TestMissingIngredients {

    private static HolderLookup.Provider LOOKUP;

    @BeforeAll
    public static void init() {
        // Trigger static initialization of stubs
        IngredientComponent<?, ?> simple = IngredientComponentStubs.SIMPLE;
        Assertions.assertNotNull(simple);
        LOOKUP = HolderLookup.Provider.create(Stream.empty());

        // Register stub components into the NeoForge registry so deserialize() can look them up.
        // The registry may be frozen at this point, so we temporarily unfreeze it via reflection.
        if (IngredientComponent.REGISTRY != null
                && IngredientComponent.REGISTRY.getValue(simple.getName()) == null) {
            try {
                java.lang.reflect.Method unfreeze = IngredientComponent.REGISTRY.getClass()
                        .getMethod("unfreeze", boolean.class);
                unfreeze.invoke(IngredientComponent.REGISTRY, false);
                Registry.register(IngredientComponent.REGISTRY, simple.getName(), simple);
                IngredientComponent.REGISTRY.freeze();
            } catch (Exception e) {
                throw new RuntimeException("Failed to register ingredient component stubs into registry", e);
            }
        }
    }

    private static TagValueOutput createOutput() {
        return TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
    }

    private static ValueInput createInput(TagValueOutput output) {
        CompoundTag tag = output.buildResult();
        return TagValueInput.create(ProblemReporter.DISCARDING, LOOKUP, tag);
    }

    // -------------------------------------------------------------------------
    // Serialization tests
    // -------------------------------------------------------------------------

    @Test
    public void testSerializeEmpty() {
        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of());
        CompoundTag tag = output.buildResult();

        // The "v" list should exist and be empty
        assertThat(tag.contains("v"), is(true));
        assertThat(((ListTag) tag.get("v")).size(), is(0));
    }

    @Test
    public void testSerializeSingleElement() {
        IPrototypedIngredient<Long, Boolean> prototype = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 42L, Boolean.TRUE);

        MissingIngredients.PrototypedWithRequested<Long, Boolean> alternative =
                new MissingIngredients.PrototypedWithRequested<>(prototype, 10L);

        MissingIngredients.Element<Long, Boolean> element =
                new MissingIngredients.Element<>(Lists.newArrayList(alternative), false);

        MissingIngredients<Long, Boolean> missing =
                new MissingIngredients<>(Lists.newArrayList(element));

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of(IngredientComponentStubs.SIMPLE, missing));
        CompoundTag tag = output.buildResult();

        ListTag vList = (ListTag) tag.get("v");
        assertThat(vList.size(), is(1));

        CompoundTag entry = (CompoundTag) vList.get(0);
        assertThat(entry.getString("component").orElseThrow(), is("cyclopscore:simple"));

        ListTag elements = (ListTag) entry.get("elements");
        assertThat(elements.size(), is(1));

        CompoundTag elem = (CompoundTag) elements.get(0);
        assertThat(elem.getBoolean("inputReusable").orElse(false), is(false));

        ListTag alternatives = (ListTag) elem.get("alternatives");
        assertThat(alternatives.size(), is(1));

        CompoundTag alt = (CompoundTag) alternatives.get(0);
        assertThat(alt.getLong("quantityMissing").orElseThrow(), is(10L));
    }

    @Test
    public void testSerializeInputReusableTrue() {
        IPrototypedIngredient<Long, Boolean> prototype = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 1L, Boolean.TRUE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt =
                new MissingIngredients.PrototypedWithRequested<>(prototype, 5L);
        MissingIngredients.Element<Long, Boolean> element =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), true);
        MissingIngredients<Long, Boolean> missing = new MissingIngredients<>(Lists.newArrayList(element));

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of(IngredientComponentStubs.SIMPLE, missing));
        CompoundTag tag = output.buildResult();

        ListTag vList = (ListTag) tag.get("v");
        CompoundTag elem = (CompoundTag) ((ListTag) ((CompoundTag) vList.get(0)).get("elements")).get(0);
        assertThat(elem.getBoolean("inputReusable").orElse(false), is(true));
    }

    @Test
    public void testSerializeMultipleAlternatives() {
        IPrototypedIngredient<Long, Boolean> proto1 = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 1L, Boolean.TRUE);
        IPrototypedIngredient<Long, Boolean> proto2 = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 2L, Boolean.FALSE);

        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt1 =
                new MissingIngredients.PrototypedWithRequested<>(proto1, 5L);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt2 =
                new MissingIngredients.PrototypedWithRequested<>(proto2, 3L);

        MissingIngredients.Element<Long, Boolean> element =
                new MissingIngredients.Element<>(Lists.newArrayList(alt1, alt2), true);
        MissingIngredients<Long, Boolean> missing = new MissingIngredients<>(Lists.newArrayList(element));

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of(IngredientComponentStubs.SIMPLE, missing));
        CompoundTag tag = output.buildResult();

        ListTag alternatives = (ListTag) ((CompoundTag)
                ((ListTag) ((CompoundTag) ((ListTag) tag.get("v")).get(0)).get("elements")).get(0))
                .get("alternatives");
        assertThat(alternatives.size(), is(2));
        assertThat(((CompoundTag) alternatives.get(0)).getLong("quantityMissing").orElseThrow(), is(5L));
        assertThat(((CompoundTag) alternatives.get(1)).getLong("quantityMissing").orElseThrow(), is(3L));
    }

    @Test
    public void testSerializeMultipleElements() {
        IPrototypedIngredient<Long, Boolean> proto = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 7L, Boolean.TRUE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt =
                new MissingIngredients.PrototypedWithRequested<>(proto, 2L);

        MissingIngredients.Element<Long, Boolean> elem1 =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), false);
        MissingIngredients.Element<Long, Boolean> elem2 =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), true);
        MissingIngredients<Long, Boolean> missing = new MissingIngredients<>(Lists.newArrayList(elem1, elem2));

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of(IngredientComponentStubs.SIMPLE, missing));
        CompoundTag tag = output.buildResult();

        ListTag elements = (ListTag) ((CompoundTag) ((ListTag) tag.get("v")).get(0)).get("elements");
        assertThat(elements.size(), is(2));
        assertThat(((CompoundTag) elements.get(0)).getBoolean("inputReusable").orElse(false), is(false));
        assertThat(((CompoundTag) elements.get(1)).getBoolean("inputReusable").orElse(false), is(true));
    }

    // -------------------------------------------------------------------------
    // Deserialization tests
    // -------------------------------------------------------------------------

    @Test
    public void testDeserializeEmpty() {
        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of());
        ValueInput input = createInput(output);
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> result = MissingIngredients.deserialize(input);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void testDeserializeUnknownComponentThrows() {
        // Manually build a tag with an unknown component name
        TagValueOutput output = createOutput();
        ValueOutput.ValueOutputList list = output.childrenList("v");
        ValueOutput entry = list.addChild();
        entry.putString("component", "unknown:nonexistent");
        entry.childrenList("elements");

        ValueInput input = createInput(output);
        Assertions.assertThrows(IllegalArgumentException.class, () -> MissingIngredients.deserialize(input));
    }

    @Test
    public void testRoundTripSingleElement() {
        IPrototypedIngredient<Long, Boolean> prototype = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 42L, Boolean.TRUE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alternative =
                new MissingIngredients.PrototypedWithRequested<>(prototype, 10L);
        MissingIngredients.Element<Long, Boolean> element =
                new MissingIngredients.Element<>(Lists.newArrayList(alternative), false);
        MissingIngredients<Long, Boolean> missing = new MissingIngredients<>(Lists.newArrayList(element));

        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> original = Map.of(IngredientComponentStubs.SIMPLE, missing);

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, original);
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> result = MissingIngredients.deserialize(createInput(output));

        assertThat(result.size(), is(1));
        assertThat(result.containsKey(IngredientComponentStubs.SIMPLE), is(true));
        assertThat(result.get(IngredientComponentStubs.SIMPLE), is(missing));
    }

    @Test
    public void testRoundTripInputReusable() {
        IPrototypedIngredient<Long, Boolean> proto = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 7L, Boolean.TRUE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt =
                new MissingIngredients.PrototypedWithRequested<>(proto, 2L);
        MissingIngredients.Element<Long, Boolean> elem =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), true);
        MissingIngredients<Long, Boolean> missing = new MissingIngredients<>(Lists.newArrayList(elem));

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of(IngredientComponentStubs.SIMPLE, missing));
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> result = MissingIngredients.deserialize(createInput(output));

        List<? extends MissingIngredients.Element<?, ?>> elements = result.get(IngredientComponentStubs.SIMPLE).getElements();
        assertThat(elements.size(), is(1));
        assertThat(elements.get(0).isInputReusable(), is(true));
    }

    @Test
    public void testRoundTripMultipleAlternatives() {
        IPrototypedIngredient<Long, Boolean> proto1 = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 1L, Boolean.TRUE);
        IPrototypedIngredient<Long, Boolean> proto2 = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 2L, Boolean.FALSE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt1 =
                new MissingIngredients.PrototypedWithRequested<>(proto1, 5L);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt2 =
                new MissingIngredients.PrototypedWithRequested<>(proto2, 3L);
        MissingIngredients.Element<Long, Boolean> element =
                new MissingIngredients.Element<>(Lists.newArrayList(alt1, alt2), true);
        MissingIngredients<Long, Boolean> missing = new MissingIngredients<>(Lists.newArrayList(element));

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of(IngredientComponentStubs.SIMPLE, missing));
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> result = MissingIngredients.deserialize(createInput(output));

        MissingIngredients<?, ?> resultMissing = result.get(IngredientComponentStubs.SIMPLE);
        assertThat(resultMissing, is(missing));
        assertThat(resultMissing.getElements().get(0).getAlternatives().size(), is(2));
        assertThat(resultMissing.getElements().get(0).getAlternatives().get(0).getQuantityMissing(), is(5L));
        assertThat(resultMissing.getElements().get(0).getAlternatives().get(1).getQuantityMissing(), is(3L));
    }

    @Test
    public void testRoundTripMultipleElements() {
        IPrototypedIngredient<Long, Boolean> proto = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 7L, Boolean.TRUE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt =
                new MissingIngredients.PrototypedWithRequested<>(proto, 2L);
        MissingIngredients.Element<Long, Boolean> elem1 =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), false);
        MissingIngredients.Element<Long, Boolean> elem2 =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), true);
        MissingIngredients<Long, Boolean> missing = new MissingIngredients<>(Lists.newArrayList(elem1, elem2));

        TagValueOutput output = createOutput();
        MissingIngredients.serialize(output, Map.of(IngredientComponentStubs.SIMPLE, missing));
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> result = MissingIngredients.deserialize(createInput(output));

        MissingIngredients<?, ?> resultMissing = result.get(IngredientComponentStubs.SIMPLE);
        assertThat(resultMissing, is(missing));
        assertThat(resultMissing.getElements().size(), is(2));
        assertThat(resultMissing.getElements().get(0).isInputReusable(), is(false));
        assertThat(resultMissing.getElements().get(1).isInputReusable(), is(true));
    }


    // -------------------------------------------------------------------------
    // Equality / model tests
    // -------------------------------------------------------------------------

    @Test
    public void testPrototypedWithRequestedEquality() {
        IPrototypedIngredient<Long, Boolean> proto = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 10L, Boolean.TRUE);

        MissingIngredients.PrototypedWithRequested<Long, Boolean> a =
                new MissingIngredients.PrototypedWithRequested<>(proto, 5L);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> b =
                new MissingIngredients.PrototypedWithRequested<>(proto, 5L);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> c =
                new MissingIngredients.PrototypedWithRequested<>(proto, 6L);

        assertThat(a.equals(b), is(true));
        assertThat(a.equals(c), is(false));
        assertThat(a.getQuantityMissing(), is(5L));
        assertThat(a.getRequestedPrototype(), is(proto));
    }

    @Test
    public void testPrototypedWithRequestedSetQuantity() {
        IPrototypedIngredient<Long, Boolean> proto = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 10L, Boolean.TRUE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> p =
                new MissingIngredients.PrototypedWithRequested<>(proto, 5L);
        p.setQuantityMissing(99L);
        assertThat(p.getQuantityMissing(), is(99L));
    }

    @Test
    public void testElementEquality() {
        IPrototypedIngredient<Long, Boolean> proto = new PrototypedIngredient<>(
                IngredientComponentStubs.SIMPLE, 1L, Boolean.TRUE);
        MissingIngredients.PrototypedWithRequested<Long, Boolean> alt =
                new MissingIngredients.PrototypedWithRequested<>(proto, 1L);

        MissingIngredients.Element<Long, Boolean> e1 =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), false);
        MissingIngredients.Element<Long, Boolean> e2 =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), false);
        MissingIngredients.Element<Long, Boolean> e3 =
                new MissingIngredients.Element<>(Lists.newArrayList(alt), true);

        assertThat(e1.equals(e2), is(true));
        assertThat(e1.equals(e3), is(false));
        assertThat(e1.isInputReusable(), is(false));
    }

    @Test
    public void testMissingIngredientsEquality() {
        MissingIngredients<Long, Boolean> a = new MissingIngredients<>(Lists.newArrayList());
        MissingIngredients<Long, Boolean> b = new MissingIngredients<>(Lists.newArrayList());
        assertThat(a.equals(b), is(true));
        assertThat(a.equals(new Object()), is(false));
    }

    @Test
    public void testMissingIngredientsToString() {
        MissingIngredients<Long, Boolean> m = new MissingIngredients<>(Lists.newArrayList());
        assertThat(m.toString(), is("[]"));
    }
}
