package org.cyclops.integratedcrafting.inventory.container;

import com.google.common.collect.Lists;
import org.cyclops.integratedcrafting.api.recipe.RecipeKey;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedRecipes.RecipeEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author rubensworks
 */
public class TestRecipeEntryComparator {

    protected static RecipeEntry newEntry(int serverIndex, String sortName, String identifier) {
        return new RecipeEntry(serverIndex, RecipeKey.ofRecipeId(identifier), null, null,
                sortName, identifier, sortName + " " + identifier);
    }

    protected static List<String> sortedIdentifiers(List<RecipeEntry> entries) {
        List<RecipeEntry> sorted = Lists.newArrayList(entries);
        sorted.sort(RecipeEntry.COMPARATOR);
        return sorted.stream().map(RecipeEntry::identifier).collect(Collectors.toList());
    }

    @Test
    public void testSortsByOutputName() {
        List<RecipeEntry> entries = Lists.newArrayList(
                newEntry(0, "stick", "minecraft:stick"),
                newEntry(1, "chest", "minecraft:chest"),
                newEntry(2, "oak planks", "minecraft:oak_planks")
        );
        assertThat(sortedIdentifiers(entries),
                is(Lists.newArrayList("minecraft:chest", "minecraft:oak_planks", "minecraft:stick")));
    }

    /**
     * Recipes that share an output name must keep a stable order,
     * independent of the order in which the server sent them.
     */
    @Test
    public void testTiebreakOnIdentifierIsStable() {
        List<RecipeEntry> entries = Lists.newArrayList(
                newEntry(0, "oak planks", "othermod:oak_planks_from_wood"),
                newEntry(1, "oak planks", "minecraft:oak_planks"),
                newEntry(2, "oak planks", "anothermod:oak_planks")
        );
        List<String> expected = Lists.newArrayList(
                "anothermod:oak_planks", "minecraft:oak_planks", "othermod:oak_planks_from_wood");
        assertThat(sortedIdentifiers(entries), is(expected));

        // The same entries in a different receive order must sort identically
        List<RecipeEntry> reversed = Lists.newArrayList(entries);
        java.util.Collections.reverse(reversed);
        assertThat(sortedIdentifiers(reversed), is(expected));
    }

    @Test
    public void testEqualNamesAndIdentifiersAreEqual() {
        assertThat(RecipeEntry.COMPARATOR.compare(
                newEntry(0, "chest", "minecraft:chest"),
                newEntry(1, "chest", "minecraft:chest")), is(0));
    }

}
