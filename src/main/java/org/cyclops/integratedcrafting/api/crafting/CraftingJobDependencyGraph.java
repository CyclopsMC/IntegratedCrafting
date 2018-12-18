package org.cyclops.integratedcrafting.api.crafting;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Collection;
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
                .collect(Collectors.toList());
    }

    public boolean hasDependencies(CraftingJob craftingJob) {
        return dependencies.containsKey(craftingJob.getId());
    }

    public Collection<CraftingJob> getDependents(CraftingJob craftingJob) {
        return dependents.getOrDefault(craftingJob.getId(), new IntArrayList())
                .stream()
                .map(craftingJobs::get)
                .collect(Collectors.toList());
    }

    public void addCraftingJobId(CraftingJob craftingJob) {
        craftingJobs.put(craftingJob.getId(), craftingJob);
    }

    public void removeCraftingJobId(CraftingJob craftingJob) {
        craftingJobs.remove(craftingJob.getId());
    }

    public void onCraftingJobFinished(CraftingJob craftingJob) {
        // Check if the crafting job can be finished.
        if (dependencies.containsKey(craftingJob.getId())) {
            throw new IllegalStateException("A crafting job was finished while it still has unfinished dependencies.");
        }

        // Remove the job instance reference
        removeCraftingJobId(craftingJob);

        // Remove the dependents
        IntCollection removed = dependents.remove(craftingJob.getId());

        // Remove all backwards dependency links
        if (removed != null) {
            IntIterator removedIt = removed.iterator();
            while (removedIt.hasNext()) {
                int dependent = removedIt.nextInt();
                IntCollection dependentDependencies = dependencies.get(dependent);
                dependentDependencies.rem(craftingJob.getId());
                if (dependentDependencies.isEmpty()) {
                    dependencies.remove(dependent);
                    if (!dependents.containsKey(dependent)) {
                        craftingJobs.remove(dependent);
                    }
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

    public static NBTTagCompound serialize(CraftingJobDependencyGraph graph) {
        NBTTagCompound tag = new NBTTagCompound();

        NBTTagList craftingJobs = new NBTTagList();
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            craftingJobs.appendTag(CraftingJob.serialize(craftingJob));
        }
        tag.setTag("craftingJobs", craftingJobs);

        NBTTagCompound dependencies = new NBTTagCompound();
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            IntCollection intCollection = graph.dependencies.get(craftingJob.getId());
            if (intCollection != null) {
                dependencies.setTag(Integer.toString(craftingJob.getId()), new NBTTagIntArray(intCollection.toIntArray()));
            }
        }
        tag.setTag("dependencies", dependencies);

        NBTTagCompound dependents = new NBTTagCompound();
        for (CraftingJob craftingJob : graph.getCraftingJobs()) {
            IntCollection intCollection = graph.dependents.get(craftingJob.getId());
            if (intCollection != null) {
                dependents.setTag(Integer.toString(craftingJob.getId()), new NBTTagIntArray(intCollection.toIntArray()));
            }
        }
        tag.setTag("dependents", dependents);

        return tag;
    }

    public static CraftingJobDependencyGraph deserialize(NBTTagCompound tag) {
        if (!tag.hasKey("craftingJobs", Constants.NBT.TAG_LIST)) {
            throw new IllegalArgumentException("Could not find a craftingJobs entry in the given tag");
        }
        if (!tag.hasKey("dependencies", Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a dependencies entry in the given tag");
        }
        if (!tag.hasKey("dependents", Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Could not find a dependents entry in the given tag");
        }

        Int2ObjectMap<CraftingJob> craftingJobs = new Int2ObjectOpenHashMap<>();
        NBTTagList craftingJobsTag = tag.getTagList("craftingJobs", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < craftingJobsTag.tagCount(); i++) {
            CraftingJob craftingJob = CraftingJob.deserialize(craftingJobsTag.getCompoundTagAt(i));
            craftingJobs.put(craftingJob.getId(), craftingJob);
        }

        Int2ObjectMap<IntCollection> dependencies = new Int2ObjectOpenHashMap<>();
        NBTTagCompound dependenciesTag = tag.getCompoundTag("dependencies");
        for (String key : dependenciesTag.getKeySet()) {
            int id = Integer.parseInt(key);
            int[] value = dependenciesTag.getIntArray(key);
            dependencies.put(id, new IntArrayList(value));
        }

        Int2ObjectMap<IntCollection> dependents = new Int2ObjectOpenHashMap<>();
        NBTTagCompound dependentsTag = tag.getCompoundTag("dependencies");
        for (String key : dependentsTag.getKeySet()) {
            int id = Integer.parseInt(key);
            int[] value = dependentsTag.getIntArray(key);
            dependents.put(id, new IntArrayList(value));
        }

        return new CraftingJobDependencyGraph(craftingJobs, dependencies, dependents);
    }

}
