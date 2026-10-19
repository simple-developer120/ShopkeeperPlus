package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import de.shop.gui.ShopkeeperGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShopkeeperGUIListener implements Listener {

    private final VillagerShop plugin;
    private final Set<UUID> skipNextClose = new HashSet<>(); // ✅ جلوگیری از پاک شدن استیت

    public ShopkeeperGUIListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith("§8Shop ")) return;

        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        int slot = event.getRawSlot();

        if (clicked != null && !clicked.equals(top)) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }
        
        // اسلات‌های Cost → قفل بشن که پلیر نتونه آیتم‌ها رو برداره یا جابجا کنه
if (slot == ShopkeeperGUI.SLOT_COST1 || slot == ShopkeeperGUI.SLOT_COST2) {
    event.setCancelled(true); // ✅ این خط اضافه شد
    return;
}

        if (slot == ShopkeeperGUI.SLOT_COST1 || slot == ShopkeeperGUI.SLOT_COST2) {
            return;
        }

        event.setCancelled(true);

        UUID skUuid = ShopkeeperGUI.getViewingShopkeeper(player);
        if (skUuid == null) { player.closeInventory(); return; }
        ShopData data = plugin.getShopDataManager().getByEntity(skUuid);
        if (data == null) { player.closeInventory(); return; }

        if (slot == ShopkeeperGUI.SLOT_CLOSE) {
            ShopkeeperGUI.returnCostToPlayer(player, top);
            player.closeInventory();
            return;
        }

        // ✅ Prev با skipNextClose
        if (slot == ShopkeeperGUI.SLOT_PREV) {
            ShopkeeperGUI.returnCostToPlayer(player, top);
            int page = ShopkeeperGUI.getViewingPage(player);
            skipNextClose.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                ShopkeeperGUI.open(player, data, Math.max(0, page - 1));
            });
            return;
        }

        // ✅ Next با skipNextClose
        if (slot == ShopkeeperGUI.SLOT_NEXT) {
            ShopkeeperGUI.returnCostToPlayer(player, top);
            int page = ShopkeeperGUI.getViewingPage(player);
            skipNextClose.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                ShopkeeperGUI.open(player, data, page + 1);
            });
            return;
        }

        if (slot == ShopkeeperGUI.SLOT_RESULT) {
            ShopkeeperGUI.tryBuy(player, data);
            return;
        }

        if (slot >= ShopkeeperGUI.TRADES_START
                && slot < ShopkeeperGUI.TRADES_START + ShopkeeperGUI.TRADES_PER_PAGE) {
            int tradeIdx = slot - ShopkeeperGUI.TRADES_START;
            ShopkeeperGUI.selectTrade(player, data, tradeIdx);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith("§8Shop ")) return;

        // ✅ اگه در حال تغییر صفحه هستیم، استیت رو پاک نکن
        if (skipNextClose.remove(player.getUniqueId())) {
            return;
        }

        Inventory inv = event.getView().getTopInventory();
        ShopkeeperGUI.returnCostToPlayer(player, inv);
        ShopkeeperGUI.clearViewing(player);
    }
}