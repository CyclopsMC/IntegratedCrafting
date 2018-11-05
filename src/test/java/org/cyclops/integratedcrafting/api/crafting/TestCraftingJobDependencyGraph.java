package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author rubensworks
 */
public class TestCraftingJobDependencyGraph {

    private static final CraftingJob J0 = new CraftingJob(0, 0, null, 1);
    private static final CraftingJob J1 = new CraftingJob(1, 0, null, 1);
    private static final CraftingJob J2 = new CraftingJob(2, 0, null, 1);
    private static final CraftingJob J3 = new CraftingJob(3, 0, null, 1);
    private static final CraftingJob J4 = new CraftingJob(4, 0, null, 1);

    private CraftingJobDependencyGraph g;

    @Before
    public void beforeEach() {
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

}
