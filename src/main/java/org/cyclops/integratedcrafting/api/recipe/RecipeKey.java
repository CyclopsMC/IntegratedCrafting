package org.cyclops.integratedcrafting.api.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.cyclops.commoncapabilities.api.capability.recipehandler.IRecipeDefinition;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An opaque and persistable identifier of a recipe.
 *
 * Recipes that originate from a built-in recipe are identified by their recipe id,
 * which keeps this key small, even for machines that expose thousands of recipes.
 * All other recipes are identified by their full structural serialization.
 *
 * Keys are deliberately never resolved back into recipes,
 * as {@link org.cyclops.commoncapabilities.api.capability.recipehandler.RecipeDefinition#fromRecipeId(HolderLookup.Provider, ResourceLocation)}
 * throws once the recipe no longer exists.
 * Keeping unknown keys opaque makes sure that a pack update can not silently
 * change the meaning of a key that was stored before.
 *
 * @author rubensworks
 */
public final class RecipeKey {

    /**
     * The NBT key under which {@link IRecipeDefinition#serialize(HolderLookup.Provider, IRecipeDefinition)}
     * stores the id of built-in recipes.
     */
    private static final String NBT_RECIPE_ID = "recipeId";

    @Nullable
    private final String recipeId;
    @Nullable
    private final CompoundTag structure;

    private RecipeKey(@Nullable String recipeId, @Nullable CompoundTag structure) {
        this.recipeId = recipeId;
        this.structure = structure;
    }

    /**
     * Create a key for the given built-in recipe id.
     * @param recipeId A recipe id.
     * @return A key.
     */
    public static RecipeKey ofRecipeId(String recipeId) {
        return new RecipeKey(Objects.requireNonNull(recipeId), null);
    }

    /**
     * Create a key for the given recipe.
     * @param lookupProvider A lookup provider,
     *                       only used for recipes that are not backed by a built-in recipe.
     * @param recipe A recipe.
     * @return A key.
     */
    public static RecipeKey of(HolderLookup.Provider lookupProvider, IRecipeDefinition recipe) {
        ResourceLocation recipeId = recipe.getRecipeId();
        if (recipeId != null) {
            return new RecipeKey(recipeId.toString(), null);
        }
        return new RecipeKey(null, IRecipeDefinition.serialize(lookupProvider, recipe));
    }

    /**
     * Read a key from NBT.
     * @param tag An NBT tag, as produced by {@link #serialize()}.
     * @return A key.
     */
    public static RecipeKey deserialize(CompoundTag tag) {
        if (tag.contains(NBT_RECIPE_ID, Tag.TAG_STRING)) {
            return new RecipeKey(tag.getString(NBT_RECIPE_ID), null);
        }
        return new RecipeKey(null, tag.copy());
    }

    /**
     * @return An NBT representation of this key.
     */
    public CompoundTag serialize() {
        if (this.recipeId != null) {
            CompoundTag tag = new CompoundTag();
            tag.putString(NBT_RECIPE_ID, this.recipeId);
            return tag;
        }
        return this.structure.copy();
    }

    /**
     * @return The id of the built-in recipe this key refers to, or null if this key is structural.
     */
    @Nullable
    public String getRecipeId() {
        return this.recipeId;
    }

    /**
     * @return If this key identifies a recipe by its full structure instead of by a recipe id.
     */
    public boolean isStructural() {
        return this.recipeId == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RecipeKey that)) {
            return false;
        }
        return Objects.equals(this.recipeId, that.recipeId) && Objects.equals(this.structure, that.structure);
    }

    @Override
    public int hashCode() {
        return this.recipeId != null ? this.recipeId.hashCode() : this.structure.hashCode();
    }

    @Override
    public String toString() {
        return "[RecipeKey " + (this.recipeId != null ? this.recipeId : this.structure) + "]";
    }

}
