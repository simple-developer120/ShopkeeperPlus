package de.shop.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemDataUtil {

    private ItemDataUtil() {}

    /**
     * بررسی می‌کند که آیا آیتم بازیکن با آیتم موردنیاز ترید
     * از نظر DataComponent تطابق کامل دارد یا خیر.
     */
    public static boolean isExactMatch(ItemStack playerItem, ItemStack requiredItem) {
        if (playerItem == null || requiredItem == null) return false;
        if (playerItem.getType() != requiredItem.getType()) return false;
        if (playerItem.getAmount() < requiredItem.getAmount()) return false;
        return playerItem.isSimilar(requiredItem);
    }

    /**
     * بررسی می‌کند که آیا آیتم دارای CustomModelData خاصی است یا خیر.
     */
    public static boolean hasCustomModelData(ItemStack item, int modelData) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.hasCustomModelData() && meta.getCustomModelData() == modelData;
    }

    /**
     * بررسی می‌کند که آیا آیتم نام سفارشی دارد یا خیر.
     */
    public static boolean hasCustomName(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName();
    }
}