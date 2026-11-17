package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import de.shop.gui.ShopkeeperGUI;
import de.shop.util.GuiItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class ShopkeeperGUIListener implements Listener {

    private final VillagerShop plugin;

    public ShopkeeperGUIListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!ShopkeeperGUI.isShopGui(top)) return;

        ClickType click = event.getClick();
        InventoryAction action = event.getAction();

        // Creative middle-click ("clone stack") would copy display items to the cursor.
        if (click == ClickType.CREATIVE || action == InventoryAction.CLONE_STACK) {
            event.setCancelled(true);
            GuiItemUtil.resync(plugin, player, true);
            return;
        }

        // Double-click gathers matching items from BOTH inventories into the cursor.
        if (click == ClickType.DOUBLE_CLICK || action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            GuiItemUtil.resync(plugin, player, false);
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        // Clicks in the player's own inventory: allowed, except shift-click into the GUI.
        if (!clicked.equals(top)) {
            if (event.isShiftClick() || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        // From here on: click inside the shop GUI itself. Nothing may be taken or placed.
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == ShopkeeperGUI.SLOT_COST1 || slot == ShopkeeperGUI.SLOT_COST2) return;

        UUID skUuid = ShopkeeperGUI.getViewingShopkeeper(player);
        ShopData data = skUuid == null ? null : plugin.getShopDataManager().getByEntity(skUuid);
        if (data == null) { player.closeInventory(); return; }

        if (slot == ShopkeeperGUI.SLOT_CLOSE) {
            ShopkeeperGUI.returnCostToPlayer(player, top);
            player.closeInventory();
            return;
        }

        if (slot == ShopkeeperGUI.SLOT_PREV) {
            ShopkeeperGUI.returnCostToPlayer(player, top);
            int page = ShopkeeperGUI.getViewingPage(player);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) ShopkeeperGUI.open(player, data, Math.max(0, page - 1));
            });
            return;
        }

        if (slot == ShopkeeperGUI.SLOT_NEXT) {
            ShopkeeperGUI.returnCostToPlayer(player, top);
            int page = ShopkeeperGUI.getViewingPage(player);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) ShopkeeperGUI.open(player, data, page + 1);
            });
            return;
        }

        if (slot == ShopkeeperGUI.SLOT_RESULT) {
            ShopkeeperGUI.tryBuy(player, data);
            return;
        }

        if (slot >= ShopkeeperGUI.TRADES_START
                && slot < ShopkeeperGUI.TRADES_START + ShopkeeperGUI.TRADES_PER_PAGE) {
            ShopkeeperGUI.selectTrade(player, data, slot - ShopkeeperGUI.TRADES_START);
        }
    }

    /** Dragging the cursor over GUI slots would place/replace items inside the shop GUI. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!ShopkeeperGUI.isShopGui(top)) return;

        int size = top.getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < size) {
                event.setCancelled(true);
                GuiItemUtil.resync(plugin, player, false);
                return;
            }
        }
    }

    /** Creative-mode slot packets are a separate event type and bypass InventoryClickEvent handlers. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (ShopkeeperGUI.isShopGui(top) || GuiItemUtil.isGuiItem(event.getCursor())) {
            event.setCancelled(true);
            GuiItemUtil.resync(plugin, player, true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (!ShopkeeperGUI.isShopGui(inv)) return;

        // Always hand escrowed items back (idempotent), even for a GUI that was already replaced.
        ShopkeeperGUI.returnCostToPlayer(player, inv);
        // Only drop player state if this was the GUI currently tracked for them.
        if (ShopkeeperGUI.isShopInventory(player, inv)) ShopkeeperGUI.clearViewing(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ShopkeeperGUI.releaseOnQuit(event.getPlayer());
    }
}
