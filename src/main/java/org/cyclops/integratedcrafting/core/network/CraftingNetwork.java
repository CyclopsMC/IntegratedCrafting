package org.cyclops.integratedcrafting.core.network;

import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.datastructure.MultitransformIterator;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.crafting.UnavailableCraftingInterfacesException;
import org.cyclops.integratedcrafting.api.network.ICraftingNetwork;
import org.cyclops.integratedcrafting.api.recipe.ICraftingJobIndexModifiable;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndexModifiable;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.core.CraftingJobIndexDefault;
import org.cyclops.integratedcrafting.core.RecipeIndexDefault;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A crafting handler network with multiple channels.
 * @author rubensworks
 */
public class CraftingNetwork implements ICraftingNetwork {

    private final Set<ICraftingInterface> allCraftingInterfaces = Sets.newHashSet();
    private final Int2ObjectMap<Set<ICraftingInterface>> craftingInterfaces = new Int2ObjectOpenHashMap<>();

    private final Multimap<IRecipeDefinition, ICraftingInterface> allRecipeCraftingInterfaces = newRecipeCraftingInterfacesMap();
    private final Int2ObjectMap<Multimap<IRecipeDefinition, ICraftingInterface>> recipeCraftingInterfaces = new Int2ObjectOpenHashMap<>();

    private final IRecipeIndexModifiable allRecipesIndex = new RecipeIndexDefault();
    private final Int2ObjectMap<IRecipeIndexModifiable> recipeIndexes = new Int2ObjectOpenHashMap<>();

    private final ICraftingJobIndexModifiable allIndexedCraftingJobs = new CraftingJobIndexDefault();
    private final Int2ObjectMap<ICraftingJobIndexModifiable> indexedCraftingJobs = new Int2ObjectOpenHashMap<>();

    private final Int2ObjectMap<ICraftingInterface> allCraftingJobsToInterface = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Int2ObjectMap<ICraftingInterface>> channeledCraftingJobsToInterface = new Int2ObjectOpenHashMap<>();

    private final CraftingJobDependencyGraph craftingJobDependencyGraph = new CraftingJobDependencyGraph();

    protected static Multimap<IRecipeDefinition, ICraftingInterface> newRecipeCraftingInterfacesMap() {
        return MultimapBuilder.hashKeys().treeSetValues(ICraftingInterface.createComparator()).build();
    }

    @Override
    public int[] getChannels() {
        return craftingInterfaces.keySet().toIntArray();
    }

    @Override
    public Set<ICraftingInterface> getCraftingInterfaces(int channel) {
        if (channel == IPositionedAddonsNetwork.WILDCARD_CHANNEL) {
            return allCraftingInterfaces;
        }
        Set<ICraftingInterface> craftingInterfaces = this.craftingInterfaces.get(channel);
        if (craftingInterfaces == null) {
            craftingInterfaces = Sets.newTreeSet(ICraftingInterface.createComparator());
            this.craftingInterfaces.put(channel, craftingInterfaces);
        }
        return craftingInterfaces;
    }

    @Override
    public Multimap<IRecipeDefinition, ICraftingInterface> getRecipeCraftingInterfaces(int channel) {
        if (channel == IPositionedAddonsNetwork.WILDCARD_CHANNEL) {
            return allRecipeCraftingInterfaces;
        }
        Multimap<IRecipeDefinition, ICraftingInterface> recipeCraftingInterfaces = this.recipeCraftingInterfaces.get(channel);
        if (recipeCraftingInterfaces == null) {
            recipeCraftingInterfaces = newRecipeCraftingInterfacesMap();
            this.recipeCraftingInterfaces.put(channel, recipeCraftingInterfaces);
        }
        return recipeCraftingInterfaces;
    }

    @Override
    public IRecipeIndexModifiable getRecipeIndex(int channel) {
        if (channel == IPositionedAddonsNetwork.WILDCARD_CHANNEL) {
            return allRecipesIndex;
        }
        IRecipeIndexModifiable recipeIndex = this.recipeIndexes.get(channel);
        if (recipeIndex == null) {
            recipeIndex = new RecipeIndexDefault();
            this.recipeIndexes.put(channel, recipeIndex);
        }
        return recipeIndex;
    }

    @Override
    public boolean addCraftingInterface(int channel, ICraftingInterface craftingInterface) {
        // Only process deeper indexes if the interface was not yet present
        if (getCraftingInterfaces(channel).add(craftingInterface)) {
            allCraftingInterfaces.add(craftingInterface);
            IRecipeIndexModifiable recipeIndex = getRecipeIndex(channel);
            Multimap<IRecipeDefinition, ICraftingInterface> recipeCraftingInterfaces = getRecipeCraftingInterfaces(channel);
            for (IRecipeDefinition recipe : craftingInterface.getRecipes()) {
                // Save the recipes in the index
                recipeIndex.addRecipe(recipe);
                allRecipesIndex.addRecipe(recipe);
                // Save a mapping from each of the recipes to this crafting interface
                recipeCraftingInterfaces.put(recipe, craftingInterface);
                allRecipeCraftingInterfaces.put(recipe, craftingInterface);
            }

            // Loop over the crafting jobs owned by the interface
            Iterator<CraftingJob> craftingJobsIt = craftingInterface.getCraftingJobs();
            while (craftingJobsIt.hasNext()) {
                CraftingJob craftingJob = craftingJobsIt.next();

                // Store mapping between interface and job in the network
                addCraftingJob(craftingJob.getChannel(), craftingJob, craftingInterface);

                // Add the crafting job dependencies
                craftingJobDependencyGraph.addCraftingJobId(craftingJob);
                IntListIterator dependencyIt = craftingJob.getDependencyCraftingJobs().iterator();
                while (dependencyIt.hasNext()) {
                    craftingJobDependencyGraph.addDependency(craftingJob, dependencyIt.nextInt());
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean removeCraftingInterface(int channel, ICraftingInterface craftingInterface) {
        // Only process deeper indexes if the interface was present
        if (getCraftingInterfaces(channel).remove(craftingInterface)) {
            allCraftingInterfaces.remove(craftingInterface);
            IRecipeIndexModifiable recipeIndex = getRecipeIndex(channel);
            Multimap<IRecipeDefinition, ICraftingInterface> recipeCraftingInterfaces = getRecipeCraftingInterfaces(channel);
            for (IRecipeDefinition recipe : craftingInterface.getRecipes()) {
                // Remove the mappings from each of the recipes to this crafting interface
                recipeCraftingInterfaces.remove(recipe, craftingInterface);
                allRecipeCraftingInterfaces.remove(recipe, craftingInterface);

                // If the mapping from this recipe to crafting interfaces is empty, remove the recipe from the index
                if (!recipeCraftingInterfaces.containsKey(recipe)) {
                    recipeIndex.removeRecipe(recipe);
                }
                if (!allRecipeCraftingInterfaces.containsKey(recipe)) {
                    allRecipesIndex.removeRecipe(recipe);
                }
            }

            // Try cleaning up the channel
            cleanupChannelIfEmpty(channel);

            // Loop over the crafting jobs owned by the interface
            Iterator<CraftingJob> craftingJobsIt = craftingInterface.getCraftingJobs();
            while (craftingJobsIt.hasNext()) {
                CraftingJob craftingJob = craftingJobsIt.next();

                // Remove the mapping between interface and job in the network
                removeCraftingJob(channel, craftingJob);

                // Remove the crafting job dependencies
                craftingJobDependencyGraph.removeCraftingJobId(craftingJob);
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean addCraftingInterfaceRecipe(int channel, ICraftingInterface craftingInterface, IRecipeDefinition recipe) {
        IRecipeIndexModifiable recipeIndex = getRecipeIndex(channel);
        Multimap<IRecipeDefinition, ICraftingInterface> recipeCraftingInterfaces = getRecipeCraftingInterfaces(channel);

        // Save the recipes in the index
        recipeIndex.addRecipe(recipe);
        allRecipesIndex.addRecipe(recipe);
        // Save a mapping from each of the recipes to this crafting interface
        boolean changed = recipeCraftingInterfaces.put(recipe, craftingInterface);
        allRecipeCraftingInterfaces.put(recipe, craftingInterface);

        return changed;
    }

    @Override
    public boolean removeCraftingInterfaceRecipe(int channel, ICraftingInterface craftingInterface, IRecipeDefinition recipe) {
        IRecipeIndexModifiable recipeIndex = getRecipeIndex(channel);
        Multimap<IRecipeDefinition, ICraftingInterface> recipeCraftingInterfaces = getRecipeCraftingInterfaces(channel);

        // Remove the mappings from each of the recipes to this crafting interface
        boolean changed = recipeCraftingInterfaces.remove(recipe, craftingInterface);
        allRecipeCraftingInterfaces.remove(recipe, craftingInterface);

        // If the mapping from this recipe to crafting interfaces is empty, remove the recipe from the index
        if (!recipeCraftingInterfaces.containsKey(recipe)) {
            recipeIndex.removeRecipe(recipe);
        }
        if (!allRecipeCraftingInterfaces.containsKey(recipe)) {
            allRecipesIndex.removeRecipe(recipe);
        }

        return changed;
    }

    @Override
    public void scheduleCraftingJob(CraftingJob craftingJob, boolean allowDistribution)
            throws UnavailableCraftingInterfacesException {
        Multimap<IRecipeDefinition, ICraftingInterface> recipeInterfaces = getRecipeCraftingInterfaces(craftingJob.getChannel());
        Collection<ICraftingInterface> craftingInterfaces = recipeInterfaces.get(craftingJob.getRecipe())
                .stream()
                .filter(ICraftingInterface::canScheduleCraftingJobs)
                .collect(Collectors.toList());

        if (craftingInterfaces.size() == 0) {
            throw new UnavailableCraftingInterfacesException(craftingJob);
        }

        // If our crafting job amount is larger than 1,
        // and we have multiple crafting interfaces available,
        // split our crafting job so we can distribute
        if (allowDistribution && craftingInterfaces.size() > 1 && craftingJob.getAmount() > 1) {
            Collection<CraftingJob> splitCraftingJobs = CraftingHelpers.splitCraftingJobs(craftingJob,
                    craftingInterfaces.size(), getCraftingJobDependencyGraph(),
                    CraftingHelpers.getGlobalCraftingJobIdentifier());
            for (CraftingJob splitCraftingJob : splitCraftingJobs) {
                scheduleCraftingJob(splitCraftingJob, false);
            }
            return;
        }

        // Find the crafting interface that has the least number of crafting jobs.
        // This will achieve parallelized jobs.
        int bestCraftingInterfaceJobCount = 0;
        ICraftingInterface bestCraftingInterface = null;
        for (ICraftingInterface craftingInterface : craftingInterfaces) {
            int jobCount = craftingInterface.getCraftingJobsCount();
            if (bestCraftingInterface == null || jobCount < bestCraftingInterfaceJobCount) {
                bestCraftingInterfaceJobCount = jobCount;
                bestCraftingInterface = craftingInterface;
            }
        }

        // This should not be null, but let's check to be sure.
        if (bestCraftingInterface != null) {
            // Schedule the job in the interface
            bestCraftingInterface.scheduleCraftingJob(craftingJob);
            addCraftingJob(craftingJob.getChannel(), craftingJob, bestCraftingInterface);

            // Store the starting tick in the job
            craftingJob.setStartTick(getCurrentTick());
        }
    }

    protected long getCurrentTick() {
        return ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD).getGameTime();
    }

    @Override
    public void onCraftingJobFinished(CraftingJob craftingJob) {
        removeCraftingJob(craftingJob.getChannel(), craftingJob);
        getCraftingJobDependencyGraph().onCraftingJobFinished(craftingJob);
    }

    @Override
    public boolean cancelCraftingJob(int channel, int craftingJobId) {
        CraftingJob craftingJob = getCraftingJob(channel, craftingJobId);
        if (craftingJob != null) {
            cancelCraftingJob(craftingJob);
            return true;
        }
        return false;
    }

    protected void cancelCraftingJob(CraftingJob craftingJob) {
        // First cancel all dependencies
        for (CraftingJob dependency : getCraftingJobDependencyGraph().getDependencies(craftingJob)) {
            cancelCraftingJob(dependency);
        }

        // Remove all job references from the interface
        ICraftingInterface craftingInterface = getCraftingJobInterface(craftingJob.getChannel(), craftingJob.getId());
        if (craftingInterface != null) {
            craftingInterface.cancelCraftingJob(craftingJob.getChannel(), craftingJob.getId());
        }

        // Remove all job references from the network
        onCraftingJobFinished(craftingJob);
    }

    @Override
    public Iterator<CraftingJob> getCraftingJobs(int channel) {
        return new MultitransformIterator<>(getCraftingInterfaces(channel).iterator(),
                ICraftingInterface::getCraftingJobs);
    }

    @Nullable
    @Override
    public CraftingJob getCraftingJob(int channel, int craftingJobId) {
        if (channel == IPositionedAddonsNetwork.WILDCARD_CHANNEL) {
            return allIndexedCraftingJobs.getCraftingJob(craftingJobId);
        }

        // Check for the channel directly
        ICraftingJobIndexModifiable index = indexedCraftingJobs.get(channel);
        if (index != null) {
            CraftingJob craftingJob = index.getCraftingJob(craftingJobId);
            if (craftingJob != null) {
                return craftingJob;
            }
        }

        // Check for the case the crafting job was explicitly started on the wildcard channel
        ICraftingJobIndexModifiable wildcardIndex = indexedCraftingJobs.get(IPositionedAddonsNetwork.WILDCARD_CHANNEL);
        if (wildcardIndex != null) {
            return wildcardIndex.getCraftingJob(craftingJobId);
        }

        return null;
    }

    protected void addCraftingJob(int channel, CraftingJob craftingJob, ICraftingInterface craftingInterface) {
        // Prepare crafting job index
        ICraftingJobIndexModifiable craftingJobIndex = indexedCraftingJobs.get(channel);
        if (craftingJobIndex == null) {
            craftingJobIndex = new CraftingJobIndexDefault();
            indexedCraftingJobs.put(channel, craftingJobIndex);
        }

        // Prepare crafting job to interface mapping
        Int2ObjectMap<ICraftingInterface> craftingJobsToInterface = this.channeledCraftingJobsToInterface.get(channel);
        if (craftingJobsToInterface == null) {
            craftingJobsToInterface = new Int2ObjectOpenHashMap<>();
            this.channeledCraftingJobsToInterface.put(channel, craftingJobsToInterface);
        }

        // Insert into crafting job index
        allIndexedCraftingJobs.addCraftingJob(craftingJob);
        craftingJobIndex.addCraftingJob(craftingJob);

        // Insert into crafting job to interface mapping
        allCraftingJobsToInterface.put(craftingJob.getId(), craftingInterface);
        craftingJobsToInterface.put(craftingJob.getId(), craftingInterface);
    }

    protected void removeCraftingJob(int channel, CraftingJob craftingJob) {
        // Prepare crafting job index
        ICraftingJobIndexModifiable craftingJobIndex = indexedCraftingJobs.get(channel);

        // Prepare crafting job to interface mapping
        Int2ObjectMap<ICraftingInterface> craftingJobsToInterface = this.channeledCraftingJobsToInterface.get(channel);

        // Remove from crafting job index
        allIndexedCraftingJobs.removeCraftingJob(craftingJob);
        if (craftingJobIndex != null) {
            craftingJobIndex.removeCraftingJob(craftingJob);
        }

        // Remove from crafting job to interface mapping
        allCraftingJobsToInterface.remove(craftingJob.getId());
        if (craftingJobsToInterface != null) {
            craftingJobsToInterface.remove(craftingJob.getId());
        }
    }

    @Override
    public <T, M> Iterator<CraftingJob> getCraftingJobs(int channel, IngredientComponent<T, M> ingredientComponent,
                                                        T instance, M matchCondition) {
        if (channel == IPositionedAddonsNetwork.WILDCARD_CHANNEL) {
            return allIndexedCraftingJobs.getCraftingJobs(ingredientComponent, instance, matchCondition);
        }

        // Check for the specific channel
        ICraftingJobIndexModifiable craftingJobIndex = indexedCraftingJobs.get(channel);
        Iterator<CraftingJob> channelIterator;
        if (craftingJobIndex != null) {
            channelIterator = craftingJobIndex.getCraftingJobs(ingredientComponent, instance, matchCondition);
        } else {
            channelIterator = Iterators.forArray();
        }

        // Check for the case the crafting job was explicitly started on the wildcard channel
        ICraftingJobIndexModifiable wildcardCraftingJobIndex = indexedCraftingJobs.get(IPositionedAddonsNetwork.WILDCARD_CHANNEL);
        Iterator<CraftingJob> wildcardChannelIterator;
        if (wildcardCraftingJobIndex != null) {
            wildcardChannelIterator = wildcardCraftingJobIndex.getCraftingJobs(ingredientComponent, instance, matchCondition);
        } else {
            wildcardChannelIterator = Iterators.forArray();
        }

        // Concat both iterators
        return Iterators.concat(channelIterator, wildcardChannelIterator);
    }

    @Override
    public CraftingJobDependencyGraph getCraftingJobDependencyGraph() {
        return craftingJobDependencyGraph;
    }

    @Nullable
    @Override
    public ICraftingInterface getCraftingJobInterface(int channel, int craftingJobId) {
        if (channel == IPositionedAddonsNetwork.WILDCARD_CHANNEL) {
            return allCraftingJobsToInterface.get(craftingJobId);
        }

        // Check for the channel directly
        Int2ObjectMap<ICraftingInterface> craftingJobsToInterface = this.channeledCraftingJobsToInterface.get(channel);
        if (craftingJobsToInterface != null) {
            ICraftingInterface craftingInterface = craftingJobsToInterface.get(craftingJobId);
            if (craftingInterface != null) {
                return craftingInterface;
            }
        }

        // In case the crafting job was explicitly started on the wildcard channel
        Int2ObjectMap<ICraftingInterface> craftingJobsToInterfaceWildcard = this.channeledCraftingJobsToInterface
                .get(IPositionedAddonsNetwork.WILDCARD_CHANNEL);
        if (craftingJobsToInterfaceWildcard != null) {
            return craftingJobsToInterfaceWildcard.get(craftingJobId);
        }

        return null;
    }

    @Override
    public long getRunningTicks(CraftingJob craftingJob) {
        return getCurrentTick() - craftingJob.getStartTick();
    }

    protected void cleanupChannelIfEmpty(int channel) {
        Set<ICraftingInterface> craftingInterfaces = this.craftingInterfaces.get(channel);
        if (craftingInterfaces != null && craftingInterfaces.isEmpty()) {
            this.craftingInterfaces.remove(channel);
            this.recipeIndexes.remove(channel);
            this.recipeCraftingInterfaces.remove(channel);
        }
    }
}
