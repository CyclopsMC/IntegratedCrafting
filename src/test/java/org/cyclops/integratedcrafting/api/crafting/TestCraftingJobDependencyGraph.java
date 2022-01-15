package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.integratedcrafting.ingredient.IngredientComponentStubs;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author rubensworks
 */
public class TestCraftingJobDependencyGraph {

    private static CraftingJob J0;
    private static CraftingJob J1;
    private static CraftingJob J2;
    private static CraftingJob J3;
    private static CraftingJob J4;

    private CraftingJobDependencyGraph g;

    @Before
    public void beforeEach() {
        J0 = new CraftingJob(0, 0, null, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        J1 = new CraftingJob(1, 0, null, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        J2 = new CraftingJob(2, 0, null, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        J3 = new CraftingJob(3, 0, null, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        J4 = new CraftingJob(4, 0, null, 1, new MixedIngredients(Maps.newIdentityHashMap()));

        g = new CraftingJobDependencyGraph();
    }

    @Test
    public void testEmpty() {
        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testSingleDependency() {
        g.addDependency(J0, J1);
        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));
    }

    @Test
    public void testSingleDependencyDouble() {
        g.addDependency(J0, J1);
        g.addDependency(J0, J1);
        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));
    }

    @Test
    public void testSingleDependencyRemove() {
        g.addDependency(J0, J1);
        g.removeDependency(0, 1);
        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testTripleDependency() {
        g.addDependency(J0, J1);
        g.addDependency(J0, J2);
        g.addDependency(J0, J3);
        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1, J2, J3)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1, J2, J3)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependencies(J3)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J0)));
        assertThat(Sets.newHashSet(g.getDependents(J3)), equalTo(Sets.newHashSet(J0)));
    }

    @Test
    public void testTripleDependencyRemove() {
        g.addDependency(J0, J1);
        g.addDependency(J0, J2);
        g.addDependency(J0, J3);


        g.removeDependency(0, 1);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J2, J3)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J2, J3)));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependencies(J3)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J0)));
        assertThat(Sets.newHashSet(g.getDependents(J3)), equalTo(Sets.newHashSet(J0)));


        g.removeDependency(0, 2);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J3)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J3)));
        assertThat(Sets.newHashSet(g.getDependencies(J3)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J3)), equalTo(Sets.newHashSet(J0)));


        g.removeDependency(0, 3);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testTripleDependencyNested() {
        g.addDependency(J0, J1);
        g.addDependency(J1, J2);
        g.addDependency(J2, J3);
        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1, J2, J3)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet(J2)));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet(J3)));
        assertThat(Sets.newHashSet(g.getDependencies(J3)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependents(J3)), equalTo(Sets.newHashSet(J2)));
    }

    @Test
    public void testTripleDependencyNestedRemove() {
        g.addDependency(J0, J1);
        g.addDependency(J1, J2);
        g.addDependency(J2, J3);


        g.removeDependency(0, 1);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J1, J2, J3)));

        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet(J2)));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet(J3)));
        assertThat(Sets.newHashSet(g.getDependencies(J3)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependents(J3)), equalTo(Sets.newHashSet(J2)));


        g.removeDependency(2, 3);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J1, J2)));

        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet(J2)));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J1)));

        g.removeDependency(1, 2);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testImport() {
        CraftingJobDependencyGraph gOther = new CraftingJobDependencyGraph();
        gOther.addDependency(J0, J1);
        gOther.addDependency(J1, J2);
        gOther.addDependency(J2, J3);
        g.importDependencies(gOther);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1, J2, J3)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet(J2)));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet(J3)));
        assertThat(Sets.newHashSet(g.getDependencies(J3)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependents(J3)), equalTo(Sets.newHashSet(J2)));
    }

    @Test
    public void testRemoveAndReAdd() {
        g.addDependency(J0, J1);
        g.addDependency(J1, J2);
        g.addDependency(J2, J3);

        g.removeCraftingJobId(J1);

        g.addCraftingJobId(J1);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1, J2, J3)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet(J2)));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet(J3)));
        assertThat(Sets.newHashSet(g.getDependencies(J3)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependents(J3)), equalTo(Sets.newHashSet(J2)));
    }

    @Test(expected = IllegalStateException.class)
    public void testOnFinishedInvalid1() {
        g.addDependency(J0, J1);
        g.addDependency(J1, J2);
        g.addDependency(J2, J3);

        g.onCraftingJobFinished(J2);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnFinishedInvalid2() {
        g.addDependency(J0, J1);
        g.addDependency(J1, J2);
        g.addDependency(J2, J3);

        g.onCraftingJobFinished(J1);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnFinishedInvalid3() {
        g.addDependency(J0, J1);
        g.addDependency(J1, J2);
        g.addDependency(J2, J3);

        g.onCraftingJobFinished(J0);
    }

    @Test
    public void testOnFinished() {
        g.addDependency(J0, J1);
        g.addDependency(J1, J2);
        g.addDependency(J2, J3);

        g.onCraftingJobFinished(J3);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1, J2)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet(J2)));
        assertThat(Sets.newHashSet(g.getDependencies(J2)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));
        assertThat(Sets.newHashSet(g.getDependents(J2)), equalTo(Sets.newHashSet(J1)));

        g.onCraftingJobFinished(J2);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J1)));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet(J1)));
        assertThat(Sets.newHashSet(g.getDependencies(J1)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
        assertThat(Sets.newHashSet(g.getDependents(J1)), equalTo(Sets.newHashSet(J0)));

        g.onCraftingJobFinished(J1);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependencies(J0)), equalTo(Sets.newHashSet()));

        assertThat(Sets.newHashSet(g.getDependents(J0)), equalTo(Sets.newHashSet()));
    }

    @Test
    public void testMergeCraftingJobsSimple() {
        g.addCraftingJobId(J0);
        g.addCraftingJobId(J1);

        g.mergeCraftingJobs(J0, J1, true);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0)));
        assertThat(J0.getAmount(), equalTo(2));

        assertThat(J0.getDependencyCraftingJobs(), equalTo(Lists.newArrayList()));
        assertThat(g.getDependencies(J0), equalTo(Lists.newArrayList()));
    }

    @Test
    public void testMergeCraftingJobsDependenciesMatching() {
        IRecipeDefinition R0 = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(Maps.newIdentityHashMap()));

        CraftingJob J0D = new CraftingJob(1000, 0, R0, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        CraftingJob J1D = new CraftingJob(1001, 0, R0, 1, new MixedIngredients(Maps.newIdentityHashMap()));

        J0.addDependency(J0D);
        g.addDependency(J0, J0D);
        J1.addDependency(J1D);
        g.addDependency(J1, J1D);

        g.mergeCraftingJobs(J0, J1, true);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J0D)));
        assertThat(J0.getAmount(), equalTo(2));

        assertThat(J0.getDependencyCraftingJobs(), equalTo(Lists.newArrayList(1000)));
        assertThat(g.getDependencies(J0), equalTo(Lists.newArrayList(J0D)));
        assertThat(J0D.getAmount(), equalTo(2));
        assertThat(J0D.getDependencyCraftingJobs(), equalTo(Lists.newArrayList()));
        assertThat(g.getDependencies(J0D), equalTo(Lists.newArrayList()));
        assertThat(J0D.getDependentCraftingJobs(), equalTo(Lists.newArrayList(0)));
        assertThat(g.getDependents(J0D), equalTo(Lists.newArrayList(J0)));
    }

    @Test
    public void testMergeCraftingJobsDependenciesNonMatching() {
        IRecipeDefinition R0 = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(Maps.newIdentityHashMap()));
        Map<IngredientComponent<?, ?>, List<?>> o1 = Maps.newIdentityHashMap();
        o1.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(0L));
        IRecipeDefinition R1 = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(o1));

        CraftingJob J0D = new CraftingJob(1000, 0, R0, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        CraftingJob J1D = new CraftingJob(1001, 0, R1, 1, new MixedIngredients(Maps.newIdentityHashMap()));

        J0.addDependency(J0D);
        g.addDependency(J0, J0D);
        J1.addDependency(J1D);
        g.addDependency(J1, J1D);

        g.mergeCraftingJobs(J0, J1, true);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J0D, J1D)));
        assertThat(J0.getAmount(), equalTo(2));

        assertThat(J0.getDependencyCraftingJobs(), equalTo(Lists.newArrayList(1000, 1001)));
        assertThat(g.getDependencies(J0), equalTo(Lists.newArrayList(J0D, J1D)));

        assertThat(J0D.getAmount(), equalTo(1));
        assertThat(J0D.getDependencyCraftingJobs(), equalTo(Lists.newArrayList()));
        assertThat(g.getDependencies(J0D), equalTo(Lists.newArrayList()));
        assertThat(J0D.getDependentCraftingJobs(), equalTo(Lists.newArrayList(0)));
        assertThat(g.getDependents(J0D), equalTo(Lists.newArrayList(J0)));

        assertThat(J1D.getAmount(), equalTo(1));
        assertThat(J1D.getDependencyCraftingJobs(), equalTo(Lists.newArrayList()));
        assertThat(g.getDependencies(J1D), equalTo(Lists.newArrayList()));
        assertThat(J1D.getDependentCraftingJobs(), equalTo(Lists.newArrayList(0)));
        assertThat(g.getDependents(J1D), equalTo(Lists.newArrayList(J0)));
    }

    @Test
    public void testMergeCraftingJobsDependenciesMatchingAndNonMatching() {
        IRecipeDefinition R0 = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(Maps.newIdentityHashMap()));
        Map<IngredientComponent<?, ?>, List<?>> o1 = Maps.newIdentityHashMap();
        o1.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(0L));
        IRecipeDefinition R1 = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(o1));
        Map<IngredientComponent<?, ?>, List<?>> o2 = Maps.newIdentityHashMap();
        o2.put(IngredientComponentStubs.SIMPLE, Lists.newArrayList(1L));
        IRecipeDefinition R2 = new RecipeDefinition(Maps.newIdentityHashMap(), new MixedIngredients(o2));

        CraftingJob J00D = new CraftingJob(1000, 0, R0, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        CraftingJob J01D = new CraftingJob(1001, 0, R1, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        CraftingJob J10D = new CraftingJob(1002, 0, R0, 1, new MixedIngredients(Maps.newIdentityHashMap()));
        CraftingJob J11D = new CraftingJob(1003, 0, R2, 1, new MixedIngredients(Maps.newIdentityHashMap()));

        J0.addDependency(J00D);
        J0.addDependency(J01D);
        g.addDependency(J0, J00D);
        g.addDependency(J0, J01D);
        J1.addDependency(J10D);
        J1.addDependency(J11D);
        g.addDependency(J1, J10D);
        g.addDependency(J1, J11D);

        g.mergeCraftingJobs(J0, J1, true);

        assertThat(Sets.newHashSet(g.getCraftingJobs()), equalTo(Sets.newHashSet(J0, J00D, J01D, J11D)));
        assertThat(J0.getAmount(), equalTo(2));

        assertThat(J0.getDependencyCraftingJobs(), equalTo(Lists.newArrayList(1000, 1001, 1003)));
        assertThat(g.getDependencies(J0), equalTo(Lists.newArrayList(J00D, J01D, J11D)));

        assertThat(J00D.getAmount(), equalTo(2));
        assertThat(J00D.getDependencyCraftingJobs(), equalTo(Lists.newArrayList()));
        assertThat(g.getDependencies(J00D), equalTo(Lists.newArrayList()));
        assertThat(J00D.getDependentCraftingJobs(), equalTo(Lists.newArrayList(0)));
        assertThat(g.getDependents(J00D), equalTo(Lists.newArrayList(J0)));

        assertThat(J01D.getAmount(), equalTo(1));
        assertThat(J01D.getDependencyCraftingJobs(), equalTo(Lists.newArrayList()));
        assertThat(g.getDependencies(J01D), equalTo(Lists.newArrayList()));
        assertThat(J01D.getDependentCraftingJobs(), equalTo(Lists.newArrayList(0)));
        assertThat(g.getDependents(J01D), equalTo(Lists.newArrayList(J0)));

        assertThat(J11D.getAmount(), equalTo(1));
        assertThat(J11D.getDependencyCraftingJobs(), equalTo(Lists.newArrayList()));
        assertThat(g.getDependencies(J11D), equalTo(Lists.newArrayList()));
        assertThat(J11D.getDependentCraftingJobs(), equalTo(Lists.newArrayList(0)));
        assertThat(g.getDependents(J11D), equalTo(Lists.newArrayList(J0)));
    }

}
