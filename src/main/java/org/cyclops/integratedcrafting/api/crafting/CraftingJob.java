package org.cyclops.integratedcrafting.api.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.MixedIngredients;
import org.cyclops.cyclopscore.ingredient.collection.IngredientList;
import org.cyclops.cyclopscore.ingredient.storage.IngredientComponentStorageSlottedCollectionWrapper;
import org.cyclops.integratedcrafting.core.CraftingHelpers;
import org.cyclops.integratedcrafting.core.MissingIngredients;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author rubensworks
 */
public class CraftingJob {

    private final int id;
    private final int channel;
    private final IRecipeDefinition recipe;
    private final IntList dependencyCraftingJobs;
    private final IntList dependentCraftingJobs;
    private int amount;
    private int amountTotal;
    private IMixedIngredients ingredientsStorage; // Total to extract from storage (simulated and immutable)
    private IMixedIngredients ingredientsStorageBuffer; // The actual ingredients from storage, which are consumed over time.
    private Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients;
    private long startTick;
    private boolean invalidInputs;
    @Nullable
    private String initiatorUuid;
    private boolean notifyInitiator;
    private boolean ignoreDependencyCheck;
    private boolean cancelled;

    public CraftingJob(int id, int channel, IRecipeDefinition recipe, int amount, IMixedIngredients ingredientsStorage) {
        this.id = id;
        this.channel = channel;
        this.recipe = recipe;
        this.amount = amount;
        this.amountTotal = amount;
        this.ingredientsStorage = ingredientsStorage;
        this.ingredientsStorageBuffer = new MixedIngredients(Maps.newIdentityHashMap());
        this.lastMissingIngredients = Maps.newIdentityHashMap();
        this.dependencyCraftingJobs = new IntArrayList();
        this.dependentCraftingJobs = new IntArrayList();
        this.invalidInputs = false;
        this.ignoreDependencyCheck = false;
    }

    public int getId() {
        return id;
    }

    public int getChannel() {
        return this.channel;
    }

    public IRecipeDefinition getRecipe() {
        return this.recipe;
    }

    public IntList getDependencyCraftingJobs() {
        return dependencyCraftingJobs;
    }

    public IntList getDependentCraftingJobs() {
        return dependentCraftingJobs;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    /**
     * @return The amount this job started with, including the amount that was crafted already.
     *         Contrary to {@link #getAmount()}, this value is not decremented while crafting.
     */
    public int getAmountTotal() {
        return amountTotal;
    }

    public void setAmountTotal(int amountTotal) {
        this.amountTotal = amountTotal;
    }

    public void addDependency(CraftingJob dependency) {
        dependencyCraftingJobs.add(dependency.getId());
        dependency.dependentCraftingJobs.add(this.getId());
    }

    public void removeDependency(CraftingJob dependency) {
        dependencyCraftingJobs.rem(dependency.getId());
        dependency.dependentCraftingJobs.rem(this.getId());
    }

    /**
     * @return The ingredients that will be taken from storage
     *         The amount of this job is already taken into account.
     */
    public IMixedIngredients getIngredientsStorage() {
        return ingredientsStorage;
    }

    public void setIngredientsStorage(IMixedIngredients ingredientsStorage) {
        this.ingredientsStorage = ingredientsStorage;
    }

    public IMixedIngredients getIngredientsStorageBuffer() {
        return ingredientsStorageBuffer;
    }

    public void setIngredientsStorageBuffer(IMixedIngredients ingredientsStorageBuffer) {
        this.ingredientsStorageBuffer = ingredientsStorageBuffer;
    }

    public <T, M> long getMissingIngredientQuantity(IngredientComponent<T, M> ingredientComponent, T instance) {
        long quantityMissing = 0;
        MissingIngredients<?, ?> missingIngredients = this.lastMissingIngredients.get(ingredientComponent);
        if (missingIngredients != null) {
            IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
            for (MissingIngredients.Element<?, ?> element : missingIngredients.getElements()) {
                for (MissingIngredients.PrototypedWithRequested<?, ?> alternative : element.getAlternatives()) {
                    if (matcher.matches(instance, (T) alternative.getRequestedPrototype().getPrototype(), matcher.withoutCondition((M) alternative.getRequestedPrototype().getCondition(), ingredientComponent.getPrimaryQuantifier().getMatchCondition()))) {
                        quantityMissing += alternative.getQuantityMissing();
                    }
                }
            }
        }
        return quantityMissing;
    }

    public <T, M> void addToIngredientsStorageBuffer(IngredientComponent<T, M> ingredientComponent, T instance) {
        // Add instance to the buffer
        IIngredientMatcher<T, M> matcher = ingredientComponent.getMatcher();
        IMixedIngredients buffer = this.getIngredientsStorageBuffer();
        if (!buffer.getComponents().contains(ingredientComponent)) {
            Map<IngredientComponent<?, ?>, List<?>> mixedIngredientsRaw = Maps.newIdentityHashMap();
            for (IngredientComponent<?, ?> component : buffer.getComponents()) {
                mixedIngredientsRaw.put(component, buffer.getInstances(component));
            }
            List<T> list = Lists.newArrayList();
            mixedIngredientsRaw.put(ingredientComponent, list);
            list.add(instance);
            buffer = new MixedIngredients(mixedIngredientsRaw);
            this.setIngredientsStorageBuffer(buffer);
        } else {
            List<T> instances = buffer.getInstances(ingredientComponent);
            if (!instances.stream().anyMatch(matcher::isEmpty)) {
                // Make sure we have at least one empty slot available, to guarantee insertion can succeed.
                instances.add(matcher.getEmptyInstance());
            }
            T remaining = new IngredientComponentStorageSlottedCollectionWrapper<>(new IngredientList<>(ingredientComponent, instances), Integer.MAX_VALUE, Integer.MAX_VALUE).insert(instance, false);
            if (!matcher.isEmpty(remaining)) {
                throw new IllegalStateException(String.format("Unable to insert %s into the crafting job buffer, remaining: %s", instances, remaining));
            }
        }

        // If the instance was a missing ingredient, remove it
        long instanceQuantity = matcher.getQuantity(instance);
        MissingIngredients<?, ?> missingIngredients = this.lastMissingIngredients.get(ingredientComponent);
        if (missingIngredients != null) {
            Iterator<? extends MissingIngredients.Element<?, ?>> it = missingIngredients.getElements().iterator();
            boolean removed = false;
            while (it.hasNext() && instanceQuantity > 0) {
                MissingIngredients.Element<?, ?> element = it.next();
                for (MissingIngredients.PrototypedWithRequested<?, ?> alternative : element.getAlternatives()) {
                    if (matcher.matches(instance, (T) alternative.getRequestedPrototype().getPrototype(), matcher.withoutCondition((M) alternative.getRequestedPrototype().getCondition(), ingredientComponent.getPrimaryQuantifier().getMatchCondition()))) {
                        long missingQuantityToConsume = Math.min(alternative.getQuantityMissing(), instanceQuantity);
                        alternative.setQuantityMissing(alternative.getQuantityMissing() - missingQuantityToConsume);
                        instanceQuantity -= missingQuantityToConsume;
                        if (alternative.getQuantityMissing() == 0) {
                            removed = true;
                            it.remove();
                        }
                        break;
                    }
                }
            }
            if (removed) {
                if (missingIngredients.getElements().isEmpty()) {
                    this.lastMissingIngredients.remove(ingredientComponent);
                }
            }
        }
    }

    /**
     * @return The ingredients that were missing for 1 job amount. This will mostly be an empty map.
     */
    public Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> getLastMissingIngredients() {
        return lastMissingIngredients;
    }

    public void setLastMissingIngredients(Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients) {
        this.lastMissingIngredients = lastMissingIngredients;
    }

    public long getStartTick() {
        return startTick;
    }

    public void setStartTick(long startTick) {
        this.startTick = startTick;
    }

    public boolean isInvalidInputs() {
        return invalidInputs;
    }

    public void setInvalidInputs(boolean invalidInputs) {
        this.invalidInputs = invalidInputs;
    }

    @Nullable
    public String getInitiatorUuid() {
        return initiatorUuid;
    }

    public void setInitiatorUuid(String initiatorUuid) {
        this.initiatorUuid = initiatorUuid;
    }

    /**
     * @return If the initiator wants to be notified when this job is completed.
     */
    public boolean isNotifyInitiator() {
        return notifyInitiator;
    }

    public void setNotifyInitiator(boolean notifyInitiator) {
        this.notifyInitiator = notifyInitiator;
    }

    /**
     * @return If this job was cancelled instead of running to completion.
     *         This is not persisted, as cancelled jobs are removed from their network right away.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setIgnoreDependencyCheck(boolean ignoreDependencyCheck) {
        this.ignoreDependencyCheck = ignoreDependencyCheck;
    }

    public boolean isIgnoreDependencyCheck() {
        return ignoreDependencyCheck;
    }

    public static void serialize(ValueOutput valueOutput, CraftingJob craftingJob) {
        valueOutput.putInt("id", craftingJob.id);
        valueOutput.putInt("channel", craftingJob.channel);
        IRecipeDefinition.serialize(valueOutput.child("recipe"), craftingJob.recipe);
        valueOutput.putIntArray("dependencies", craftingJob.getDependencyCraftingJobs().toIntArray());
        valueOutput.putIntArray("dependents", craftingJob.getDependentCraftingJobs().toIntArray());
        valueOutput.putInt("amount", craftingJob.amount);
        valueOutput.putInt("amountTotal", craftingJob.amountTotal);
        IMixedIngredients.serialize(valueOutput.child("ingredientsStorage"), craftingJob.ingredientsStorage);
        IMixedIngredients.serialize(valueOutput.child("ingredientsStorageBuffer"), craftingJob.ingredientsStorageBuffer);
        MissingIngredients.serialize(valueOutput.child("lastMissingIngredients"), craftingJob.lastMissingIngredients);
        valueOutput.putLong("startTick", craftingJob.startTick);
        valueOutput.putBoolean("invalidInputs", craftingJob.invalidInputs);
        if (craftingJob.initiatorUuid != null) {
            valueOutput.putString("initiatorUuid", craftingJob.initiatorUuid);
        }
        valueOutput.putBoolean("notifyInitiator", craftingJob.notifyInitiator);
        valueOutput.putBoolean("ignoreDependencyCheck", craftingJob.ignoreDependencyCheck);
    }

    public static CraftingJob deserialize(ValueInput valueInput) {
        int id = valueInput.getInt("id").orElseThrow();
        int channel = valueInput.getInt("channel").orElseThrow();
        IRecipeDefinition recipe = IRecipeDefinition.deserialize(valueInput.child("recipe").orElseThrow());
        int amount = valueInput.getInt("amount").orElseThrow();
        IMixedIngredients ingredientsStorage = IMixedIngredients.deserialize(valueInput.child("ingredientsStorage").orElseThrow());
        IMixedIngredients ingredientsStorageBuffer = valueInput.child("ingredientsStorageBuffer").map(IMixedIngredients::deserialize).orElseGet(() -> new MixedIngredients(Maps.newIdentityHashMap())); // TODO: rm backwards-compat in nextmajor (use orElseThrow)
        CraftingJob craftingJob = new CraftingJob(id, channel, recipe, amount, ingredientsStorage);
        for (int dependency : valueInput.getIntArray("dependencies").orElseThrow()) {
            craftingJob.dependencyCraftingJobs.add(dependency);
        }
        for (int dependent : valueInput.getIntArray("dependents").orElseThrow()) {
            craftingJob.dependentCraftingJobs.add(dependent);
        }
        Map<IngredientComponent<?, ?>, MissingIngredients<?, ?>> lastMissingIngredients = MissingIngredients
                .deserialize(valueInput.child("lastMissingIngredients").orElseThrow());
        craftingJob.setLastMissingIngredients(lastMissingIngredients);
        craftingJob.setAmountTotal(valueInput.getIntOr("amountTotal", amount)); // TODO: rm backwards-compat in next major
        craftingJob.setStartTick(valueInput.getLong("startTick").orElseThrow());
        craftingJob.setInvalidInputs(valueInput.getBooleanOr("invalidInputs", false));
        valueInput.getString("initiatorUuid").ifPresent(craftingJob::setInitiatorUuid);
        craftingJob.setNotifyInitiator(valueInput.getBooleanOr("notifyInitiator", false));
        craftingJob.setIgnoreDependencyCheck(valueInput.getBooleanOr("ignoreDependencyCheck", false));
        craftingJob.setIngredientsStorageBuffer(ingredientsStorageBuffer);
        return craftingJob;
    }

    @Override
    public String toString() {
        return String.format("[Crafting Job id: %s, channel: %s, recipe: %s, dependencies: %s, dependents: %s, amount: %s, storage: %s]",
                getId(), getChannel(), getRecipe(), getDependencyCraftingJobs(), getDependentCraftingJobs(), getAmount(), getIngredientsStorage());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CraftingJob)) {
            return false;
        }
        CraftingJob that = (CraftingJob) obj;
        return this.getId() == that.getId()
                && this.getChannel() == that.getChannel()
                && Objects.equals(this.getRecipe(), that.getRecipe())
                && this.getDependencyCraftingJobs().equals(that.getDependencyCraftingJobs())
                && this.getDependentCraftingJobs().equals(that.getDependentCraftingJobs())
                && this.getAmount() == that.getAmount()
                && this.getIngredientsStorage().equals(that.getIngredientsStorage());
    }

    public CraftingJob clone(CraftingHelpers.IIdentifierGenerator identifierGenerator) {
        if (!this.getIngredientsStorageBuffer().isEmpty()) {
            throw new IllegalStateException("Cloning a job with an ingredient buffer is illegal");
        }
        CraftingJob clone = new CraftingJob(
                identifierGenerator.getNext(),
                getChannel(),
                getRecipe(),
                getAmount(),
                getIngredientsStorage()
        );
        clone.setAmountTotal(getAmountTotal());
        clone.setInitiatorUuid(getInitiatorUuid());
        clone.setNotifyInitiator(isNotifyInitiator());
        return clone;
    }
}
