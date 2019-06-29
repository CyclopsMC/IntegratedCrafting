package org.cyclops.integratedcrafting.api.crafting;

import java.util.Collection;
import java.util.Collections;

/**
 * An exception for when a crafting job could not be started due to no crafting interfaces being available.
 *
 * @author rubensworks
 */
public class UnavailableCraftingInterfacesException extends Exception {

    private final Collection<CraftingJob> craftingJobs;

    public UnavailableCraftingInterfacesException(CraftingJob craftingJob) {
        this(Collections.singleton(craftingJob));
    }

    public UnavailableCraftingInterfacesException(Collection<CraftingJob> craftingJobs) {
        this.craftingJobs = craftingJobs;
    }

    public Collection<CraftingJob> getCraftingJobs() {
        return craftingJobs;
    }
}
