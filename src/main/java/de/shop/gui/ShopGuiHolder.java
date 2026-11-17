package de.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marks an inventory as a shop GUI, independent of any per-player state map. */
public class ShopGuiHolder implements InventoryHolder {
    private Inventory inventory;

    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
