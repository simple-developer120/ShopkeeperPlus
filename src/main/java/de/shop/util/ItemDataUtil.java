package de.shop.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemDataUtil {

    private ItemDataUtil() {}

    public static boolean isExactMatch(ItemStack playerItem, ItemStack requiredItem) {
        if (playerItem == null || requiredItem == null) return false;
        if (playerItem.getType() != requiredItem.getType()) return false;
        if (playerItem.getAmount() < requiredItem.getAmount()) return false;
        return playerItem.isSimilar(requiredItem);
    }
    
    public static boolean hasCustomModelData(ItemStack item, int modelData) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == modelData;
    }
    
    public static boolean hasCustomName(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName();
    }
}
