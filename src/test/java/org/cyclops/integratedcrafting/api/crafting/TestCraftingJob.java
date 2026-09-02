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

}
