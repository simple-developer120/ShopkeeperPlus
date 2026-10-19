package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import de.shop.data.ShopDataManager;
import de.shop.gui.ShopkeeperGUI;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ShopEditorListener implements Listener {

    private final VillagerShop plugin;
    public static final String SHOPKEEPER_METADATA = "shopkeeperplus";
    public static final NamespacedKey SHOPKEEPER_KEY = new NamespacedKey("shopkeeperplus", "shopkeeper");
    public static final String PERM_EDITOR = "shopkeeperplus.editor";

    public ShopEditorListener(VillagerShop plugin) { this.plugin = plugin; }

    public static boolean isShopkeeper(Entity entity) {
        if (entity == null) return false;
        return entity.getPersistentDataContainer().has(SHOPKEEPER_KEY, PersistentDataType.BYTE)
                || entity.hasMetadata(SHOPKEEPER_METADATA);
    }

    public static void markAsShopkeeper(Entity entity, VillagerShop plugin) {
        entity.getPersistentDataContainer().set(SHOPKEEPER_KEY, PersistentDataType.BYTE, (byte) 1);
        entity.setMetadata(SHOPKEEPER_METADATA, new FixedMetadataValue(plugin, true));
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();

        if (!(clicked instanceof Merchant) && !(clicked instanceof ZombieVillager)) return;
        if (!isShopkeeper(clicked)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        event.setCancelled(true);

        ShopData data = plugin.getShopDataManager().getByEntity(clicked.getUniqueId());
        if (data == null) return;

        if (player.isSneaking()) {
            if (!player.hasPermission(PERM_EDITOR)) {
                player.sendMessage("§cYou don't have permission to edit shop!");
                return;
            }
            openEditorGUI(player, clicked);
            return;
        }

        if (!data.isTradesOpen()) {
            player.sendMessage("§cThis shop is currently closed!");
            return;
        }

        ShopkeeperGUI.open(player, data, 0);
    }

    public void openEditorGUI(Player player, Entity entity) {
        plugin.getEditorGUIListener().setEditingEntity(player, entity);

        int currentPage = plugin.getPaginationManager().getPage(player);
        Inventory gui = Bukkit.createInventory(null, 45,
                "§8ShopkeeperPlus Editor §7(Page " + (currentPage + 1) + ")");

        ShopData data = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
        List<MerchantRecipe> recipes = data != null ? data.getRecipes() : List.of();

        int pageStart = currentPage * EditorGUIListener.TRADES_PER_PAGE;
        for (int col = 0; col < EditorGUIListener.TRADES_PER_PAGE; col++) {
            int globalIndex = pageStart + col;
            if (globalIndex >= recipes.size()) break;

            MerchantRecipe recipe = recipes.get(globalIndex);
            if (recipe == null) continue;

            gui.setItem(EditorGUIListener.getResultSlot(col), recipe.getResult());

            List<ItemStack> ingredients = recipe.getIngredients();
            if (ingredients.size() == 1) {
                gui.setItem(EditorGUIListener.getCost1Slot(col), ingredients.get(0));
            } else if (ingredients.size() >= 2) {
                gui.setItem(EditorGUIListener.getCost1Slot(col), ingredients.get(0));
                gui.setItem(EditorGUIListener.getCost2Slot(col), ingredients.get(1));
            }
        }
        
        gui.setItem(EditorGUIListener.SLOT_ARMOR, item(Material.ARMOR_STAND, "§eEdit Equipment", "§7Click to change items"));

        boolean isOpen = data != null && data.isTradesOpen();
        Material bannerMat = isOpen ? Material.GREEN_BANNER : Material.RED_BANNER;
        String bannerName = isOpen ? "§aTrades: OPEN" : "§cTrades: CLOSED";
        
        gui.setItem(EditorGUIListener.SLOT_CLOSE_TRADE, item(bannerMat, bannerName, "§7Click to toggle trade status"));
        gui.setItem(EditorGUIListener.SLOT_TELEPORT, item(Material.ENDER_PEARL, "§aTeleport Shopkeeper"));
        gui.setItem(EditorGUIListener.SLOT_DELETE, item(Material.BONE, "§cDelete Shopkeeper"));

        String currentName = (data != null && data.getCustomName() != null) ? data.getCustomName() : ShopDataManager.DEFAULT_NAME;
        gui.setItem(EditorGUIListener.SLOT_NAME_TAG, item(Material.NAME_TAG, "§bChange Shop Name", "§7Current: " + currentName, "§eClick to rename"));

        int badgeLevel = (data != null) ? data.getVillagerLevel() : 1;
        gui.setItem(EditorGUIListener.SLOT_BADGE, getBadgeColorItem(badgeLevel));

        if (entity instanceof Villager v) {
            gui.setItem(EditorGUIListener.SLOT_PROFESSION, getProfessionItem(v.getProfession()));
            gui.setItem(EditorGUIListener.SLOT_TYPE, getTypeItem(v.getVillagerType()));
            gui.setItem(EditorGUIListener.SLOT_BABY, item(Material.EGG, "§dToggle Baby/Adult", "§7Current: " + (v.isAdult() ? "Adult" : "Baby")));
        } 
        else if (entity instanceof ZombieVillager zv) {
            gui.setItem(EditorGUIListener.SLOT_PROFESSION, getProfessionItem(zv.getVillagerProfession()));
            gui.setItem(EditorGUIListener.SLOT_BABY, item(Material.EGG, "§dToggle Baby/Adult", "§7Current: " + (zv.isBaby() ? "Baby" : "Adult")));
        }

        if (currentPage < EditorGUIListener.MAX_PAGES - 1) gui.setItem(EditorGUIListener.SLOT_NEXT_PAGE, item(Material.ARROW, "§6Next Page"));
        if (currentPage > 0) gui.setItem(EditorGUIListener.SLOT_PREV_PAGE, item(Material.SPECTRAL_ARROW, "§6Previous Page"));

        player.openInventory(gui);
    }

    private ItemStack getBadgeColorItem(int level) {
        Material mat;
        String name;
        switch (level) {
            case 2: mat = Material.IRON_INGOT; name = "§fApprentice (Skin)"; break;
            case 3: mat = Material.GOLD_INGOT; name = "§eJourneyman (Yellow)"; break;
            case 4: mat = Material.EMERALD; name = "§aExpert (Green)"; break;
            case 5: mat = Material.DIAMOND; name = "§bMaster (Blue)"; break;
            default: mat = Material.COBBLESTONE; name = "§7Novice (Gray)"; break;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eChoose villager badge color");
            meta.setLore(Collections.singletonList("§7Current: " + name + " §7(Level " + level + ")"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getProfessionItem(Villager.Profession prof) {
        Material mat = Material.BARRIER;
        Color color = null;
        switch (prof) {
            case NONE: mat = Material.BARRIER; break;
            case FISHERMAN: mat = Material.FISHING_ROD; break;
            case LEATHERWORKER: mat = Material.LEATHER; break;
            case MASON: mat = Material.STONECUTTER; break;
            case FLETCHER: mat = Material.FLETCHING_TABLE; break;
            case WEAPONSMITH: mat = Material.GRINDSTONE; break;
            case TOOLSMITH: mat = Material.SMITHING_TABLE; break;
            case LIBRARIAN: mat = Material.LECTERN; break;
            case SHEPHERD: mat = Material.LOOM; break;
            case FARMER: mat = Material.WHEAT; break;
            case CLERIC: mat = Material.BREWING_STAND; break;
            case NITWIT: mat = Material.LEATHER_CHESTPLATE; color = Color.fromRGB(0x008000); break;
            case CARTOGRAPHER: mat = Material.CARTOGRAPHY_TABLE; break;
            case ARMORER: mat = Material.BLAST_FURNACE; break;
            case BUTCHER: mat = Material.SMOKER; break;
        }
        
        ItemStack item = new ItemStack(mat);
        if (mat == Material.LEATHER_CHESTPLATE && color != null) {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            if (meta != null) { meta.setColor(color); item.setItemMeta(meta); }
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bChange Profession");
            meta.setLore(Collections.singletonList("§7Current: " + prof.name()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getTypeItem(Villager.Type type) {
        Color color;
        switch (type) {
            case PLAINS: color = Color.fromRGB(0xA06540); break;
            case JUNGLE: color = Color.fromRGB(0xFFD200); break;
            case TAIGA: color = Color.fromRGB(0xC1A998); break;
            case DESERT: color = Color.fromRGB(0xFFA500); break;
            case SAVANNA: color = Color.fromRGB(0xFF0000); break;
            case SWAMP: color = Color.fromRGB(0x8932B8); break;
            case SNOW: color = Color.fromRGB(0x169C9C); break;
            default: color = Color.fromRGB(0xA06540); break;
        }

        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            meta.setDisplayName("§aChange Villager Type");
            meta.setLore(Collections.singletonList("§7Current: " + type.name()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }
}