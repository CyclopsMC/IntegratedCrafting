package org.cyclops.integratedcrafting.recipe.type;

import net.minecraft.world.item.crafting.RecipeSerializer;
import org.cyclops.cyclopscore.config.extendedconfig.RecipeConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.integratedcrafting.IntegratedCrafting;

/**
 * Config for {@link RecipeDeadBush}.
 * @author rubensworks
 */
public class RecipeSerializerDeadBushConfig extends RecipeConfigCommon<RecipeDeadBush, IModBase> {

    public RecipeSerializerDeadBushConfig() {
        super(IntegratedCrafting._instance,
                "crafting_special_dead_bush",
                eConfig -> new RecipeSerializer<>(RecipeDeadBush.MAP_CODEC, RecipeDeadBush.STREAM_CODEC));
    }

}
