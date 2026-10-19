package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import de.shop.gui.EquipmentGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class EquipmentGUIListener implements Listener {

    private final VillagerShop plugin;
    private final Map<UUID, UUID> editingEntity = new java.util.HashMap<>();

    public EquipmentGUIListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    public void setEditingEntity(Player player, Entity entity) {
        editingEntity.put(player.getUniqueId(), entity.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§8Edit Equipment")) return;

        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        int slot = event.getRawSlot();

        if (clicked != null && !clicked.equals(top)) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        if (slot >= 0 && slot <= 5) {
            return; 
        }

        event.setCancelled(true);
        if (slot == EquipmentGUI.SLOT_CLOSE) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§8Edit Equipment")) return;

        UUID uuid = editingEntity.remove(player.getUniqueId());
        if (uuid == null) return;

        Entity entity = Bukkit.getEntity(uuid);
        if (entity == null || entity.isDead()) return;

        Inventory inv = event.getView().getTopInventory();

        if (entity instanceof org.bukkit.entity.LivingEntity living) {
            org.bukkit.inventory.EntityEquipment eq = living.getEquipment();
            if (eq != null) {
                eq.setHelmet(inv.getItem(EquipmentGUI.SLOT_HELMET));
                eq.setChestplate(inv.getItem(EquipmentGUI.SLOT_CHEST));
                eq.setLeggings(inv.getItem(EquipmentGUI.SLOT_LEGS));
                eq.setBoots(inv.getItem(EquipmentGUI.SLOT_BOOTS));
                eq.setItemInMainHand(inv.getItem(EquipmentGUI.SLOT_MAIN_HAND));
                eq.setItemInOffHand(inv.getItem(EquipmentGUI.SLOT_OFF_HAND));
            }
        }

        ShopData data = plugin.getShopDataManager().getByEntity(uuid);
        if (data != null) {
            data.getEquipment().clear();
            for (int i = 0; i <= 5; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.getType().isAir()) {
                    org.bukkit.inventory.EquipmentSlot eqSlot = switch (i) {
                        case 0 -> org.bukkit.inventory.EquipmentSlot.HEAD;
                        case 1 -> org.bukkit.inventory.EquipmentSlot.CHEST;
                        case 2 -> org.bukkit.inventory.EquipmentSlot.LEGS;
                        case 3 -> org.bukkit.inventory.EquipmentSlot.FEET;
                        case 4 -> org.bukkit.inventory.EquipmentSlot.HAND;
                        case 5 -> org.bukkit.inventory.EquipmentSlot.OFF_HAND;
                        default -> null;
                    };
                    if (eqSlot != null) data.getEquipment().put(eqSlot, item);
                }
            }
            plugin.getShopDataManager().save(data);
        }
    }
}
