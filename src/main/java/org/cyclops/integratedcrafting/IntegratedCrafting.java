package org.cyclops.integratedcrafting;

import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.apache.logging.log4j.Level;
import org.cyclops.cyclopscore.config.ConfigHandlerCommon;
import org.cyclops.cyclopscore.infobook.IInfoBookRegistry;
import org.cyclops.cyclopscore.init.ModBaseNeoForge;
import org.cyclops.cyclopscore.persist.world.GlobalCounters;
import org.cyclops.cyclopscore.proxy.IClientProxy;
import org.cyclops.cyclopscore.proxy.ICommonProxy;
import org.cyclops.integratedcrafting.api.crafting.ICraftingProcessOverrideRegistry;
import org.cyclops.integratedcrafting.capability.network.CraftingNetworkCapabilityConstructors;
import org.cyclops.integratedcrafting.command.CommandGenerateCrafting;
import org.cyclops.integratedcrafting.capability.network.NetworkCraftingHandlerCraftingNetwork;
import org.cyclops.integratedcrafting.core.CraftingProcessOverrideRegistry;
import org.cyclops.integratedcrafting.core.CraftingProcessOverrides;
import org.cyclops.integratedcrafting.gametest.GameTestsAdvancements;
import org.cyclops.integratedcrafting.gametest.GameTestsAttunedRecipes;
import org.cyclops.integratedcrafting.gametest.GameTestsCraftingJobFinishedEvent;
import org.cyclops.integratedcrafting.gametest.GameTestsItemsCraft;
import org.cyclops.integratedcrafting.gametest.GameTestsItemsMechanicalDryingBasin;
import org.cyclops.integratedcrafting.gametest.GameTestsItemsMechanicalSqueezer;
import org.cyclops.integratedcrafting.gametest.GameTestsItemsSmithing;
import org.cyclops.integratedcrafting.gametest.GameTestsItemsStonecutting;
import org.cyclops.integratedcrafting.gametest.GameTestsPartOffsets;
import org.cyclops.integratedcrafting.gametest.GameTestsPerformance;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedOffsetsConfig;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingAttunedRecipesConfig;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingConfig;
import org.cyclops.integratedcrafting.inventory.container.ContainerPartInterfaceCraftingSettingsConfig;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integratedcrafting.proxy.ClientProxy;
import org.cyclops.integratedcrafting.proxy.CommonProxy;
import org.cyclops.integratedcrafting.recipe.type.RecipeSerializerDeadBushConfig;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.network.INetworkCraftingHandlerRegistry;
import org.cyclops.integrateddynamics.core.event.IntegratedDynamicsSetupEvent;
import org.cyclops.integrateddynamics.infobook.OnTheDynamicsOfIntegrationBook;
import org.cyclops.integrateddynamics.part.aspect.Aspects;

/**
 * The main mod class of this mod.
 * @author rubensworks (aka kroeserr)
 *
 */
@Mod(Reference.MOD_ID)
public class IntegratedCrafting extends ModBaseNeoForge<IntegratedCrafting> {

    public static IntegratedCrafting _instance;

    public static GlobalCounters.Access globalCounters = null;

    public IntegratedCrafting(IEventBus modEventBus) {
        super(Reference.MOD_ID, (instance) -> _instance = instance, modEventBus);

        // Registries
        getRegistryManager().addRegistry(ICraftingProcessOverrideRegistry.class, CraftingProcessOverrideRegistry.getInstance());

        // Register world storages
        registerWorldStorage(globalCounters = new GlobalCounters.Access(this));

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
    protected LiteralArgumentBuilder<CommandSourceStack> constructBaseCommand(Commands.CommandSelection selection, CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> root = super.constructBaseCommand(selection, context);

        root.then(CommandGenerateCrafting.make());

        return root;
    }

    @Override
    protected void setup(FMLCommonSetupEvent event) {
        super.setup(event);
    }

    protected void onSetup(IntegratedDynamicsSetupEvent event) {
        Aspects.REGISTRY.register(org.cyclops.integrateddynamics.core.part.PartTypes.NETWORK_READER, Lists.newArrayList(
                CraftingAspects.Read.Network.RECIPES,
                CraftingAspects.Read.Network.CRAFTING_JOBS,
                CraftingAspects.Read.Network.CRAFTING_INGREDIENTS
        ));

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
    public void onConfigsRegister(ConfigHandlerCommon configHandler) {
        super.onConfigsRegister(configHandler);

        configHandler.addConfigurable(new GeneralConfig());

        configHandler.addConfigurable(new ContainerPartInterfaceCraftingConfig());
        configHandler.addConfigurable(new ContainerPartInterfaceCraftingSettingsConfig());
        configHandler.addConfigurable(new ContainerPartInterfaceCraftingAttunedRecipesConfig());
        configHandler.addConfigurable(new ContainerPartInterfaceCraftingAttunedOffsetsConfig());

        configHandler.addConfigurable(new RecipeSerializerDeadBushConfig()); // This one is only used in game tests.
    }

    @Override
    public Class<?>[] getGameTestClasses() {
        return new Class<?>[]{
                GameTestsAdvancements.class,
                GameTestsAttunedRecipes.class,
                GameTestsCraftingJobFinishedEvent.class,
                GameTestsItemsCraft.class,
                GameTestsItemsMechanicalDryingBasin.class,
                GameTestsItemsSmithing.class,
                GameTestsItemsMechanicalSqueezer.class,
                GameTestsItemsStonecutting.class,
                GameTestsPartOffsets.class,
                GameTestsPerformance.class
        };
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
