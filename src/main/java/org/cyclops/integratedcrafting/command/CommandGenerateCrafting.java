package org.cyclops.integratedcrafting.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.cyclops.cyclopscore.command.argument.ArgumentTypeEnum;
import org.cyclops.integratedcrafting.core.part.PartTypeInterfaceCraftingBase;
import org.cyclops.integratedcrafting.gametest.GameTestHelpersIntegratedCrafting;
import org.cyclops.integratedcrafting.part.PartTypeInterfaceCrafting;
import org.cyclops.integratedcrafting.part.PartTypes;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspectWriteBuilders;
import org.cyclops.integratedcrafting.part.aspect.CraftingAspects;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.write.IPartStateWriter;
import org.cyclops.integrateddynamics.block.BlockCable;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.CableHelpers;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Command for generating crafting networks with different presets.
 *
 * These presets are used for performance benchmarking,
 * both from within game tests, and for manual profiling inside a real world.
 *
 * @author rubensworks
 */
public class CommandGenerateCrafting implements Command<CommandSourceStack> {

    public static LiteralArgumentBuilder<CommandSourceStack> make() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("generatecrafting")
                .requires((commandSource) -> commandSource.hasPermission(2));

        // Add the preset subcommand with optional size argument
        builder.then(Commands.argument("preset", new ArgumentTypeEnum(CraftingPreset.class))
                .executes(new CommandGenerateCraftingExecutor(true, false))
                .then(Commands.argument("size", IntegerArgumentType.integer(1, 100))
                        .executes(new CommandGenerateCraftingExecutor(true, true))));

        return builder;
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        context.getSource().sendFailure(Component.literal("Please specify one of the presets: " + joinPresets())
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    /**
     * @return All preset names, joined by a comma.
     */
    public static String joinPresets() {
        StringBuilder sb = new StringBuilder();
        for (CraftingPreset preset : CraftingPreset.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(preset.name().toLowerCase());
        }
        return sb.toString();
    }

    /**
     * The available network presets.
     */
    public enum CraftingPreset {
        /** Only cables, no parts: the baseline that the append benchmark grows from. */
        EMPTY,
        /** Idle crafting interfaces in front of crafting tables, each holding a single recipe. */
        INTERFACESIDLE,
        /** Idle crafting interfaces in front of crafting tables, each holding nine recipes. */
        INTERFACESIDLERECIPES,
        /** Crafting writers continuously requesting items that are crafted from stored planks. */
        CRAFTSIMPLE,
        /** Crafting writers requesting items whose ingredients must themselves be crafted first. */
        CRAFTNESTED,
        /** As {@link #CRAFTSIMPLE}, but with every crafting interface loaded with nine recipes. */
        CRAFTRECIPEINDEX,
        /** Crafting writers requesting items that are already in storage, so no job is ever scheduled. */
        CRAFTSATISFIEDIDLE,
        /** Remove everything that the other presets generate. */
        CLEAR,
    }

    /**
     * Executor for the generatecrafting command.
     */
    public static class CommandGenerateCraftingExecutor implements Command<CommandSourceStack> {
        private final boolean hasPreset;
        private final boolean hasSize;

        public CommandGenerateCraftingExecutor(boolean hasPreset, boolean hasSize) {
            this.hasPreset = hasPreset;
            this.hasSize = hasSize;
        }

        @Override
        public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            if (!hasPreset) {
                context.getSource().sendFailure(Component.literal("Please specify one of the presets: " + joinPresets())
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            CraftingPreset preset = ArgumentTypeEnum.getValue(context, "preset", CraftingPreset.class);
            ServerLevel level = context.getSource().getLevel();
            BlockPos playerPos = BlockPos.containing(context.getSource().getPosition());
            int size = hasSize ? IntegerArgumentType.getInteger(context, "size") : getDefaultSize(preset);

            if (preset == CraftingPreset.CLEAR) {
                context.getSource().sendSuccess(
                        () -> Component.literal("Clearing generated crafting networks within radius: " + size)
                                .withStyle(ChatFormatting.GREEN),
                        true);
                CraftingGenerationHelper.clearGrid(level, playerPos, size);
                return 1;
            }

            context.getSource().sendSuccess(
                    () -> Component.literal("Generating crafting preset: " + preset.name().toLowerCase()
                                    + " (size: " + size + "x" + size + "x" + size + ")")
                            .withStyle(ChatFormatting.GREEN),
                    true);
            CraftingGenerationHelper.generate(preset, level, playerPos.above(2), size);

            return 1;
        }

        /**
         * Get the default size for the given preset.
         */
        private int getDefaultSize(CraftingPreset preset) {
            return preset == CraftingPreset.CLEAR ? 50 : 9;
        }
    }

    /**
     * Helper class for crafting network generation logic, shared between command and game tests.
     *
     * All presets are built on top of the same grid layout:
     * cable planes at even Y levels, and a checkerboard of cables and free "cells" at odd Y levels.
     * Every cell is a free position that is surrounded by cables of a single network,
     * so it can hold a container or a crafter that is observed or targeted by parts on the surrounding cables.
     *
     * Every cell can carry up to two parts: one on the cable below it (pointing up),
     * and one on a horizontally neighbouring cable (pointing sideways).
     */
    public static class CraftingGenerationHelper {

        /**
         * The number of distinct recipes that fit in a single crafting interface.
         */
        public static final int RECIPES_PER_INTERFACE = 9;

        /**
         * The number of full stacks of crafting inputs that is placed in every storage chest.
         *
         * This deliberately leaves slots free in every chest.
         * A crafting interface buffers its results until it can push them into network storage,
         * and it stops ticking its jobs entirely while that buffer is non-empty.
         * Completely filled chests would therefore jam every crafting interface after a single craft.
         */
        public static final int STORAGE_STACKS = 12;

        /**
         * All crafting chains that the presets can request.
         *
         * Every chain is a wooden item that is crafted purely out of planks of a single wood species,
         * where those planks are in turn crafted out of a single log of that species.
         * This gives us a large pool of independent chains:
         * the crafting writer aspect refuses to schedule a job for an item that is already being crafted,
         * so every writer in a preset needs its own distinct output item to actually cause load.
         *
         * Only recipes that are shapeless, at most 2x2, or exactly 3 wide are usable here:
         * {@link org.cyclops.integratedcrafting.core.crafting.processoverride.CraftingGrid} fills a 3x3 grid
         * row by row, so a recipe that is 2 wide and 3 tall (such as doors) would end up misaligned.
         */
        public static final List<CraftingChain> CHAINS = Lists.newArrayList();

        /**
         * Extra recipes that are only used to fill up the recipe slots of crafting interfaces.
         *
         * These are never requested by a crafting writer: their only purpose is to grow the number of
         * recipes that the network's recipe index holds, and that every crafting interface has to
         * evaluate and validate.
         */
        public static final List<ResourceLocation> FILLER_RECIPES = Lists.newArrayList();

        /**
         * All recipes that a crafting interface can be filled up with,
         * which is every chain recipe followed by every filler recipe.
         */
        public static final List<ResourceLocation> ALL_RECIPES = Lists.newArrayList();

        /**
         * The subset of {@link #CHAINS} that requires at least 4 planks per craft.
         *
         * A planks recipe yields 4 planks at a time, so a request for at least 4 planks can never be
         * fully satisfied by the leftovers of a previous craft, which means such a chain keeps
         * requiring a nested planks job for as long as the benchmark runs.
         */
        public static final List<CraftingChain> CHAINS_NESTED = Lists.newArrayList();

        static {
            // Wood species that have a boat variant
            registerSpecies("oak", Items.OAK_LOG, Items.OAK_PLANKS, true);
            registerSpecies("spruce", Items.SPRUCE_LOG, Items.SPRUCE_PLANKS, true);
            registerSpecies("birch", Items.BIRCH_LOG, Items.BIRCH_PLANKS, true);
            registerSpecies("jungle", Items.JUNGLE_LOG, Items.JUNGLE_PLANKS, true);
            registerSpecies("acacia", Items.ACACIA_LOG, Items.ACACIA_PLANKS, true);
            registerSpecies("dark_oak", Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS, true);
            registerSpecies("mangrove", Items.MANGROVE_LOG, Items.MANGROVE_PLANKS, true);
            registerSpecies("cherry", Items.CHERRY_LOG, Items.CHERRY_PLANKS, true);
            // Nether wood species, which have no boat variant
            registerSpecies("crimson", Items.CRIMSON_STEM, Items.CRIMSON_PLANKS, false);
            registerSpecies("warped", Items.WARPED_STEM, Items.WARPED_PLANKS, false);

            for (CraftingChain chain : CHAINS) {
                if (!ALL_RECIPES.contains(chain.resultRecipe())) {
                    ALL_RECIPES.add(chain.resultRecipe());
                }
            }
            ALL_RECIPES.addAll(FILLER_RECIPES);
        }

        private static void registerSpecies(String species, Item log, Item planks, boolean hasBoat) {
            // Deep chains: at least 4 planks per craft, so a nested planks job is always required
            registerChain(species, log, planks, species + "_stairs", true);
            registerChain(species, log, planks, species + "_trapdoor", true);
            if (hasBoat) {
                registerChain(species, log, planks, species + "_boat", true);
            }
            // Shallow chains: fewer than 4 planks per craft
            registerChain(species, log, planks, species + "_slab", false);
            registerChain(species, log, planks, species + "_pressure_plate", false);
            registerChain(species, log, planks, species + "_button", false);
            // Filler recipes, which are never requested, and only grow the recipe index
            FILLER_RECIPES.add(ResourceLocation.withDefaultNamespace(species + "_planks"));
            FILLER_RECIPES.add(ResourceLocation.withDefaultNamespace(species + "_fence"));
            FILLER_RECIPES.add(ResourceLocation.withDefaultNamespace(species + "_fence_gate"));
            FILLER_RECIPES.add(ResourceLocation.withDefaultNamespace(species + "_sign"));
        }

        private static void registerChain(String species, Item log, Item planks, String resultName, boolean nested) {
            CraftingChain chain = new CraftingChain(
                    ResourceLocation.withDefaultNamespace(resultName),
                    ResourceLocation.withDefaultNamespace(species + "_planks"),
                    log,
                    planks
            );
            CHAINS.add(chain);
            if (nested) {
                CHAINS_NESTED.add(chain);
            }
        }

        /**
         * A single crafting chain: a wooden item that is crafted out of planks,
         * which are in turn crafted out of a log.
         *
         * @param resultRecipe The recipe of the requested item, which only consumes planks.
         * @param planksRecipe The recipe of the planks that the result recipe consumes.
         * @param log The log that the planks recipe consumes.
         * @param planks The planks that the planks recipe produces.
         */
        public record CraftingChain(ResourceLocation resultRecipe, ResourceLocation planksRecipe,
                                    Item log, Item planks) {
            /**
             * @return The item that the result recipe produces.
             */
            public Item getResultItem() {
                return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resultRecipe());
            }
        }

        /**
         * Generate the given preset.
         * @param preset The preset to generate.
         * @param level The level to generate in.
         * @param startPos The lowest corner of the generated grid.
         * @param size The edge length of the generated grid.
         */
        public static void generate(CraftingPreset preset, ServerLevel level, BlockPos startPos, int size) {
            switch (preset) {
                case EMPTY -> generateEmptyGrid(level, startPos, size);
                case INTERFACESIDLE -> generateInterfacesIdle(level, startPos, size);
                case INTERFACESIDLERECIPES -> generateInterfacesIdleRecipes(level, startPos, size);
                case CRAFTSIMPLE -> generateCraftSimple(level, startPos, size);
                case CRAFTNESTED -> generateCraftNested(level, startPos, size);
                case CRAFTRECIPEINDEX -> generateCraftRecipeIndex(level, startPos, size);
                case CRAFTSATISFIEDIDLE -> generateCraftSatisfiedIdle(level, startPos, size);
                case CLEAR -> clearGrid(level, startPos, size);
            }
        }

        /*
         * Grid construction
         */

        /**
         * Generate a grid of cables without any parts or containers.
         * @param level The level to generate in.
         * @param startPos The lowest corner of the generated grid.
         * @param size The edge length of the generated grid.
         */
        public static void generateEmptyGrid(ServerLevel level, BlockPos startPos, int size) {
            List<BlockPos> placedPositions = Lists.newArrayList();

            // Place all cables at once without triggering a network init for each of them,
            // as that would make generation unusably slow for larger sizes.
            BlockCable.SKIP_NETWORK_INIT = true;
            try {
                for (int x = 0; x < size; x++) {
                    for (int y = 0; y < size; y++) {
                        for (int z = 0; z < size; z++) {
                            if (isCablePosition(x, y, z)) {
                                BlockPos pos = startPos.offset(x, y, z);
                                level.setBlock(pos, RegistryEntries.BLOCK_CABLE.value().defaultBlockState(), 2);
                                placedPositions.add(pos);
                            }
                        }
                    }
                }
            } finally {
                BlockCable.SKIP_NETWORK_INIT = false;
            }

            for (BlockPos pos : placedPositions) {
                CableHelpers.updateConnectionsNeighbours(level, pos, CableHelpers.ALL_SIDES);
            }

            NetworkHelpers.initNetwork(level, startPos, null);
        }

        /**
         * @param x The local X coordinate within the grid.
         * @param y The local Y coordinate within the grid.
         * @param z The local Z coordinate within the grid.
         * @return If a cable should be placed at the given local grid coordinate.
         */
        private static boolean isCablePosition(int x, int y, int z) {
            // Even Y levels are fully filled with cables,
            // odd Y levels alternate between cables and free cells.
            return y % 2 == 0 || (x + z) % 2 == 0;
        }

        /**
         * Get all cell positions of the grid, in a deterministic order.
         *
         * A cell is a free position at an odd Y level that is surrounded by cables,
         * so that it can hold a container or crafter that is targeted by the surrounding parts.
         *
         * @param startPos The lowest corner of the grid.
         * @param size The edge length of the grid.
         * @return The absolute cell positions.
         */
        public static List<BlockPos> getCells(BlockPos startPos, int size) {
            List<BlockPos> cells = Lists.newArrayList();
            for (int y = 1; y < size; y += 2) {
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        if (!isCablePosition(x, y, z)) {
                            cells.add(startPos.offset(x, y, z));
                        }
                    }
                }
            }
            return cells;
        }

        /**
         * Get the direction from the given cell towards a horizontally neighbouring cable.
         * @param level The level.
         * @param cell A cell position.
         * @return The direction, or null if the cell has no horizontally neighbouring cable.
         */
        @Nullable
        public static Direction getSideCableDirection(ServerLevel level, BlockPos cell) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(cell.relative(direction)).getBlock() == RegistryEntries.BLOCK_CABLE.value()) {
                    return direction;
                }
            }
            return null;
        }

        /**
         * Add a part on the cable below the given cell, targeting the cell.
         */
        private static PartPos addPartBelow(ServerLevel level, BlockPos cell, IPartType partType) {
            BlockPos cablePos = cell.below();
            PartHelpers.addPart(level, cablePos, Direction.UP, partType, new ItemStack(partType.getItem()));
            return PartPos.of(level, cablePos, Direction.UP);
        }

        /**
         * Add a part on a cable next to the given cell, targeting the cell.
         * @return The position of the added part, or null if the cell has no horizontal cable neighbour.
         */
        @Nullable
        private static PartPos addPartBeside(ServerLevel level, BlockPos cell, IPartType partType) {
            Direction sideDirection = getSideCableDirection(level, cell);
            if (sideDirection == null) {
                return null;
            }
            BlockPos cablePos = cell.relative(sideDirection);
            Direction side = sideDirection.getOpposite();
            PartHelpers.addPart(level, cablePos, side, partType, new ItemStack(partType.getItem()));
            return PartPos.of(level, cablePos, side);
        }

        /**
         * Notify all cables around the given cells that their neighbours have changed,
         * so that newly added parts and containers are picked up by the network.
         */
        private static void updateCells(ServerLevel level, List<BlockPos> cells) {
            for (BlockPos cell : cells) {
                for (Direction direction : Direction.values()) {
                    BlockPos pos = cell.relative(direction);
                    if (level.getBlockState(pos).getBlock() == RegistryEntries.BLOCK_CABLE.value()) {
                        level.updateNeighborsAt(pos, RegistryEntries.BLOCK_CABLE.value());
                    }
                }
            }
        }

        /*
         * Cell roles
         */

        /**
         * Turn the given cell into network storage: a chest filled with crafting inputs,
         * exposed to the network by an item interface.
         * @param level The level.
         * @param cell The cell position.
         * @param item The item to fill the chest with.
         */
        public static void placeStorageCell(ServerLevel level, BlockPos cell, Item item) {
            level.setBlock(cell, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(cell) instanceof ChestBlockEntity chest) {
                // Only fill part of the chest: the remaining slots are needed to hold the crafting outputs.
                for (int slot = 0; slot < Math.min(STORAGE_STACKS, chest.getContainerSize()); slot++) {
                    ItemStack itemStack = new ItemStack(item);
                    itemStack.setCount(itemStack.getMaxStackSize());
                    chest.setItem(slot, itemStack);
                }
            }
            addPartBelow(level, cell, org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM);
        }

        /**
         * Turn the given cell into a crafter: a crafting table with a crafting interface pointing at it,
         * holding the given recipes.
         * @param level The level.
         * @param cell The cell position.
         * @param recipes The recipes to place in the crafting interface, at most {@link #RECIPES_PER_INTERFACE}.
         * @return The position of the crafting interface.
         */
        public static PartPos placeCrafterCell(ServerLevel level, BlockPos cell, List<ResourceLocation> recipes) {
            level.setBlock(cell, Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
            PartPos interfacePos = addPartBelow(level, cell, PartTypes.INTERFACE_CRAFTING);

            PartTypeInterfaceCraftingBase.State<?, ?> state = (PartTypeInterfaceCraftingBase.State<?, ?>)
                    PartHelpers.getPart(interfacePos).getState();
            if (state instanceof PartTypeInterfaceCrafting.State craftingState) {
                for (int slot = 0; slot < Math.min(recipes.size(), RECIPES_PER_INTERFACE); slot++) {
                    ItemStack variableRecipe = createRecipeVariable(level, recipes.get(slot));
                    if (!variableRecipe.isEmpty()) {
                        craftingState.getInventoryVariables().setItem(slot, variableRecipe);
                    }
                }
            }
            return interfacePos;
        }

        /**
         * Turn the given cell into a crafting writer that continuously requests the given item.
         *
         * The cell itself is left empty: the crafting writer aspect only operates on the network,
         * so it does not need a target block.
         *
         * @param level The level.
         * @param cell The cell position.
         * @param item The item to request.
         * @param ignoreStorage If the writer should request the item even when it is already in storage.
         *                      This is what turns a single craft into a continuous stream of crafting jobs.
         * @return The position of the crafting writer.
         */
        public static PartPos placeWriterCell(ServerLevel level, BlockPos cell, Item item, boolean ignoreStorage) {
            PartPos writerPos = addPartBelow(level, cell, PartTypes.CRAFTING_WRITER);
            GameTestHelpersIntegratedDynamics.placeVariableInWriter(level, writerPos, CraftingAspects.Write.ITEMSTACK_CRAFT,
                    GameTestHelpersIntegratedDynamics.createVariableForValue(level, ValueTypes.OBJECT_ITEMSTACK,
                            ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(item))));
            if (ignoreStorage) {
                GameTestHelpersIntegratedCrafting.setWriterAspectProperty(writerPos, CraftingAspects.Write.ITEMSTACK_CRAFT,
                        CraftingAspectWriteBuilders.PROP_IGNORE_STORAGE, ValueTypeBoolean.ValueBoolean.of(true));
            }
            return writerPos;
        }

        /**
         * Create a variable itemstack holding the given crafting recipe.
         * @param level The level.
         * @param recipe The name of a vanilla crafting recipe.
         * @return The variable itemstack, or an empty itemstack if the recipe does not exist.
         */
        public static ItemStack createRecipeVariable(ServerLevel level, ResourceLocation recipe) {
            try {
                return GameTestHelpersIntegratedCrafting.createVariableForRecipe(level, RecipeType.CRAFTING, recipe);
            } catch (RuntimeException e) {
                org.cyclops.integratedcrafting.IntegratedCrafting.clog(org.apache.logging.log4j.Level.WARN,
                        "Skipping unknown crafting recipe " + recipe + " in benchmark preset generation");
                return ItemStack.EMPTY;
            }
        }

        /*
         * Presets
         */

        /**
         * Generate a grid where every cell holds a crafting table with an idle crafting interface in front of it,
         * each holding a single recipe.
         * Nothing ever requests a craft here, so this isolates the standing cost of crafting interfaces:
         * their part ticks, their recipe registrations, and their presence in the crafting network.
         */
        public static void generateInterfacesIdle(ServerLevel level, BlockPos startPos, int size) {
            generateInterfacesIdle(level, startPos, size, 1);
        }

        /**
         * As {@link #generateInterfacesIdle}, but with every crafting interface completely filled with recipes.
         * Compared to {@link #generateInterfacesIdle}, this scales up the network's recipe index
         * without changing the number of parts.
         */
        public static void generateInterfacesIdleRecipes(ServerLevel level, BlockPos startPos, int size) {
            generateInterfacesIdle(level, startPos, size, RECIPES_PER_INTERFACE);
        }

        private static void generateInterfacesIdle(ServerLevel level, BlockPos startPos, int size, int recipesPerInterface) {
            generateEmptyGrid(level, startPos, size);

            List<BlockPos> cells = getCells(startPos, size);
            for (int i = 0; i < cells.size(); i++) {
                placeCrafterCell(level, cells.get(i), getRecipes(i, recipesPerInterface));
            }

            updateCells(level, cells);
        }

        /**
         * @param offset The offset within {@link #ALL_RECIPES} to start from.
         * @param count The number of recipes to collect.
         * @return The recipes at the given offset.
         */
        private static List<ResourceLocation> getRecipes(int offset, int count) {
            List<ResourceLocation> recipes = Lists.newArrayList();
            for (int i = 0; i < count; i++) {
                recipes.add(ALL_RECIPES.get(Math.floorMod(offset * count + i, ALL_RECIPES.size())));
            }
            return recipes;
        }

        /**
         * Generate a grid where crafting writers continuously request items
         * that are crafted out of planks that are already present in network storage.
         * This isolates the cost of scheduling and executing a stream of single-step crafting jobs.
         */
        public static void generateCraftSimple(ServerLevel level, BlockPos startPos, int size) {
            generateCrafting(level, startPos, size, CHAINS, false, 1, false);
        }

        /**
         * Generate a grid where crafting writers continuously request items whose ingredients
         * are themselves not in storage, and must first be crafted out of logs.
         * Compared to {@link #generateCraftSimple}, every request additionally has to resolve
         * and schedule a dependency graph of crafting jobs.
         */
        public static void generateCraftNested(ServerLevel level, BlockPos startPos, int size) {
            generateCrafting(level, startPos, size, CHAINS_NESTED, true, 1, false);
        }

        /**
         * As {@link #generateCraftSimple}, but with every crafting interface completely filled with recipes.
         * This scales up the network's recipe index, which every crafting job calculation has to search.
         */
        public static void generateCraftRecipeIndex(ServerLevel level, BlockPos startPos, int size) {
            generateCrafting(level, startPos, size, CHAINS, false, RECIPES_PER_INTERFACE, false);
        }

        /**
         * As {@link #generateCraftSimple}, but with writers that request an item that is already in storage.
         * No crafting job is ever scheduled, so this isolates the cost that a crafting writer pays
         * per tick just to determine that there is nothing to do.
         */
        public static void generateCraftSatisfiedIdle(ServerLevel level, BlockPos startPos, int size) {
            generateCrafting(level, startPos, size, CHAINS, false, 1, true);
        }

        /**
         * Generate a grid of repeating four-cell units.
         *
         * Every unit is an independent crafting chain, because the crafting writer aspect refuses to
         * schedule a job for an item that the network is already crafting: if all units requested the
         * same item, only one of them would ever cause load.
         *
         * A non-nested unit is laid out as two storage cells, one crafter cell and one writer cell.
         * A nested unit trades one storage cell for a second crafter cell, which holds the planks recipe
         * that the requested recipe depends on.
         *
         * @param level The level.
         * @param startPos The lowest corner of the grid.
         * @param size The edge length of the grid.
         * @param chains The crafting chains to distribute over the units.
         * @param nested If the ingredients of the requested item must themselves be crafted.
         * @param recipesPerInterface The number of recipes to place in every crafting interface.
         * @param satisfied If the writers should request an item that is already in storage,
         *                  so that no crafting job is ever scheduled.
         */
        private static void generateCrafting(ServerLevel level, BlockPos startPos, int size,
                                             List<CraftingChain> chains, boolean nested,
                                             int recipesPerInterface, boolean satisfied) {
            generateEmptyGrid(level, startPos, size);

            List<BlockPos> cells = getCells(startPos, size);
            for (int i = 0; i < cells.size(); i++) {
                BlockPos cell = cells.get(i);
                int unit = i / 4;
                CraftingChain chain = chains.get(Math.floorMod(unit, chains.size()));
                switch (i % 4) {
                    case 0 -> placeStorageCell(level, cell, nested ? chain.log() : chain.planks());
                    case 1 -> {
                        if (nested) {
                            placeCrafterCell(level, cell, Lists.newArrayList(chain.planksRecipe()));
                        } else {
                            placeStorageCell(level, cell, chain.planks());
                        }
                    }
                    case 2 -> placeCrafterCell(level, cell, getUnitRecipes(chain, unit, recipesPerInterface));
                    case 3 -> placeWriterCell(level, cell,
                            satisfied ? chain.planks() : chain.getResultItem(), !satisfied);
                }
            }

            updateCells(level, cells);
        }

        /**
         * @param chain The crafting chain of the unit.
         * @param unit The index of the unit.
         * @param count The number of recipes to collect.
         * @return The recipes of a unit's crafter, always starting with the recipe of the unit's own chain.
         */
        private static List<ResourceLocation> getUnitRecipes(CraftingChain chain, int unit, int count) {
            List<ResourceLocation> recipes = Lists.newArrayList(chain.resultRecipe());
            for (int i = 1; i < count; i++) {
                recipes.add(ALL_RECIPES.get(Math.floorMod(unit * count + i, ALL_RECIPES.size())));
            }
            return recipes;
        }

        /*
         * Validation
         */

        /**
         * Collect everything that is wrong with the parts of a generated grid.
         *
         * A part that failed to activate, or whose active aspect is in an error state,
         * silently does nothing while still costing tick time,
         * which would turn a benchmark preset into an expensive no-op.
         *
         * @param level The level.
         * @param startPos The lowest corner of the grid.
         * @param size The edge length of the grid.
         * @return A description of every broken part, empty if all parts are healthy.
         */
        public static List<String> getPartProblems(ServerLevel level, BlockPos startPos, int size) {
            List<String> problems = Lists.newArrayList();
            for (BlockPos cell : getCells(startPos, size)) {
                collectPartProblems(level, PartPos.of(level, cell.below(), Direction.UP), problems);
                Direction sideDirection = getSideCableDirection(level, cell);
                if (sideDirection != null) {
                    collectPartProblems(level, PartPos.of(level, cell.relative(sideDirection),
                            sideDirection.getOpposite()), problems);
                }
            }
            return problems;
        }

        private static void collectPartProblems(ServerLevel level, PartPos partPos, List<String> problems) {
            PartHelpers.PartStateHolder<?, ?> partStateHolder = PartHelpers.getPart(partPos);
            if (partStateHolder == null) {
                return;
            }
            if (partStateHolder.getState() instanceof IPartStateWriter<?> writerState) {
                if (writerState.isDeactivated()) {
                    problems.add("Writer part at " + partPos + " is deactivated");
                } else if (writerState.getActiveAspect() == null) {
                    problems.add("Writer part at " + partPos + " has no active aspect");
                } else if (!writerState.getErrors(writerState.getActiveAspect()).isEmpty()) {
                    problems.add("Writer part at " + partPos + " has aspect errors: "
                            + writerState.getErrors(writerState.getActiveAspect()).get(0).getString());
                }
            }
            if (partStateHolder.getState() instanceof PartTypeInterfaceCrafting.State craftingState) {
                for (int slot = 0; slot < craftingState.getInventoryVariables().getContainerSize(); slot++) {
                    if (!craftingState.getInventoryVariables().getItem(slot).isEmpty()
                            && !craftingState.isRecipeSlotValid(slot)) {
                        problems.add("Crafting interface at " + partPos + " has an invalid recipe in slot " + slot
                                + ": " + craftingState.getRecipeSlotUnlocalizedMessage(slot).getString());
                    }
                }
            }
        }

        /*
         * Topology churn
         */

        /**
         * Add a crafting table with a crafting interface at the given cell.
         * This is used to measure the cost of growing a crafting network at runtime.
         * @param level The level.
         * @param cell The cell to add a crafting interface for.
         * @param index The index of the cell, which determines the recipe that the interface receives.
         */
        public static void addCraftingInterfaceCell(ServerLevel level, BlockPos cell, int index) {
            placeCrafterCell(level, cell, getRecipes(index, 1));
            updateCells(level, Lists.newArrayList(cell));
        }

        /**
         * Remove the crafter of the given cell, together with the cable that holds its crafting interface.
         * This is used to measure the cost of shrinking a crafting network at runtime.
         * @param level The level.
         * @param cell The cell to remove.
         */
        public static void removeCell(ServerLevel level, BlockPos cell) {
            // Empty the container before removing it. Even without block drops, a container block
            // spills its contents as item entities when it is removed, and ticking those entities
            // would dominate the measurement instead of the network shrinking itself.
            if (level.getBlockEntity(cell) instanceof Container container) {
                container.clearContent();
            }
            level.destroyBlock(cell, false);
            level.destroyBlock(cell.below(), false);
        }

        /**
         * Remove all blocks that the presets of this command can generate,
         * within the given radius of the given position.
         * @param level The level.
         * @param centerPos The center position.
         * @param radius The radius to clear.
         */
        public static void clearGrid(ServerLevel level, BlockPos centerPos, int radius) {
            BlockCable.SKIP_NETWORK_INIT = true;

            try {
                for (int x = centerPos.getX() - radius; x <= centerPos.getX() + radius; x++) {
                    for (int y = centerPos.getY() - radius; y <= centerPos.getY() + radius; y++) {
                        for (int z = centerPos.getZ() - radius; z <= centerPos.getZ() + radius; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (isGeneratedBlock(level.getBlockState(pos).getBlock())) {
                                if (level.getBlockEntity(pos) instanceof Container container) {
                                    container.clearContent();
                                }
                                level.destroyBlock(pos, false);
                            }
                        }
                    }
                }
            } finally {
                BlockCable.SKIP_NETWORK_INIT = false;
            }
        }

        /**
         * @param block A block.
         * @return If the given block is one that the presets of this command can generate.
         */
        private static boolean isGeneratedBlock(Block block) {
            return block == RegistryEntries.BLOCK_CABLE.value()
                    || block == Blocks.CHEST
                    || block == Blocks.CRAFTING_TABLE;
        }
    }
}
