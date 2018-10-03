package org.cyclops.integratedcrafting.core.crafting.processoverride;

import com.google.common.collect.Sets;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import org.cyclops.commoncapabilities.api.ingredient.IMixedIngredients;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.helper.ItemStackHelpers;

import java.util.List;

/**
 * A crafting grid itemstack holder.
 * @author rubensworks
 */
public class CraftingGrid extends InventoryCrafting {

    public CraftingGrid(IMixedIngredients ingredients, int rows, int columns) {
        super(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
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
            setInventorySlotContents(slot++, itemStack);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CraftingGrid)) {
            return false;
        }
        for (int i = 0; i < getSizeInventory(); i++) {
            if (!ItemStack.areItemStacksEqual(this.getStackInSlot(i), ((CraftingGrid) obj).getStackInSlot(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 11 + getSizeInventory();
        for (int i = 0; i < getSizeInventory(); i++) {
            hash = hash << 1;
            hash |= ItemStackHelpers.getItemStackHashCode(getStackInSlot(i));
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.getSizeInventory(); i++) {
            sb.append(this.getStackInSlot(i));
            sb.append(",");
        }
        return sb.toString();
    }
}
