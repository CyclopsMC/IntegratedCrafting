package org.cyclops.integratedcrafting.core;

import com.google.common.collect.Lists;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverride;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverrideRegistry;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link ICraftingProcessOverrideRegistry}.
 * @author rubensworks
 */
public class CraftingProcessOverrideRegistry implements ICraftingProcessOverrideRegistry {

    private static CraftingProcessOverrideRegistry INSTANCE = new CraftingProcessOverrideRegistry();

    private final List<ICraftingProcessOverride> overrides = Lists.newArrayList();

    private CraftingProcessOverrideRegistry() {

    }

    public static CraftingProcessOverrideRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public <T extends ICraftingProcessOverride> T register(T craftingProcessOverride) {
        overrides.add(craftingProcessOverride);
        return craftingProcessOverride;
    }

    @Override
    public Collection<ICraftingProcessOverride> getCraftingProcessOverrides() {
        return overrides;
    }
}
