package org.cyclops.integratedcrafting.core.crafting.processoverride;

import com.google.common.collect.Sets;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.ItemStackHelpers;

import java.util.List;

/**
 * A crafting grid itemstack holder.
 * @author rubensworks
 */
public class CraftingGrid extends CraftingContainer {

    public CraftingGrid(IMixedIngredients ingredients, int rows, int columns) {
        super(new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player playerIn) {
                return false;
            }
        }, rows, columns);

        // Input validation
        if (!ingredients.getComponents().equals(Sets.newHashSet(IngredientComponent.ITEMSTACK))) {
            throw new IllegalArgumentException("Can only craft with items, while received: "
                    + ingredients.getComponents());
        }
        List<ItemStack> itemStacks = ingredients.getInstances(IngredientComponent.ITEMSTACK);
        if (itemStacks.size() > rows * columns) {
            throw new IllegalArgumentException("Can only craft in a grid with " + (rows * columns)
                    + " items, while got" + itemStacks.size());
        }

        // Insert items into grid
        int slot = 0;
        for (ItemStack itemStack : itemStacks) {
            setItem(slot++, itemStack);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CraftingGrid)) {
            return false;
        }
        for (int i = 0; i < getContainerSize(); i++) {
            if (!ItemStack.matches(this.getItem(i), ((CraftingGrid) obj).getItem(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 11 + getContainerSize();
        for (int i = 0; i < getContainerSize(); i++) {
            hash = hash << 1;
            hash |= ItemStackHelpers.getItemStackHashCode(getItem(i));
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.getContainerSize(); i++) {
            sb.append(this.getItem(i));
            sb.append(",");
        }
        return sb.toString();
    }
}
