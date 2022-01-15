package org.cyclops.integratedcrafting.proxy;

import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.proxy.ClientProxyComponent;
import org.cyclops.integratedcrafting.IntegratedCrafting;

/**
 * Proxy for the client side.
 *
 * @author rubensworks
 *
 */
public class ClientProxy extends ClientProxyComponent {

    public ClientProxy() {
        super(new CommonProxy());
    }

    @Override
    public ModBase getMod() {
        return IntegratedCrafting._instance;
    }

}
