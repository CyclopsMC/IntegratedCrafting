package org.cyclops.integratedcrafting.api.network;

import com.google.common.collect.Multimap;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integratedcrafting.api.crafting.CraftingJob;
import org.cyclops.integratedcrafting.api.crafting.CraftingJobDependencyGraph;
import org.cyclops.integratedcrafting.api.crafting.ICraftingInterface;
import org.cyclops.integratedcrafting.api.crafting.UnavailableCraftingInterfacesException;
import org.cyclops.integratedcrafting.api.recipe.IRecipeIndex;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;

/**
 * A network capability for crafting.
 * @author rubensworks
 */
public interface ICraftingNetwork {

    /**
     * @return The channels that have at least one active position.
     */
    public int[] getChannels();

    /**
     * Get all crafting interfaces for the given channel.
     * @param channel The crafting channel.
     * @return Crafting interfaces.
     */
    public Set<ICraftingInterface> getCraftingInterfaces(int channel);

    /**
     * Get the recipe to interface mapping for the given channel.
     * @param channel The crafting channel.
     * @return The recipe to interface mapping.
     */
    public Multimap<IRecipeDefinition, ICraftingInterface> getRecipeCraftingInterfaces(int channel);

    /**
     * Get the recipe index on the given channel.
     * @param channel The crafting channel.
     * @return The index.
     */
    public IRecipeIndex getRecipeIndex(int channel);

    /**
     * Add a crafting interface to the network.
     * @param channel The channel of the interface.
     * @param craftingInterface A crafting interface.
     * @return If the crafting interface did not exist before in the network.
     */
    public boolean addCraftingInterface(int channel, ICraftingInterface craftingInterface);

    /**
     * Remove a crafting interface from the network.
     * @param channel The channel of the interface.
     * @param craftingInterface A crafting interface.
     * @return If the crafting interface existed.
     */
    public boolean removeCraftingInterface(int channel, ICraftingInterface craftingInterface);

    /**
     * Add the given recipe that is owned by the given crafting interface.
     *
     * This should only be called for differential changes,
     * otherwise {@link #addCraftingInterface(int, ICraftingInterface)} should be called.
     *
     * @param channel The channel of the interface.
     * @param craftingInterface A crafting interface.
     * @param recipe The recipe to add.
     * @return If the recipe was added.
     */
    public boolean addCraftingInterfaceRecipe(int channel, ICraftingInterface craftingInterface, IRecipeDefinition recipe);

    /**
     * Remove the given recipe that is owned by the given crafting interface.
     *
     * This should only be called for differential changes,
     * otherwise {@link #removeCraftingInterface(int, ICraftingInterface)} should be called.
     *
     * @param channel The channel of the interface.
     * @param craftingInterface A crafting interface.
     * @param recipe The recipe to remove.
     * @return If the recipe was removed.
     */
    public boolean removeCraftingInterfaceRecipe(int channel, ICraftingInterface craftingInterface, IRecipeDefinition recipe);

    /**
     * Add the given crafting job to the list of crafting jobs.
     * @param craftingJob The crafting job.
     * @param allowDistribution If the crafting job is allowed to be split over multiple crafting interfaces.
     * @throws UnavailableCraftingInterfacesException If no crafting interfaces were available.
     */
    public void scheduleCraftingJob(CraftingJob craftingJob, boolean allowDistribution) throws UnavailableCraftingInterfacesException;

    /**
     * Called by crafting interfaces when the crafting job is finished and should be removed from all lists.
     * @param craftingJob The crafting job.
     */
    public void onCraftingJobFinished(CraftingJob craftingJob);

    /**
     * Cancel the given crafting job.
     * This will also cancel all its dependencies.
     *
     * @param channel A channel id.
     * @param craftingJobId The crafting job id.
     * @return If the crafting job existed.
     */
    public boolean cancelCraftingJob(int channel, int craftingJobId);

    /**
     * @param channel A channel id.
     * @return Get all present crafting jobs.
     */
    public Iterator<CraftingJob> getCraftingJobs(int channel);

    /**
     * @param channel A channel id.
     * @param craftingJobId A crafting job id.
     * @return The crafting job with the given id or null.
     */
    @Nullable
    public CraftingJob getCraftingJob(int channel, int craftingJobId);

    /**
     * Get present crafting jobs for the given (expected) output instance.
     * @param channel The channel.
     * @param ingredientComponent The ingredient component of the given output type.
     * @param instance The expected output instance.
     * @param matchCondition The matching condition under which the instance should be matched.
     * @param <T> The instance type.
     * @param <M> The matching condition parameter.
     * @return The applicable crafting jobs, can be empty.
     */
    public <T, M> Iterator<CraftingJob> getCraftingJobs(int channel, IngredientComponent<T, M> ingredientComponent,
                                                        T instance, M matchCondition);

    /**
     * @return An overview of all crafting job dependencies in this network.
     */
    public CraftingJobDependencyGraph getCraftingJobDependencyGraph();

    /**
     * Get the interface in which the given crafting job is being crafted.
     * @param channel The channel.
     * @param craftingJobId A crafting job.
     * @return The owning crafting interface or null.
     */
    @Nullable
    public ICraftingInterface getCraftingJobInterface(int channel, int craftingJobId);

    /**
     * Calculate the number of ticks the given job has been running.
     * @param craftingJob A crafting job.
     * @return The number of ticks this job has been running.
     */
    public long getRunningTicks(CraftingJob craftingJob);

}
