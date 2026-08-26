# Performance Benchmarking Setup

This document describes the performance benchmarking infrastructure for Integrated Crafting,
which measures the continuous performance of auto-crafting on Integrated Dynamics networks.
Performance results are tracked in https://github.com/CyclopsMC/cyclops-performance-results

This setup mirrors the one of [Integrated Dynamics](https://github.com/CyclopsMC/IntegratedDynamics/blob/master-1.21-lts/PERFORMANCE_BENCHMARKING.md)
and [Integrated Tunnels](https://github.com/CyclopsMC/IntegratedTunnels/blob/master-1.21-lts/PERFORMANCE_BENCHMARKING.md),
and reuses the measurement infrastructure of Integrated Dynamics.
Where Integrated Dynamics measures the cost of the network graph and variable evaluation,
and Integrated Tunnels measures the cost of moving ingredients in and out of a network,
these benchmarks measure the cost of what Integrated Crafting adds on top of that:
crafting interfaces, crafting job scheduling and execution, and the network's recipe index.

## Overview

The performance benchmarking system consists of three main components:

1. **GitHub Workflow** (`.github/workflows/performance.yml`)
   - Executes on the same triggers as CI (push and pull_request)
   - Runs game tests to measure network performance
   - Uses `benchmark-action/github-action-benchmark` to track performance evolution

2. **Game Tests** (`src/main/java/org/cyclops/integratedcrafting/gametest/GameTestsPerformance.java`)
   - Generates networks with different presets for benchmarking
   - Measures performance metrics for each preset
   - Writes results to `runs/gameTestServer/logs/benchmark_results.txt`

3. **Network Generation Command** (`src/main/java/org/cyclops/integratedcrafting/command/CommandGenerateCrafting.java`)
   - Provides `/integratedcrafting generatecrafting <preset> <size>` for manual testing
   - Supports all presets that the benchmarks use, plus `clear`
   - Can be used in both single-player and multiplayer environments

## Grid Layout

All presets are built on top of the same grid layout, so that their results are comparable:

- Even Y levels are fully filled with logic cables.
- Odd Y levels alternate between logic cables and free **cells**.

Every cell is a free position that is fully surrounded by cables of a single network,
so it can hold a container or a crafter that is targeted by a part on the cable below it,
and by a second part on a cable next to it.
A grid of size 9 (the size used in the benchmarks, which is the largest that fits in the `empty10`
game test template) contains 569 cables and 160 cells.

Every cell takes one of three roles:

| Role | Contents |
| --- | --- |
| Storage | A chest holding crafting inputs, exposed to the network by an Integrated Tunnels item interface on the cable below it |
| Crafter | A crafting table, with a crafting interface on the cable below it holding one or nine recipes |
| Writer | An empty cell, with a crafting writer on the cable below it. The crafting writer aspect only operates on the network, so it does not need a target block |

The active crafting presets repeat a **four-cell unit** over the 160 cells, giving 40 units:

| Preset family | Cell 0 | Cell 1 | Cell 2 | Cell 3 |
| --- | --- | --- | --- | --- |
| Simple / recipe index / satisfied | Storage | Storage | Crafter | Writer |
| Nested | Storage | Crafter (planks recipe) | Crafter (result recipe) | Writer |

### Crafting chains

Every unit drives its own independent **crafting chain**: a wooden item that is crafted purely out of
planks of a single wood species, where those planks are in turn crafted out of a single log.

Independent chains are required, not cosmetic: the crafting writer aspect refuses to schedule a job for
an item that the network is already crafting, so if every writer requested the same item, only one of
them would ever cause load and the benchmark would collapse to a single crafting chain.

The chain pool covers 10 wood species (oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry,
crimson, warped) times the `_stairs`, `_trapdoor`, `_boat`, `_slab`, `_pressure_plate` and `_button`
recipes, for **58 chains** in total (crimson and warped have no boat).

Only recipes that are shapeless, at most 2x2, or exactly 3 wide are usable:
`CraftingGrid` fills a 3x3 grid row by row, so a recipe that is 2 wide and 3 tall (such as doors)
would end up misaligned and would never be found by the crafting table process override.

A separate pool of **40 filler recipes** (`_planks`, `_fence`, `_fence_gate` and `_sign` per species)
is used to fill the remaining recipe slots of crafting interfaces. These are never requested;
their only purpose is to grow the network's recipe index. Together this gives **98 distinct recipes**.

### Numbers that were chosen

| Constant | Value | Why |
| --- | --- | --- |
| `SIZE` | 9 | The largest grid that fits in the 10x10x10 `empty10` game test template |
| `WARMUP_TICKS` | 200 | Lets the JIT warm up and the network settle before measuring |
| `EXECUTION_SECONDS` | 30 | 600 game ticks of measurement. Long enough that run-to-run variance on the network tick time stays within a few percent, while the whole suite still finishes in about a minute locally |
| `TIMEOUT_TICKS` | 1000 | Warmup plus measurement plus headroom |
| `CHURN_CELLS` | 50 | Number of crafting interfaces added or removed, one per tick, by the churn presets |
| `RECIPES_PER_INTERFACE` | 9 | The size of a crafting interface's recipe inventory |
| `STORAGE_STACKS` | 12 (of 27 chest slots) | A crafting interface buffers its results until it can push them into network storage, and stops ticking its jobs entirely while that buffer is non-empty. Completely filled chests therefore jam every crafting interface after a single craft, so every storage chest deliberately keeps 15 slots free |

## Network Presets

### Idle crafting interfaces

These presets never request a craft. They isolate the standing cost of having crafting interfaces
attached to a network: their part ticks, their recipe variable evaluation, and their recipe registrations.

| Preset | Description |
| --- | --- |
| `interfaces_crafting_idle` | Every one of the 160 cells holds a crafting table with a crafting interface holding a single recipe. Recipe index: 98 recipes |
| `interfaces_crafting_idle_recipes` | The same 160 crafting interfaces, but each one completely filled with 9 recipes, for 1440 recipe registrations instead of 160. The set of distinct recipes is the same, so this isolates the per-interface cost of holding many recipes |
| `craft_satisfied_idle` | The `craft_simple` layout, but the 40 crafting writers request an item that is already in network storage. No crafting job is ever scheduled, so this isolates what a crafting writer pays per tick just to determine that there is nothing to do |

### Active crafting

These presets continuously schedule and execute crafting jobs.

| Preset | Description |
| --- | --- |
| `craft_simple` | 80 storage cells holding planks, 40 crafting interfaces and 40 crafting writers. Every writer requests its own item, with "ignore storage" enabled so that it keeps requesting instead of stopping once the item exists. Sustains 4.0 crafts per tick. Recipe index: 40 recipes |
| `craft_nested` | 40 storage cells holding only logs, 80 crafting interfaces (one for the planks recipe, one for the result recipe) and 40 crafting writers. Every requested item needs at least 4 planks per craft, and a planks recipe yields 4 planks at a time, so leftovers can never satisfy a request and every request has to resolve and schedule a dependency graph. Sustains 5.5 crafts per tick across roughly 40 concurrent jobs, of which about 35 to 45 are pending on a dependency at any moment. Recipe index: 38 recipes |
| `craft_recipe_index` | Exactly the same layout and the same crafting work as `craft_simple`, but with every crafting interface filled with 9 recipes instead of 1. This grows the recipe index that every crafting job calculation searches from 40 to 98 recipes while holding the amount of crafting constant |

Note that `craft_nested` drives 28 distinct chains rather than 40, because only 28 of the 58 chains
consume at least 4 planks per craft. The remaining 12 writers request a chain that another writer is
already crafting, and therefore idle at the cost of a `craft_satisfied_idle` writer.
The part counts stay identical to the other presets, which keeps them comparable.

### Topology churn

| Preset | Description |
| --- | --- |
| `interfaces_crafting_append` | Starts from a cable-only grid, and adds one crafting table with a crafting interface per tick after warming up, growing from 1 to 50 interfaces. Measures the cost of registering crafting interfaces and their recipes in the crafting network |
| `interfaces_crafting_remove` | Starts from a fully populated grid of 160 crafting interfaces, and removes one per tick after warming up, shrinking to 110. Measures the cost of unregistering crafting interfaces and splitting networks |

For both churn presets the server tick time is captured immediately after the churn finishes, so it
deliberately includes the block updates and network re-initialisations that the churn causes.
This is why `interfaces_crafting_remove` reports a much higher server tick time than every other preset.

## Performance Metrics

The benchmarking system measures two key metrics:

- **Average Network Tick Time (ms)**: The average time the Integrated Dynamics network subsystem takes
  to process one game tick. This is the sum of the time spent in the network's parts
  (which for these benchmarks are crafting interfaces, crafting writers and item interfaces)
  and the time spent in the ingredient observers of the item channel.
- **Average Server Tick Time (ms)**: The average time the entire Minecraft server takes per game tick.
  This measures the overall server performance impact, including both the network and all other
  server operations.
- **Network Size**: The edge length of the generated grid.

These metrics are tracked separately in the benchmark results to distinguish between network-specific
performance and overall server performance impact.

### A note on the idle presets

Integrated Dynamics' ingredient observers are change-driven, and an idle crafting interface does very
little per tick. The idle presets therefore legitimately report a network tick time close to zero
(around 0.2 to 0.6 ms on a development machine), and `interfaces_crafting_idle_recipes` can even report
*less* than `interfaces_crafting_idle`. That is not a broken preset, but it does mean that these
presets are a weak signal for the benchmark action's relative 250% alert threshold: a small absolute
regression can trip the threshold, and a small absolute regression can also hide below it.
The active crafting presets are the ones to watch for real regressions.

## Game Test Execution

The game tests are automatically executed as part of the GitHub workflow:

```bash
PERFORMANCE_BENCHMARK_ENABLED=true ./gradlew runGameTestServer
```

This command:
1. Starts a game test server
2. Runs `GameTestsPerformance` game tests
3. Generates networks with different presets
4. Measures performance metrics
5. Writes results to `runs/gameTestServer/logs/benchmark_results.txt`

When `PERFORMANCE_BENCHMARK_ENABLED` is not set, all of these game tests succeed immediately without
generating or measuring anything, so that regular `./gradlew runGameTestServer` runs are not slowed down.
On a development machine, the full game test suite takes about 18 seconds without benchmarking and
about 65 seconds with benchmarking enabled.

Every preset additionally asserts, right after the warmup, that none of the parts it generated is
deactivated or has an aspect error, and that every filled recipe slot of every crafting interface is
valid. A part that silently failed to activate would still cost tick time, which would quietly turn a
benchmark preset into an expensive no-op.

## Result Format

Results are written in the following format:
```
preset=craft_simple size=9 avgNetworkTickTime=3.13 avgServerTickTime=2.58
preset=craft_nested size=9 avgNetworkTickTime=5.18 avgServerTickTime=1.91
```

Results are then converted to JSON format for the benchmark action.
Each preset generates two metrics - one for network tick time and one for server tick time:
```json
[
  {
    "name": "NETWORK LOAD: craft_simple_size_9",
    "unit": "tick time (ms)",
    "value": 3.13
  },
  {
    "name": "SERVER LOAD: craft_simple_size_9",
    "unit": "tick time (ms)",
    "value": 2.58
  }
]
```

## Benchmark Tracking

The `benchmark-action/github-action-benchmark` GitHub action automatically:
- Stores benchmark results in the `CyclopsMC/IntegratedCrafting/<branch>/benchmarks` directory
  of the `CyclopsMC/cyclops-performance-results` repository
  (note the extra repository name segment compared to Integrated Dynamics,
  which is needed because all Cyclops mods share branch names)
- Generates historical performance charts
- Alerts when performance degrades beyond 250% of baseline
- Creates comments on commits and PRs when alerts are triggered

## Manual Testing

To manually test crafting performance in a Minecraft world:

1. **Generate a network of idle crafting interfaces**:
   ```
   /integratedcrafting generatecrafting interfacesidle 9
   ```

2. **Generate a network that continuously crafts**:
   ```
   /integratedcrafting generatecrafting craftsimple 9
   ```

3. **Generate a network that continuously crafts with dependencies**:
   ```
   /integratedcrafting generatecrafting craftnested 9
   ```

4. **Measure network performance** (provided by Integrated Dynamics):
   ```
   /integrateddynamics networkdiagnostics measure 10
   ```

5. **Clear generated networks**:
   ```
   /integratedcrafting generatecrafting clear 50
   ```

Note that the generation is `O(size^3)`, and that every cell adds a part to the network,
so sizes much larger than 9 will quickly become very heavy.

## Integration with CI/CD

The performance workflow runs on:
- Every push to any `master*` or `feature*` branch
- Every pull request

Performance degradation is tracked across commits and branches, helping to identify performance
regressions early in the development cycle.

## Adding New Benchmarks

To add new network presets or benchmarks:

1. Add a new preset enum value in `CommandGenerateCrafting.CraftingPreset`
2. Add a corresponding generation method in `CommandGenerateCrafting.CraftingGenerationHelper`,
   and dispatch to it from `CraftingGenerationHelper.generate`
3. Add a new `@GameTest` method in `GameTestsPerformance`, with a unique `batch` name,
   so that the benchmark runs in isolation from the other benchmarks
4. The workflow will automatically execute and track the new benchmark

When adding a preset that is supposed to do work, verify that it actually does, by temporarily
instrumenting the site that performs the work - `CraftingJobHandler.consumeAndInsertCrafting` for
crafting execution, and `CraftingHelpers.calculateAndScheduleCraftingJob` for job scheduling - with a
counter, and sampling it several times across the measurement window. The rate has to be non-zero
*and* linear: a preset that does a burst of work during the warmup and then stalls still produces a
plausible-looking number. Use the idle presets as a control, since they must read exactly zero.
