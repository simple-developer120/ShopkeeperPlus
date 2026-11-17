package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.util.GuiItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

/**
 * Safety net: display-only GUI items must never exist in real inventories or in the world.
 * Also hardens the editor / equipment GUIs against the same click tricks.
 */
public class GuiItemGuardListener implements Listener {

    private static final String EDITOR_TITLE = "§8ShopkeeperPlus Editor";
    private static final String EQUIPMENT_TITLE = "§8Edit Equipment";

    private final VillagerShop plugin;

    public GuiItemGuardListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    private void purgeSoon(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            int n = GuiItemUtil.purge(player);
            if (n > 0) {
                plugin.getLogger().warning("Removed " + n + " leaked GUI display item(s) from " + player.getName());
            }
        });
    }

    private static boolean isEditor(String title) { return title != null && title.startsWith(EDITOR_TITLE); }
    private static boolean isEquipment(String title) { return EQUIPMENT_TITLE.equals(title); }

    /** Button / decoration slots of the editor and equipment GUIs (not the editable item slots). */
    private static boolean isProtectedRawSlot(String title, int topSize, int raw) {
        if (raw < 0 || raw >= topSize) return false;
        if (isEditor(title)) return raw > EditorGUIListener.EDITABLE_MAX_SLOT;
        if (isEquipment(title)) return raw >= 6;
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clicked = event.getClickedInventory();

        // A display item sitting in the player's own inventory/cursor: block interaction and remove it.
        if (clicked instanceof PlayerInventory
                && (GuiItemUtil.isGuiItem(event.getCurrentItem()) || GuiItemUtil.isGuiItem(event.getCursor()))) {
            event.setCancelled(true);
            purgeSoon(player);
            return;
        }

        Inventory top = event.getView().getTopInventory();
        String title = event.getView().getTitle();
        if (!isEditor(title) && !isEquipment(title)) return;

        ClickType click = event.getClick();
        InventoryAction action = event.getAction();
        boolean protectedSlot = top.equals(clicked)
                && isProtectedRawSlot(title, top.getSize(), event.getRawSlot());
        boolean clone = click == ClickType.CREATIVE || action == InventoryAction.CLONE_STACK;

        if (click == ClickType.DOUBLE_CLICK || action == InventoryAction.COLLECT_TO_CURSOR
                || (clone && protectedSlot)) {
            event.setCancelled(true);
            GuiItemUtil.resync(plugin, player, clone);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        String title = event.getView().getTitle();
        if (!isEditor(title) && !isEquipment(title)) return;

        for (int raw : event.getRawSlots()) {
            if (isProtectedRawSlot(title, top.getSize(), raw)) {
                event.setCancelled(true);
                GuiItemUtil.resync(plugin, player, false);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) purgeSoon(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> purgeSoon(player), 20L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Item drop = event.getItemDrop();
        if (GuiItemUtil.isGuiItem(drop.getItemStack())) {
            drop.remove();
            purgeSoon(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (GuiItemUtil.isGuiItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }
}
