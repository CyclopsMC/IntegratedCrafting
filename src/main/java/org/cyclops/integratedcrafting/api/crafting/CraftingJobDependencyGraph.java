package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.integratedcrafting.core.CraftingHelpers;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A CraftingJobDependencyGraph stores dependencies between crafting jobs based on their unique ID.
 * @author rubensworks
 */
public class CraftingJobDependencyGraph {

    private final Int2ObjectMap<CraftingJob> craftingJobs;
    private final Int2ObjectMap<IntCollection> dependencies;
    private final Int2ObjectMap<IntCollection> dependents;

    public CraftingJobDependencyGraph() {
        this(new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>(), new Int2ObjectOpenHashMap<>());
    }

    public CraftingJobDependencyGraph(Int2ObjectMap<CraftingJob> craftingJobs,
                                      Int2ObjectMap<IntCollection> dependencies,
                                      Int2ObjectMap<IntCollection> dependents) {
        this.craftingJobs = craftingJobs;
        this.dependencies = dependencies;
        this.dependents = dependents;
    }

    public Collection<CraftingJob> getCraftingJobs() {
        return craftingJobs.values();
    }

    @Nullable
    public CraftingJob getCraftingJob(int id) {
        return craftingJobs.get(id);
    }

    public Collection<CraftingJob> getDependencies(CraftingJob craftingJob) {
        return getCraftingJobs(dependencies.get(craftingJob.getId()));
    }

    public boolean hasDependencies(CraftingJob craftingJob) {
        return hasDependencies(craftingJob.getId());
    }

    public boolean hasDependencies(int craftingJobId) {
        IntCollection deps = dependencies.get(craftingJobId);
        if (deps != null) {
            IntIterator it = deps.iterator();
            while (it.hasNext()) {
                if (craftingJobs.get(it.next()) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public Collection<CraftingJob> getDependents(CraftingJob craftingJob) {
        return getCraftingJobs(dependents.get(craftingJob.getId()));
    }

    /**
     * Resolve the given crafting job ids into their crafting jobs, skipping the ids that are unknown.
     * @param craftingJobIds Crafting job ids, may be null if no ids are stored.
     * @return A new collection with the resolved crafting jobs.
     */
    protected Collection<CraftingJob> getCraftingJobs(@Nullable IntCollection craftingJobIds) {
        if (craftingJobIds == null || craftingJobIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CraftingJob> resolved = Lists.newArrayListWithCapacity(craftingJobIds.size());
        IntIterator it = craftingJobIds.iterator();
        while (it.hasNext()) {
            CraftingJob craftingJob = this.craftingJobs.get(it.nextInt());
            if (craftingJob != null) {
                resolved.add(craftingJob);
            }
        }
        return resolved;
    }

    public void addCraftingJobId(CraftingJob craftingJob) {
        craftingJobs.put(craftingJob.getId(), craftingJob);
    }

    public void removeCraftingJobId(CraftingJob craftingJob) {
        craftingJobs.remove(craftingJob.getId());
    }

    public void onCraftingJobFinished(CraftingJob craftingJob) {
        this.onCraftingJobFinished(craftingJob, false);
    }

    public void onCraftingJobFinished(CraftingJob craftingJob, boolean finishInvalidDependencies) {
        // Remove the job instance reference
        removeCraftingJobId(craftingJob);

        // Remove the dependents
        IntCollection removed = dependents.remove(craftingJob.getId());
        craftingJob.getDependentCraftingJobs().clear();

        // Remove all backwards dependency links
        if (removed != null) {
            IntIterator removedIt = removed.iterator();
            while (removedIt.hasNext()) {
                int dependent = removedIt.nextInt();
                IntCollection dependentDependencies = dependencies.get(dependent);
                dependentDependencies.rem(craftingJob.getId());
                CraftingJob dependentJob = craftingJobs.get(dependent);
                if (dependentJob != null) {
                    dependentJob.getDependencyCraftingJobs().rem(craftingJob.getId());
                    if (dependentDependencies.isEmpty()) {
                        dependencies.remove(dependent);
                        if (!dependents.containsKey(dependent)) {
                            craftingJobs.remove(dependent);
                        }
                    }
                }
            }
        }

        // Remove invalid dependencies that are not present in craftingJobs
        IntCollection removedDependencies = dependencies.remove(craftingJob.getId());
        if (removedDependencies != null) {
            IntIterator removedDependenciesIt = removedDependencies.iterator();
            while (removedDependenciesIt.hasNext()) {
                int dependency = removedDependenciesIt.nextInt();
                dependents.remove(dependency);
                if (finishInvalidDependencies) {
                    onCraftingJobFinished(craftingJobs.get(dependency), true);
                }
            }
        }
    }

    public void addDependency(CraftingJob craftingJob, CraftingJob dependency) {
        // Store id's of the edge
        addCraftingJobId(dependency);
        addDependency(craftingJob, dependency.getId());
    }

    public void addDependency(CraftingJob craftingJob, int dependency) {
        // Store id's of the edge
        addCraftingJobId(craftingJob);

        // Save dependency link
        IntCollection jobDependencies = dependencies.get(craftingJob.getId());
        if (jobDependencies == null) {
            jobDependencies = new IntArrayList();
            dependencies.put(craftingJob.getId(), jobDependencies);
        }
        jobDependencies.add(dependency);

        // Save reverse link
        IntCollection jobDependents = dependents.get(dependency);
        if (jobDependents == null) {
            jobDependents = new IntArrayList();
            dependents.put(dependency, jobDependents);
        }
        jobDependents.add(craftingJob.getId());
    }

    public void removeDependency(int craftingJob, int dependency) {
        // Remove dependency link
        IntCollection jobDependencies = dependencies.get(craftingJob);
        if (jobDependencies != null) {
            jobDependencies.rem(dependency);
            if (jobDependencies.isEmpty()) {
                dependencies.remove(craftingJob);
                if (!dependents.containsKey(craftingJob)) {
                    craftingJobs.remove(craftingJob);
                }
            }
        }

        // Remove reverse link
        IntCollection jobDependents = dependents.get(dependency);
        if (jobDependents != null) {
            jobDependents.rem(craftingJob);
            if (jobDependents.isEmpty()) {
                dependents.remove(dependency);
                if (!dependencies.containsKey(dependency)) {
                    craftingJobs.remove(dependency);
                }
            }
        }
    }

    public void importDependencies(CraftingJobDependencyGraph craftingJobsGraph) {
        for (CraftingJob craftingJob : craftingJobsGraph.getCraftingJobs()) {
            for (CraftingJob dependency : craftingJobsGraph.getDependencies(craftingJob)) {
                this.addDependency(craftingJob, dependency);
            }
        }
    }

    /**
     * Merge the two crafting jobs by adding the second job's amount into the first job's amount.
     * Furthermore, all dependencies of the second job will be merged into the dependencies of the first job as well.
     * @param target The job that should be merged into.
     * @param mergee The job that should be removed and merged into the target job.
     * @param markMergeeAsFinished If the mergee job should be marked as finished.
     */
    public void mergeCraftingJobs(CraftingJob target, CraftingJob mergee, boolean markMergeeAsFinished) {
        target.setAmount(target.getAmount() + mergee.getAmount());
        target.setAmountTotal(target.getAmountTotal() + mergee.getAmountTotal());
        target.setIngredientsStorage(CraftingHelpers.mergeMixedIngredients(
                target.getIngredientsStorage(), mergee.getIngredientsStorage()));

        // If the existing job had dependencies, batch the dependencies as well
        // First, collect all dependency crafting jobs for the target job
        Map<IRecipeDefinition, CraftingJob> dependencyRecipeJobs = Maps.newHashMap();
        for (Integer dependencyCraftingJobId : target.getDependencyCraftingJobs()) {
            CraftingJob dependencyCraftingJob = this.getCraftingJob(dependencyCraftingJobId);
            dependencyRecipeJobs.put(dependencyCraftingJob.getRecipe(), dependencyCraftingJob);
        }
        // Next, try merging the mergee's jobs into the target dependency jobs
        // If no corresponding target dependency job exists, just add the dependency directly to target as dependency.
        for (Integer dependencyCraftingJobId : mergee.getDependencyCraftingJobs()) {
            CraftingJob dependencyCraftingJob = this.getCraftingJob(dependencyCraftingJobId);
            CraftingJob existingDependencyJob = dependencyRecipeJobs.get(dependencyCraftingJob.getRecipe());
            if (existingDependencyJob != null) {
                mergeCraftingJobs(existingDependencyJob, dependencyCraftingJob, false);
            } else {
                // Update dependency links
                mergee.removeDependency(dependencyCraftingJob);
                target.addDependency(dependencyCraftingJob);
                this.removeDependency(mergee.getId(), dependencyCraftingJobId);
                this.addDependency(target, dependencyCraftingJob);

                // Add to our available jobs
                dependencyRecipeJobs.put(dependencyCraftingJob.getRecipe(), dependencyCraftingJob);
            }
        }

        if (markMergeeAsFinished) {
            // Remove the crafting job from the graph
            this.onCraftingJobFinished(mergee, true);
        }
    }

    public static void serialize(ValueOutput valueOutput, CraftingJobDependencyGraph graph) {
        ValueOutput.ValueOutputList craftingJobs = valueOutput.childrenList("craftingJobs");
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            CraftingJob.serialize(craftingJobs.addChild(), craftingJob);
        }

        ValueOutput.ValueOutputList dependencies = valueOutput.childrenList("dependencies");
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            IntCollection intCollection = graph.dependencies.get(craftingJob.getId());
            if (intCollection != null) {
                ValueOutput dependency = dependencies.addChild();
                dependency.putInt("key", craftingJob.getId());
                dependency.putIntArray("values", intCollection.toIntArray());
            }
        }

        ValueOutput.ValueOutputList dependents = valueOutput.childrenList("dependents");
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            IntCollection intCollection = graph.dependents.get(craftingJob.getId());
            if (intCollection != null) {
                ValueOutput dependent = dependents.addChild();
                dependent.putInt("key", craftingJob.getId());
                dependent.putIntArray("values", intCollection.toIntArray());
            }
        }
    }

    public static CraftingJobDependencyGraph deserialize(ValueInput valueInput) {
        Int2ObjectMap<CraftingJob> craftingJobs = new Int2ObjectOpenHashMap<>();
        for (ValueInput input : valueInput.childrenList("craftingJobs").orElseThrow()) {
            CraftingJob craftingJob = CraftingJob.deserialize(input);
            craftingJobs.put(craftingJob.getId(), craftingJob);
        }

        Int2ObjectMap<IntCollection> dependencies = new Int2ObjectOpenHashMap<>();
        for (ValueInput input : valueInput.childrenList("dependencies").orElseThrow()) {
            int key = input.getInt("key").orElseThrow();
            int[] values = input.getIntArray("values").orElseThrow();
            dependencies.put(key, new IntArrayList(values));
        }

        Int2ObjectMap<IntCollection> dependents = new Int2ObjectOpenHashMap<>();
        for (ValueInput input : valueInput.childrenList("dependents").orElseThrow()) {
            int key = input.getInt("key").orElseThrow();
            int[] values = input.getIntArray("values").orElseThrow();
            dependents.put(key, new IntArrayList(values));
        }

        return new CraftingJobDependencyGraph(craftingJobs, dependencies, dependents);
    }

}
