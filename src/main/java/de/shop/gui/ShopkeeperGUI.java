package de.shop.gui;

import de.shop.data.ShopData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopkeeperGUI {

    public static final int SIZE = 27;

    public static final int SLOT_COST1 = 0;
    public static final int SLOT_COST2 = 1;
    public static final int SLOT_ARROW = 2;
    public static final int SLOT_RESULT = 3;
    public static final int SLOT_PREV = 6;
    public static final int SLOT_CLOSE = 7;
    public static final int SLOT_NEXT = 8;

    public static final int TRADES_START = 9;
    public static final int TRADES_PER_PAGE = 18;

    private static final Map<UUID, UUID> viewingShopkeeper = new HashMap<>();
    private static final Map<UUID, Integer> viewingPage = new HashMap<>();
    private static final Map<UUID, Integer> selectedTrade = new HashMap<>();

    public static void open(Player player, ShopData data, int page) {
        if (page < 0) page = 0;
        List<MerchantRecipe> recipes = data.getRecipes();
        int totalPages = Math.max(1, (int) Math.ceil((double) recipes.size() / TRADES_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;

        viewingShopkeeper.put(player.getUniqueId(), data.getEntityUuid());
        viewingPage.put(player.getUniqueId(), page);
        selectedTrade.put(player.getUniqueId(), -1);

        String displayName = data.getCustomName() != null && !data.getCustomName().isEmpty() 
                ? data.getCustomName() 
                : "§6Shopkeeper";

        Inventory gui = Bukkit.createInventory(null, SIZE,
                displayName + " §7(Page " + (page + 1) + "/" + totalPages + ")");

        ItemStack pane = staticItem(Material.GRAY_STAINED_GLASS_PANE, "§7");
        for (int i = 0; i < SIZE; i++) gui.setItem(i, pane.clone());

        gui.setItem(SLOT_COST1, null);
        gui.setItem(SLOT_COST2, null);
        gui.setItem(SLOT_ARROW, staticItem(Material.ARROW, "§7→"));
        gui.setItem(SLOT_RESULT, staticItem(Material.BARRIER, "§7Select a trade first"));

        if (page > 0) gui.setItem(SLOT_PREV, staticItem(Material.SPECTRAL_ARROW, "§6« Previous"));
        gui.setItem(SLOT_CLOSE, staticItem(Material.BARRIER, "§cClose"));
        if (page < totalPages - 1) gui.setItem(SLOT_NEXT, staticItem(Material.ARROW, "§6Next »"));

        int start = page * TRADES_PER_PAGE;
        for (int i = 0; i < TRADES_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= recipes.size()) break;
            MerchantRecipe recipe = recipes.get(idx);
            if (recipe == null) continue;
            gui.setItem(TRADES_START + i, buildTradeDisplay(recipe, player));
        }

        player.openInventory(gui);
    }

    public static void refresh(Player player, ShopData data) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top == null) return;

        int page = getViewingPage(player);
        List<MerchantRecipe> recipes = data.getRecipes();
        int start = page * TRADES_PER_PAGE;

        for (int i = 0; i < TRADES_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= recipes.size()) break;
            MerchantRecipe recipe = recipes.get(idx);
            if (recipe == null) continue;
            top.setItem(TRADES_START + i, buildTradeDisplay(recipe, player));
        }

        int sel = getSelectedTrade(player);
        if (sel >= 0) {
            int globalIndex = page * TRADES_PER_PAGE + sel;
            if (globalIndex < recipes.size()) {
                MerchantRecipe recipe = recipes.get(globalIndex);
                if (recipe != null) {
                    ItemStack resultDisplay = recipe.getResult().clone();
                    ItemMeta rMeta = resultDisplay.getItemMeta();
                    if (rMeta != null) {
                        List<String> lore = new ArrayList<>();
                        if (rMeta.hasLore()) lore.addAll(rMeta.getLore());
                        lore.add("");
                        boolean allAvailable = true;
                        for (ItemStack ing : recipe.getIngredients()) {
                            if (countSimilar(player, ing) < ing.getAmount()) {
                                allAvailable = false;
                                break;
                            }
                        }
                        lore.add(allAvailable ? "§a✔ Click to buy!" : "§c✘ Not enough items!");
                        rMeta.setLore(lore);
                        resultDisplay.setItemMeta(rMeta);
                    }
                    top.setItem(SLOT_RESULT, resultDisplay);
                }
            }
        }
    }

    public static UUID getViewingShopkeeper(Player player) { return viewingShopkeeper.get(player.getUniqueId()); }
    public static int getViewingPage(Player player) { return viewingPage.getOrDefault(player.getUniqueId(), 0); }
    public static int getSelectedTrade(Player player) { return selectedTrade.getOrDefault(player.getUniqueId(), -1); }
    public static void clearViewing(Player player) {
        viewingShopkeeper.remove(player.getUniqueId());
        viewingPage.remove(player.getUniqueId());
        selectedTrade.remove(player.getUniqueId());
    }

    public static void selectTrade(Player player, ShopData data, int tradeIndex) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (gui == null) return;

        returnCostToPlayer(player, gui);

        int page = getViewingPage(player);
        int globalIndex = page * TRADES_PER_PAGE + tradeIndex;
        List<MerchantRecipe> recipes = data.getRecipes();
        if (globalIndex >= recipes.size()) return;

        MerchantRecipe recipe = recipes.get(globalIndex);
        if (recipe == null) return;

        selectedTrade.put(player.getUniqueId(), tradeIndex);

        List<ItemStack> ingredients = recipe.getIngredients();
        int[] slots = {SLOT_COST1, SLOT_COST2};

        boolean allAvailable = true;
        for (ItemStack ing : ingredients) {
            if (countSimilar(player, ing) < ing.getAmount()) {
                allAvailable = false;
                break;
            }
        }

        if (allAvailable) {
            for (int i = 0; i < ingredients.size() && i < slots.length; i++) {
                ItemStack ing = ingredients.get(i);
                removeItems(player, ing, ing.getAmount());
                gui.setItem(slots[i], ing.clone());
            }
        } else {
            for (int i = 0; i < ingredients.size() && i < slots.length; i++) {
                ItemStack ing = ingredients.get(i);
                ItemStack display = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c" + pretty(ing));
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Required: §f" + ing.getAmount());
                    lore.add("§cYou don't have enough!");
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                gui.setItem(slots[i], display);
            }
        }

        ItemStack resultDisplay = recipe.getResult().clone();
        ItemMeta rMeta = resultDisplay.getItemMeta();
        if (rMeta != null) {
            List<String> lore = new ArrayList<>();
            if (rMeta.hasLore()) lore.addAll(rMeta.getLore());
            lore.add("");
            lore.add(allAvailable ? "§a✔ Click to buy!" : "§c✘ Not enough items!");
            rMeta.setLore(lore);
            resultDisplay.setItemMeta(rMeta);
        }
        gui.setItem(SLOT_RESULT, resultDisplay);

        player.updateInventory();
    }

    public static boolean tryBuy(Player player, ShopData data) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (gui == null) return false;

        int sel = getSelectedTrade(player);
        if (sel < 0) return false;

        int page = getViewingPage(player);
        int globalIndex = page * TRADES_PER_PAGE + sel;
        List<MerchantRecipe> recipes = data.getRecipes();
        if (globalIndex >= recipes.size()) return false;

        MerchantRecipe recipe = recipes.get(globalIndex);
        List<ItemStack> ingredients = recipe.getIngredients();

        ItemStack cost1 = gui.getItem(SLOT_COST1);
        ItemStack cost2 = gui.getItem(SLOT_COST2);

        if (!matchesIngredients(ingredients, cost1, cost2)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }

        gui.setItem(SLOT_COST1, null);
        gui.setItem(SLOT_COST2, null);

        ItemStack result = recipe.getResult().clone();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);

        boolean canRefill = true;
        for (ItemStack ing : ingredients) {
            if (countSimilar(player, ing) < ing.getAmount()) {
                canRefill = false;
                break;
            }
        }

        if (canRefill) {
            int[] slots = {SLOT_COST1, SLOT_COST2};
            for (int i = 0; i < ingredients.size() && i < slots.length; i++) {
                ItemStack ing = ingredients.get(i);
                removeItems(player, ing, ing.getAmount());
                gui.setItem(slots[i], ing.clone());
            }

            ItemStack resultDisplay = recipe.getResult().clone();
            ItemMeta rMeta = resultDisplay.getItemMeta();
            if (rMeta != null) {
                List<String> lore = new ArrayList<>();
                if (rMeta.hasLore()) lore.addAll(rMeta.getLore());
                lore.add("");
                lore.add("§a✔ Click to buy!");
                rMeta.setLore(lore);
                resultDisplay.setItemMeta(rMeta);
            }
            gui.setItem(SLOT_RESULT, resultDisplay);
        } else {
            selectedTrade.put(player.getUniqueId(), -1);
            gui.setItem(SLOT_RESULT, staticItem(Material.BARRIER, "§7No more items to trade", "§cNot enough in inventory"));
        }

        player.updateInventory();
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("ShopKeeperPlus"), player::updateInventory);
        return true;
    }

    public static void returnCostToPlayer(Player player, Inventory gui) {
        if (gui == null) return;

        for (int slot : new int[]{SLOT_COST1, SLOT_COST2}) {
            ItemStack item = gui.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    gui.setItem(slot, null);
                    continue;
                }
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    List<String> lore = item.getItemMeta().getLore();
                    boolean isPreview = lore != null && lore.stream().anyMatch(l -> l.contains("Not enough in inventory"));
                    if (isPreview) {
                        gui.setItem(slot, null);
                        continue;
                    }
                }
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                gui.setItem(slot, null);
            }
        }
        player.updateInventory();
    }

    private static boolean matchesIngredients(List<ItemStack> ingredients, ItemStack cost1, ItemStack cost2) {
        List<ItemStack> placed = new ArrayList<>();
        if (cost1 != null && !cost1.getType().isAir()) placed.add(cost1);
        if (cost2 != null && !cost2.getType().isAir()) placed.add(cost2);

        if (placed.size() != ingredients.size()) return false;

        for (int i = 0; i < placed.size(); i++) {
            if (!isStrictlyEqual(placed.get(i), ingredients.get(i))) return false;
        }
        return true;
    }

    private static boolean isStrictlyEqual(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (a.getAmount() != b.getAmount()) return false;

        ItemStack a1 = a.clone(); a1.setAmount(1);
        ItemStack b1 = b.clone(); b1.setAmount(1);

        try {
            return Arrays.equals(a1.serializeAsBytes(), b1.serializeAsBytes());
        } catch (Throwable t) {
            return a1.isSimilar(b1);
        }
    }

    public static ItemStack buildTradeDisplay(MerchantRecipe recipe, Player player) {
        ItemStack display = recipe.getResult().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        boolean canBuy = true;
        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());
        lore.add("§7§m--------------------");
        lore.add("§eCost:");

        for (ItemStack ing : recipe.getIngredients()) {
            int have = countSimilar(player, ing);
            boolean ok = have >= ing.getAmount();
            if (!ok) canBuy = false;
            lore.add((ok ? "§a✔ " : "§c✘ ") + "§f" + ing.getAmount() + "x §e" + pretty(ing) + " §7(" + have + ")");
        }

        lore.add("§7§m--------------------");
        lore.add(canBuy ? "§aClick to select!" : "§cNot enough items!");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private static ItemStack staticItem(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String pretty(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String n = item.getType().name().toLowerCase().replace("_", " ");
        return n.substring(0, 1).toUpperCase() + n.substring(1);
    }

    public static int countSimilar(Player player, ItemStack needle) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null) continue;
            if (slot.isSimilar(needle)) count += slot.getAmount();
        }
        return count;
    }

    private static void removeItems(Player player, ItemStack needle, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (slot == null || !slot.isSimilar(needle)) continue;
            int take = Math.min(slot.getAmount(), remaining);
            slot.setAmount(slot.getAmount() - take);
            remaining -= take;
            player.getInventory().setItem(i, slot.getAmount() <= 0 ? null : slot);
        }
        player.updateInventory();
    }
}