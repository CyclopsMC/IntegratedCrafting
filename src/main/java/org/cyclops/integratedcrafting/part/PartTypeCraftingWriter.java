package org.cyclops.integratedcrafting.part;

import com.google.common.collect.Lists;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integratedcrafting.GeneralConfig;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.part.aspect.IAspect;
import org.cyclops.integrateddynamics.core.part.aspect.AspectRegistry;
import org.cyclops.integrateddynamics.core.part.write.PartStateWriterBase;
import org.cyclops.integrateddynamics.core.part.write.PartTypeWriteBase;
import org.cyclops.integrateddynamics.part.aspect.Aspects;

/**
 * @author rubensworks
 */
public class PartTypeCraftingWriter extends PartTypeWriteBase<PartTypeCraftingWriter, PartStateWriterBase<PartTypeCraftingWriter>> {

    public PartTypeCraftingWriter(String name) {
        super(name);
        AspectRegistry.getInstance().register(this, Lists.<IAspect>newArrayList(
                CraftingAspects.Write.RECIPE_CRAFT,
                CraftingAspects.Write.ITEMSTACK_CRAFT,
                CraftingAspects.Write.FLUIDSTACK_CRAFT,
                CraftingAspects.Write.ENERGY_CRAFT
        ));
    }

    @Override
    public PartStateWriterBase<PartTypeCraftingWriter> constructDefaultState() {
        return new PartStateWriterBase<>(Aspects.REGISTRY.getAspects(this).size());
    }
    
    @Override
    public int getConsumptionRate(PartStateWriterBase<PartTypeCraftingWriter> state) {
        return GeneralConfig.craftingWriterBaseConsumption;
    }

    @Override
    public ModBase getMod() {
        return IntegratedCrafting._instance;
    }

    @Override
    public ModBase getModGui() {
        return IntegratedDynamics._instance;
    }

}
