package org.cyclops.integratedcrafting.gametest;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.integratedcrafting.Reference;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.event.CraftingJobFinishedEvent;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import java.util.List;
import java.util.UUID;

/**
 * Game tests for {@link CraftingJobFinishedEvent}.
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsCraftingJobFinishedEvent {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    /**
     * A job that runs to completion emits an event for the requested job.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testEventOnCompletedJob(GameTestHelper helper) {
        prepareNetwork(helper);
        UUID initiator = UUID.randomUUID();
        EventCollector collector = EventCollector.start(initiator);

        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> scheduleChestJob(helper, initiator, true))
                .thenWaitUntil(() -> helper.assertTrue(collector.getRootJobs().size() == 1,
                        "Expected exactly one completion event for the requested job, but got "
                                + collector.getRootJobs().size()))
                .thenExecute(() -> {
                    CraftingJob craftingJob = collector.getRootJobs().get(0);
                    helper.assertTrue(craftingJob.isNotifyInitiator(),
                            "The completed job did not carry the notify flag");
                    helper.assertFalse(craftingJob.isCancelled(),
                            "The completed job was marked as cancelled");
                    helper.assertTrue(craftingJob.getAmountTotal() == 1,
                            "The completed job did not retain its total amount, but had "
                                    + craftingJob.getAmountTotal());
                    helper.assertTrue(craftingJob.getAmount() == 0,
                            "The completed job still had a remaining amount");
                    collector.stop();
                })
                .thenSucceed();
    }

    /**
     * The notify flag is not set when the initiator did not ask to be notified.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testEventWithoutNotifyFlag(GameTestHelper helper) {
        prepareNetwork(helper);
        UUID initiator = UUID.randomUUID();
        EventCollector collector = EventCollector.start(initiator);

        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> scheduleChestJob(helper, initiator, false))
                .thenWaitUntil(() -> helper.assertTrue(collector.getRootJobs().size() == 1,
                        "Expected exactly one completion event for the requested job"))
                .thenExecute(() -> {
                    helper.assertFalse(collector.getRootJobs().get(0).isNotifyInitiator(),
                            "The completed job carried the notify flag");
                    collector.stop();
                })
                .thenSucceed();
    }

    /**
     * A job that is cancelled does not emit a completion event.
     */
    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testNoEventOnCancelledJob(GameTestHelper helper) {
        prepareNetwork(helper);
        UUID initiator = UUID.randomUUID();
        EventCollector collector = EventCollector.start(initiator);

        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    CraftingJob craftingJob = scheduleChestJob(helper, initiator, true);
                    helper.assertTrue(CraftingHelpers.getCraftingNetworkChecked(getNetwork(helper))
                                    .cancelCraftingJob(craftingJob.getChannel(), craftingJob.getId()),
                            "The crafting job could not be cancelled");
                })
                .thenIdle(200)
                .thenExecute(() -> {
                    helper.assertTrue(collector.getAllJobs().isEmpty(),
                            "A completion event was emitted for a cancelled job");
                    collector.stop();
                })
                .thenSucceed();
    }

    private static void prepareNetwork(GameTestHelper helper) {
        GameTestHelpersIntegratedCrafting.INetworkPositions<PartTypeInterfaceCrafting.State> positions =
                GameTestHelpersIntegratedCrafting.createBasicNetwork(helper, POS);

        // Insert crafting inputs in the interface chest
        ChestBlockEntity chest = helper.getBlockEntity(POS.east());
        chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));

        // Add the chest recipe to the crafting interface
        positions.interfaceRecipeAdders().get(0).accept(Triple.of(0, RecipeType.CRAFTING,
                ResourceLocation.fromNamespaceAndPath("minecraft", "chest")));
    }

    private static CraftingJob scheduleChestJob(GameTestHelper helper, UUID initiator, boolean notifyInitiator) {
        INetwork network = getNetwork(helper);
        int channel = IPositionedAddonsNetworkIngredients.DEFAULT_CHANNEL;
        try {
            CraftingJobDependencyGraph dependencyGraph = new CraftingJobDependencyGraph();
            CraftingJob craftingJob = CraftingHelpers.calculateCraftingJobs(network, channel,
                    IngredientComponents.ITEMSTACK, new ItemStack(Items.CHEST), ItemMatch.ITEM, true,
                    CraftingHelpers.getGlobalCraftingJobIdentifier(), dependencyGraph, false);
            CraftingHelpers.scheduleCraftingJobs(CraftingHelpers.getCraftingNetworkChecked(network),
                    CraftingHelpers.getNetworkStorageGetter(network, channel, false), dependencyGraph, true,
                    initiator, notifyInitiator);
            return craftingJob;
        } catch (Exception e) {
            throw new IllegalStateException("Crafting job could not be scheduled", e);
        }
    }

    private static INetwork getNetwork(GameTestHelper helper) {
        return NetworkHelpers.getNetwork(helper.getLevel(), helper.absolutePos(POS), null)
                .orElseThrow(() -> new IllegalStateException("Could not find a network"));
    }

    /**
     * Collects the events of a single initiator, so that concurrently running tests don't interfere.
     */
    public static class EventCollector {

        private final UUID initiator;
        private final List<CraftingJob> allJobs = Lists.newArrayList();
        private final List<CraftingJob> rootJobs = Lists.newArrayList();

        public EventCollector(UUID initiator) {
            this.initiator = initiator;
        }

        public static EventCollector start(UUID initiator) {
            EventCollector collector = new EventCollector(initiator);
            NeoForge.EVENT_BUS.register(collector);
            return collector;
        }

        public void stop() {
            NeoForge.EVENT_BUS.unregister(this);
        }

        public List<CraftingJob> getAllJobs() {
            return allJobs;
        }

        public List<CraftingJob> getRootJobs() {
            return rootJobs;
        }

        @SubscribeEvent
        public void onCraftingJobFinished(CraftingJobFinishedEvent event) {
            CraftingJob craftingJob = event.getCraftingJob();
            if (!this.initiator.toString().equals(craftingJob.getInitiatorUuid())) {
                return;
            }
            this.allJobs.add(craftingJob);
            if (event.isRootJob()) {
                this.rootJobs.add(craftingJob);
            }
        }
    }

}
