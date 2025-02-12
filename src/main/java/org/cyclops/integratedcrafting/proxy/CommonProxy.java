package org.cyclops.integratedcrafting.proxy;

import org.cyclops.cyclopscore.init.ModBaseNeoForge;
import org.cyclops.cyclopscore.proxy.CommonProxyComponent;
import org.cyclops.integratedcrafting.IntegratedCrafting;

/**
 * Proxy for server and client side.
 * @author rubensworks
 *
 */
public class CommonProxy extends CommonProxyComponent {

    @Override
    public ModBaseNeoForge<?> getMod() {
        return IntegratedCrafting._instance;
    }

}
