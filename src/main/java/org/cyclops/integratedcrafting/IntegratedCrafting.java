package org.cyclops.integratedcrafting;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.apache.logging.log4j.Level;
import org.cyclops.cyclopscore.config.ConfigHandler;
import org.cyclops.cyclopscore.infobook.IInfoBookRegistry;
import org.cyclops.cyclopscore.init.ModBaseVersionable;
import org.cyclops.cyclopscore.persist.world.GlobalCounters;
import org.cyclops.cyclopscore.proxy.IClientProxy;
import org.cyclops.cyclopscore.proxy.ICommonProxy;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverrideRegistry;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkCapabilityConstructors;
import org.cyclops.integratedcrafting.capability.network.NetworkCraftingHandlerCraftingNetwork;
import org.cyclops.integratedcrafting.core.CraftingProcessOverrideRegistry;
import org.cyclops.integratedcrafting.core.CraftingProcessOverrides;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingConfig;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettingsConfig;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integratedcrafting.proxy.ClientProxy;
import org.cyclops.integratedcrafting.proxy.CommonProxy;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.network.INetworkCraftingHandlerRegistry;
import org.cyclops.integrateddynamics.core.event.IntegratedDynamicsSetupEvent;
import org.cyclops.integrateddynamics.infobook.OnTheDynamicsOfIntegrationBook;

/**
 * The main mod class of this mod.
 * @author rubensworks (aka kroeserr)
 *
 */
@Mod(Reference.MOD_ID)
public class IntegratedCrafting extends ModBaseVersionable<IntegratedCrafting> {

    public static IntegratedCrafting _instance;

    public static GlobalCounters globalCounters = null;

    public IntegratedCrafting(IEventBus modEventBus) {
        super(Reference.MOD_ID, (instance) -> _instance = instance, modEventBus);

        // Registries
        getRegistryManager().addRegistry(ICraftingProcessOverrideRegistry.class, CraftingProcessOverrideRegistry.getInstance());

        // Register world storages
        registerWorldStorage(globalCounters = new GlobalCounters(this));

        modEventBus.addListener(this::onRegistriesCreate);
        modEventBus.addListener(this::onSetup);
        modEventBus.register(new CraftingNetworkCapabilityConstructors());
    }

    public void onRegistriesCreate(NewRegistryEvent event) {
        CraftingAspects.load();
        PartTypes.load();
        CraftingProcessOverrides.load();
    }

    @Override
    protected void setup(FMLCommonSetupEvent event) {
        super.setup(event);
    }

    protected void onSetup(IntegratedDynamicsSetupEvent event) {
        IntegratedDynamics._instance.getRegistryManager().getRegistry(INetworkCraftingHandlerRegistry.class)
                .register(new NetworkCraftingHandlerCraftingNetwork());

        // Initialize info book
        IntegratedDynamics._instance.getRegistryManager().getRegistry(IInfoBookRegistry.class)
                .registerSection(this,
                        OnTheDynamicsOfIntegrationBook.getInstance(), "info_book.integrateddynamics.manual",
                        "/data/" + Reference.MOD_ID + "/info/crafting_info.xml");
        IntegratedDynamics._instance.getRegistryManager().getRegistry(IInfoBookRegistry.class)
                .registerSection(this,
                        OnTheDynamicsOfIntegrationBook.getInstance(), "info_book.integrateddynamics.tutorials",
                        "/data/" + Reference.MOD_ID + "/info/crafting_tutorials.xml");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected IClientProxy constructClientProxy() {
        return new ClientProxy();
    }

    @Override
    protected ICommonProxy constructCommonProxy() {
        return new CommonProxy();
    }

    @Override
    protected CreativeModeTab.Builder constructDefaultCreativeModeTab(CreativeModeTab.Builder builder) {
        return super.constructDefaultCreativeModeTab(builder)
                .icon(() -> new ItemStack(RegistryEntries.ITEM_PART_INTERFACE_CRAFTING));
    }

    @Override
    public void onConfigsRegister(ConfigHandler configHandler) {
        super.onConfigsRegister(configHandler);

        configHandler.addConfigurable(new GeneralConfig());

        configHandler.addConfigurable(new ContainerPartInterfaceCraftingConfig());
        configHandler.addConfigurable(new ContainerPartInterfaceCraftingSettingsConfig());
    }

    /**
     * Log a new info message for this mod.
     * @param message The message to show.
     */
    public static void clog(String message) {
        clog(Level.INFO, message);
    }

    /**
     * Log a new message of the given level for this mod.
     * @param level The level in which the message must be shown.
     * @param message The message to show.
     */
    public static void clog(Level level, String message) {
        IntegratedCrafting._instance.getLoggerHelper().log(level, message);
    }

}
