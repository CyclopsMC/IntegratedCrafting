package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Maps;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author rubensworks
 */
public class TestCraftingJob {

    private CraftingJob job;

    @Before
    public void beforeEach() {
        job = new CraftingJob(0, 0, null, 3, new MixedIngredients(Maps.newIdentityHashMap()));
    }

    @Test
    public void testAmountTotalDefaultsToAmount() {
        assertThat(job.getAmount(), equalTo(3));
        assertThat(job.getAmountTotal(), equalTo(3));
    }

    @Test
    public void testAmountTotalIsUnaffectedByCrafting() {
        // The amount is decremented for every crafted operation, the total is not
        job.setAmount(1);

        assertThat(job.getAmount(), equalTo(1));
        assertThat(job.getAmountTotal(), equalTo(3));
    }

    @Test
    public void testCloneRetainsInitiator() {
        // Jobs are cloned when they are distributed over multiple crafting interfaces,
        // so the clones have to remain attributable to whoever requested them.
        job.setInitiatorUuid("00000000-0000-0000-0000-00000000beef");
        job.setNotifyInitiator(true);

        CraftingJob clone = job.clone(() -> 1);

        assertThat(clone.getInitiatorUuid(), equalTo("00000000-0000-0000-0000-00000000beef"));
        assertThat(clone.isNotifyInitiator(), equalTo(true));
    }

    @Test
    public void testCloneWithoutInitiator() {
        CraftingJob clone = job.clone(() -> 1);

        assertThat(clone.getInitiatorUuid(), equalTo(null));
        assertThat(clone.isNotifyInitiator(), equalTo(false));
    }

}
