package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.integratedcrafting.core.CraftingHelpers;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return dependencies.getOrDefault(craftingJob.getId(), new IntArrayList())
                .stream()
                .map(craftingJobs::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
        return dependents.getOrDefault(craftingJob.getId(), new IntArrayList())
                .stream()
                .map(craftingJobs::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void addCraftingJobId(CraftingJob craftingJob) {
        craftingJobs.put(craftingJob.getId(), craftingJob);
    }

    public void removeCraftingJobId(CraftingJob craftingJob) {
        craftingJobs.remove(craftingJob.getId());
    }

    public void onCraftingJobFinished(CraftingJob craftingJob) {
        this.onCraftingJobFinished(craftingJob, true);
    }

    public void onCraftingJobFinished(CraftingJob craftingJob, boolean validateDependencies) {
        // Check if the crafting job can be finished.
        if (validateDependencies && hasDependencies(craftingJob)) {
            throw new IllegalStateException("A crafting job was finished while it still has unfinished dependencies.");
        }

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
                craftingJobs.get(dependent).getDependencyCraftingJobs().rem(craftingJob.getId());
                if (dependentDependencies.isEmpty()) {
                    dependencies.remove(dependent);
                    if (!dependents.containsKey(dependent)) {
                        craftingJobs.remove(dependent);
                    }
                }
            }
        }

        // Remove invalid dependencies that are not present in craftingJobs
        if (!validateDependencies) {
            IntCollection removedDependencies = dependencies.remove(craftingJob.getId());
            if (removedDependencies != null) {
                IntIterator removedDependenciesIt = removedDependencies.iterator();
                while (removedDependenciesIt.hasNext()) {
                    int dependency = removedDependenciesIt.nextInt();
                    dependents.remove(dependency);
                    onCraftingJobFinished(craftingJobs.get(dependency), false);
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
            this.onCraftingJobFinished(mergee, false);
        }
    }

    public static CompoundTag serialize(CraftingJobDependencyGraph graph) {
        CompoundTag tag = new CompoundTag();

        ListTag craftingJobs = new ListTag();
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            craftingJobs.add(CraftingJob.serialize(craftingJob));
        }
        tag.put("craftingJobs", craftingJobs);

        CompoundTag dependencies = new CompoundTag();
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            IntCollection intCollection = graph.dependencies.get(craftingJob.getId());
            if (intCollection != null) {
                dependencies.put(Integer.toString(craftingJob.getId()), new IntArrayTag(intCollection.toIntArray()));
            }
        }
        tag.put("dependencies", dependencies);

        CompoundTag dependents = new CompoundTag();
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            IntCollection intCollection = graph.dependents.get(craftingJob.getId());
            if (intCollection != null) {
                dependents.put(Integer.toString(craftingJob.getId()), new IntArrayTag(intCollection.toIntArray()));
            }
        }
        tag.put("dependents", dependents);

        return tag;
    }

    public static CraftingJobDependencyGraph deserialize(CompoundTag tag) {
        if (!tag.contains("craftingJobs", Tag.TAG_LIST)) {
            throw new IllegalArgumentException("Could not find a craftingJobs entry in the given tag");
        }
        if (!tag.contains("dependencies", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a dependencies entry in the given tag");
        }
        if (!tag.contains("dependents", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a dependents entry in the given tag");
        }

        Int2ObjectMap<CraftingJob> craftingJobs = new Int2ObjectOpenHashMap<>();
        ListTag craftingJobsTag = tag.getList("craftingJobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < craftingJobsTag.size(); i++) {
            CraftingJob craftingJob = CraftingJob.deserialize(craftingJobsTag.getCompound(i));
            craftingJobs.put(craftingJob.getId(), craftingJob);
        }

        Int2ObjectMap<IntCollection> dependencies = new Int2ObjectOpenHashMap<>();
        CompoundTag dependenciesTag = tag.getCompound("dependencies");
        for (String key : dependenciesTag.getAllKeys()) {
            int id = Integer.parseInt(key);
            int[] value = dependenciesTag.getIntArray(key);
            dependencies.put(id, new IntArrayList(value));
        }

        Int2ObjectMap<IntCollection> dependents = new Int2ObjectOpenHashMap<>();
        CompoundTag dependentsTag = tag.getCompound("dependencies");
        for (String key : dependentsTag.getAllKeys()) {
            int id = Integer.parseInt(key);
            int[] value = dependentsTag.getIntArray(key);
            dependents.put(id, new IntArrayList(value));
        }

        return new CraftingJobDependencyGraph(craftingJobs, dependencies, dependents);
    }

}
