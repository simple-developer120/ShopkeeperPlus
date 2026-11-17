package de.shop.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;

/**
 * Helpers for "display-only" GUI items (trade previews, buttons, glass panes).
 * These items must NEVER end up in a player's real inventory. They are tagged with a
 * persistent-data marker so they can be recognised and purged wherever they leak to.
 */
public final class GuiItemUtil {

    public static final NamespacedKey GUI_ITEM_KEY = new NamespacedKey("shopkeeperplus", "gui_item");

    /**
     * Lore lines used by older versions (before the marker existed). Lets us also clean
     * display items that already leaked into inventories on a live server.
     */
    private static final Set<String> LEGACY_LORE_LINES = Set.of(
            "Click to select!",
            "✔ Click to buy!",
            "✘ Not enough items!",
            "Not enough items!"
    );

    private GuiItemUtil() {}

    /** Tags the item as display-only and returns it. */
    public static ItemStack mark(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(GUI_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** True if the item is a display-only GUI item (tagged, or recognisable by legacy lore). */
    public static boolean isGuiItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (meta.getPersistentDataContainer().has(GUI_ITEM_KEY, PersistentDataType.BYTE)) return true;

        List<String> lore = meta.getLore();
        if (lore == null) return false;
        for (String line : lore) {
            if (line == null) continue;
            String plain = ChatColor.stripColor(line);
            if (plain != null && LEGACY_LORE_LINES.contains(plain.trim())) return true;
        }
        return false;
    }

    /**
     * Removes every display-only item from the player's inventory (incl. armor/offhand) and cursor.
     * @return how many stacks were removed
     */
    public static int purge(Player player) {
        int removed = 0;
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isGuiItem(contents[i])) {
                inv.setItem(i, null);
                removed++;
            }
        }
        if (isGuiItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            removed++;
        }
        if (removed > 0) player.updateInventory();
        return removed;
    }

    /** Forces the client to re-sync its inventory next tick (fixes client-side "ghost" items). */
    public static void resync(Plugin plugin, Player player, boolean clearCursor) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (clearCursor) player.setItemOnCursor(null);
            player.updateInventory();
        });
    }
}
