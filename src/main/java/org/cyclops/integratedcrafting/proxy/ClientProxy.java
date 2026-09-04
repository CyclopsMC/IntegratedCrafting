package org.cyclops.integratedcrafting.proxy;

import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.proxy.ClientProxyComponent;
import org.cyclops.integratedcrafting.IntegratedCrafting;
import org.cyclops.integratedcrafting.client.gui.tooltip.ClientRecipeInputsTooltip;
import org.cyclops.integratedcrafting.client.gui.tooltip.RecipeInputsTooltip;

/**
 * Proxy for the client side.
 *
 * @author rubensworks
 *
 */
public class ClientProxy extends ClientProxyComponent {

    public ClientProxy() {
        super(new CommonProxy());

        getMod().getModEventBus().addListener(this::registerClientTooltipComponentFactories);
    }

    public void registerClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RecipeInputsTooltip.class, ClientRecipeInputsTooltip::new);
    }

    @Override
    public ModBase getMod() {
        return IntegratedCrafting._instance;
    }

}
