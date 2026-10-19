package de.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class EquipmentGUI {
    public static final int SIZE = 9;
    public static final int SLOT_HELMET = 0;
    public static final int SLOT_CHEST = 1;
    public static final int SLOT_LEGS = 2;
    public static final int SLOT_BOOTS = 3;
    public static final int SLOT_MAIN_HAND = 4;
    public static final int SLOT_OFF_HAND = 5;
    public static final int SLOT_CLOSE = 8;

    public static void open(Player player, Entity entity) {
        Inventory gui = Bukkit.createInventory(null, SIZE, "§8Edit Equipment");

        for (int i = 0; i < 6; i++) gui.setItem(i, null);

        gui.setItem(6, staticItem(Material.GRAY_STAINED_GLASS_PANE, "§7"));
        gui.setItem(7, staticItem(Material.GRAY_STAINED_GLASS_PANE, "§7"));
        gui.setItem(SLOT_CLOSE, staticItem(Material.BARRIER, "§cClose"));

        if (entity instanceof org.bukkit.entity.LivingEntity living) {
            org.bukkit.inventory.EntityEquipment eq = living.getEquipment();
            if (eq != null) {
                gui.setItem(SLOT_HELMET, eq.getHelmet());
                gui.setItem(SLOT_CHEST, eq.getChestplate());
                gui.setItem(SLOT_LEGS, eq.getLeggings());
                gui.setItem(SLOT_BOOTS, eq.getBoots());
                gui.setItem(SLOT_MAIN_HAND, eq.getItemInMainHand());
                gui.setItem(SLOT_OFF_HAND, eq.getItemInOffHand());
            }
        }

        player.openInventory(gui);
    }

    private static ItemStack staticItem(Material mat, String name) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }
  }
