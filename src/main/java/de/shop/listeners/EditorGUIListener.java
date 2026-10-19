package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import de.shop.gui.EquipmentGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.*;

public class EditorGUIListener implements Listener {

    private final VillagerShop plugin;

    public static final int TRADES_PER_PAGE = 9;
    public static final int MAX_PAGES = 20;
    public static final int EDITABLE_MAX_SLOT = 26;

    public static int getResultSlot(int col) { return col; }
    public static int getCost1Slot(int col)  { return 9 + col; }
    public static int getCost2Slot(int col)  { return 18 + col; }
        
    public static final int SLOT_CLOSE_TRADE = 36;
    public static final int SLOT_PREV_PAGE   = 27;
    public static final int SLOT_TELEPORT    = 38;
    public static final int SLOT_BABY        = 40;
    public static final int SLOT_TYPE        = 42;
    public static final int SLOT_PROFESSION  = 41;
    public static final int SLOT_DELETE      = 44;
    public static final int SLOT_NEXT_PAGE   = 35;
    public static final int SLOT_NAME_TAG    = 37;
    public static final int SLOT_ARMOR       = 39;
    public static final int SLOT_BADGE       = 43;

    private static final Villager.Profession[] CUSTOM_PROFESSIONS = {
            Villager.Profession.NONE, Villager.Profession.FISHERMAN, Villager.Profession.LEATHERWORKER,
            Villager.Profession.MASON, Villager.Profession.FLETCHER, Villager.Profession.WEAPONSMITH,
            Villager.Profession.TOOLSMITH, Villager.Profession.LIBRARIAN, Villager.Profession.SHEPHERD,
            Villager.Profession.FARMER, Villager.Profession.CLERIC, Villager.Profession.NITWIT,
            Villager.Profession.CARTOGRAPHER, Villager.Profession.ARMORER, Villager.Profession.BUTCHER
    };

    private static final Villager.Type[] CUSTOM_TYPES = {
            Villager.Type.PLAINS, Villager.Type.JUNGLE, Villager.Type.TAIGA, 
            Villager.Type.DESERT, Villager.Type.SAVANNA, Villager.Type.SWAMP, 
            Villager.Type.SNOW
    };

    private final Map<UUID, UUID> editingEntity = new HashMap<>();
    private final Set<UUID> skipNextClose = new HashSet<>();

    public EditorGUIListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    public void setEditingEntity(Player player, Entity entity) {
        editingEntity.put(player.getUniqueId(), entity.getUniqueId());
    }

    public void clearEditingEntity(Player player) {
        editingEntity.remove(player.getUniqueId());
    }

    private Entity getEditingEntity(Player player) {
        UUID uuid = editingEntity.get(player.getUniqueId());
        if (uuid == null) return null;
        Entity e = Bukkit.getEntity(uuid);
        if (e != null && !e.isDead()) return e;
        return null;
    }

    public void markSkipNextClose(Player player) {
        skipNextClose.add(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.startsWith("§8ShopkeeperPlus Editor")) return;

        int slot = event.getSlot();
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();
        Inventory bottomInv = event.getView().getBottomInventory();

        if (clickedInv != null && clickedInv.equals(bottomInv)) {
            if (event.isShiftClick() && event.getRawSlot() > EDITABLE_MAX_SLOT) {
                event.setCancelled(true);
            }
            return;
        }

        if (clickedInv != null && clickedInv.equals(topInv)) {
            if (slot <= EDITABLE_MAX_SLOT) return;

            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            Entity entity = getEditingEntity(player);
            if (entity == null) {
                player.closeInventory();
                player.sendMessage("§cShopkeeper not found!");
                return;
            }

            if (slot == SLOT_ARMOR) {
                if (entity instanceof org.bukkit.entity.LivingEntity) {
                    markSkipNextClose(player);
                    player.closeInventory();
                    plugin.getEquipmentGUIListener().setEditingEntity(player, entity);
                    EquipmentGUI.open(player, entity);
                }
            }
            else if (slot == SLOT_NAME_TAG) {
                saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                markSkipNextClose(player);
                player.closeInventory();
                ChatInputListener.awaitNameInput(player, entity.getUniqueId());
                player.sendMessage("§aPlease enter in chat the name of ShopKeeper Name to Change.");
                player.sendMessage("§7Use & for colors. Type §ccancel §7to cancel.");
            }
            else if (slot == SLOT_CLOSE_TRADE) {
                ShopData data = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                if (data != null) {
                    boolean newState = !data.isTradesOpen();
                    data.setTradesOpen(newState);
                    plugin.getShopDataManager().save(data);
                    player.sendMessage(newState ? "§aTrades opened!" : "§cTrades closed!");
                    saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                    markSkipNextClose(player);
                    plugin.getShopEditorListener().openEditorGUI(player, entity);
                }
            }
            else if (slot == SLOT_BADGE) {
                ShopData sData = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                if (sData != null) {
                    int nextLevel = sData.getVillagerLevel() + 1;
                    if (nextLevel > 5) nextLevel = 1;
                    
                    sData.setVillagerLevel(nextLevel);
                    
                    if (entity instanceof Villager v) {
                        v.setVillagerLevel(nextLevel);
                        if (v.getVillagerExperience() == 0) v.setVillagerExperience(1);
                    }
                    
                    plugin.getShopDataManager().save(sData);
                    
                    saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                    markSkipNextClose(player);
                    plugin.getShopEditorListener().openEditorGUI(player, entity);
                }
            }
            else if (slot == SLOT_PROFESSION) {
                if (entity instanceof Villager v) {
                    int current = Arrays.asList(CUSTOM_PROFESSIONS).indexOf(v.getProfession());
                    if (current == -1) current = 0;
                    int next = (current + 1) % CUSTOM_PROFESSIONS.length;
                    Villager.Profession nextProf = CUSTOM_PROFESSIONS[next];
                    
                    v.setProfession(nextProf);
                    
                    ShopData sData = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                    if (sData != null) {
                        sData.setProfession(nextProf.name());
                        plugin.getShopDataManager().save(sData);
                    }
                    
                    player.sendMessage("§bProfession: " + nextProf.name());
                    saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                    markSkipNextClose(player);
                    plugin.getShopEditorListener().openEditorGUI(player, entity);
                }
                else if (entity instanceof ZombieVillager zv) {
                    int current = Arrays.asList(CUSTOM_PROFESSIONS).indexOf(zv.getVillagerProfession());
                    if (current == -1) current = 0;
                    int next = (current + 1) % CUSTOM_PROFESSIONS.length;
                    Villager.Profession nextProf = CUSTOM_PROFESSIONS[next];
                    
                    zv.setVillagerProfession(nextProf);
                    
                    ShopData sData = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                    if (sData != null) {
                        sData.setProfession(nextProf.name());
                        plugin.getShopDataManager().save(sData);
                    }
                    
                    player.sendMessage("§bProfession: " + nextProf.name());
                    saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                    markSkipNextClose(player);
                    plugin.getShopEditorListener().openEditorGUI(player, entity);
                }
            }
            // ✅ بلاک Type که جا افتاده بود
            else if (slot == SLOT_TYPE) {
                if (entity instanceof Villager v) {
                    int current = Arrays.asList(CUSTOM_TYPES).indexOf(v.getVillagerType());
                    if (current == -1) current = 0;
                    int next = (current + 1) % CUSTOM_TYPES.length;
                    Villager.Type nextType = CUSTOM_TYPES[next];
                    
                    v.setVillagerType(nextType);
                    
                    ShopData sData = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                    if (sData != null) {
                        sData.setVillagerType(nextType.name());
                        plugin.getShopDataManager().save(sData);
                    }
                    
                    player.sendMessage("§aVillager type: " + nextType.name());
                    saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                    markSkipNextClose(player);
                    plugin.getShopEditorListener().openEditorGUI(player, entity);
                }
            }
            else if (slot == SLOT_BABY) {
                if (entity instanceof Villager v) {
                    ShopData sData = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                    if (v.isAdult()) { 
                        v.setBaby(); 
                        if(sData != null) sData.setBaby(true); 
                        player.sendMessage("§dBaby mode!"); 
                    } else { 
                        v.setAdult(); 
                        if(sData != null) sData.setBaby(false); 
                        player.sendMessage("§dAdult mode!"); 
                    }
                    if (sData != null) plugin.getShopDataManager().save(sData);
                    saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                    markSkipNextClose(player);
                    plugin.getShopEditorListener().openEditorGUI(player, entity);
                }
                else if (entity instanceof ZombieVillager zv) {
                    ShopData sData = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                    if (!zv.isBaby()) { 
                        zv.setBaby(); 
                        if(sData != null) sData.setBaby(true); 
                        player.sendMessage("§dBaby mode!"); 
                    } else { 
                        zv.setAdult(); 
                        if(sData != null) sData.setBaby(false); 
                        player.sendMessage("§dAdult mode!"); 
                    }
                    if (sData != null) plugin.getShopDataManager().save(sData);
                    saveAllTradesFromPage(topInv, entity, plugin.getPaginationManager().getPage(player), player);
                    markSkipNextClose(player);
                    plugin.getShopEditorListener().openEditorGUI(player, entity);
                }
            }
            else if (slot == SLOT_TELEPORT) {
                player.closeInventory();
                plugin.getTeleportListener().startTeleportMode(player, entity.getUniqueId());
            }
            else if (slot == SLOT_DELETE) {
                ShopData data = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
                if (data != null) plugin.getShopDataManager().delete(data);
                entity.remove();
                clearEditingEntity(player);
                player.closeInventory();
                player.sendMessage("§cShopkeeper deleted!");
            }
            else if (slot == SLOT_NEXT_PAGE) {
                int oldPage = plugin.getPaginationManager().getPage(player);
                if (oldPage + 1 >= MAX_PAGES) return;
                saveAllTradesFromPage(topInv, entity, oldPage, player);
                plugin.getPaginationManager().setPage(player, oldPage + 1);
                markSkipNextClose(player);
                plugin.getShopEditorListener().openEditorGUI(player, entity);
            }
            else if (slot == SLOT_PREV_PAGE) {
                int oldPage = plugin.getPaginationManager().getPage(player);
                if (oldPage <= 0) return;
                saveAllTradesFromPage(topInv, entity, oldPage, player);
                plugin.getPaginationManager().setPage(player, oldPage - 1);
                markSkipNextClose(player);
                plugin.getShopEditorListener().openEditorGUI(player, entity);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (skipNextClose.contains(player.getUniqueId())) {
            skipNextClose.remove(player.getUniqueId());
            return;
        }

        String title = event.getView().getTitle();
        if (!title.startsWith("§8ShopkeeperPlus Editor")) return;

        Entity entity = getEditingEntity(player);
        if (entity == null) { clearEditingEntity(player); return; }

        int pageIndex = plugin.getPaginationManager().getPage(player);
        saveAllTradesFromPage(event.getView().getTopInventory(), entity, pageIndex, player);
        clearEditingEntity(player);
    }

private void saveAllTradesFromPage(Inventory topInv, Entity entity, int pageIndex, Player player) {
    ShopData data = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
    if (data == null) return;

    List<MerchantRecipe> recipes = new ArrayList<>(data.getRecipes());
    int pageStart = pageIndex * TRADES_PER_PAGE;
    int added = 0, updated = 0, removed = 0;

    for (int col = 0; col < TRADES_PER_PAGE; col++) {
        int globalIndex = pageStart + col;

        ItemStack result = topInv.getItem(getResultSlot(col));
        ItemStack cost1  = topInv.getItem(getCost1Slot(col));
        ItemStack cost2  = topInv.getItem(getCost2Slot(col));

        boolean hasResult = result != null && !result.getType().isAir();
        boolean hasCost1  = cost1  != null && !cost1.getType().isAir();
        boolean hasCost2  = cost2  != null && !cost2.getType().isAir();

        // اگه Result خالی بود، ترید حذف بشه
        if (!hasResult) {
            if (globalIndex < recipes.size() && recipes.get(globalIndex) != null) {
                recipes.set(globalIndex, null);
                removed++;
            }
            continue;
        }

        // اگه هر دو Cost خالی بود، ترید حذف بشه
        if (!hasCost1 && !hasCost2) {
            if (globalIndex < recipes.size() && recipes.get(globalIndex) != null) {
                recipes.set(globalIndex, null);
                removed++;
            }
            continue;
        }

        MerchantRecipe recipe = new MerchantRecipe(result.clone(), Integer.MAX_VALUE);
        
        // ✅ ذخیره بدون فشرده کردن آیتم‌ها
        if (hasCost1 && hasCost2) {
            recipe.addIngredient(cost1.clone());
            recipe.addIngredient(cost2.clone());
        } 
        else if (hasCost1) {
            recipe.addIngredient(cost1.clone());
        } 
        else {
            recipe.addIngredient(cost2.clone()); // فقط پایین پر بود، به عنوان آیتم اول ذخیره میشه
        }

        while (recipes.size() <= globalIndex) recipes.add(null);
        boolean wasEmpty = recipes.get(globalIndex) == null;
        recipes.set(globalIndex, recipe);
        
        if (wasEmpty) added++; 
        else updated++;
    }

    // ✅ فشرده‌سازی نهایی: حذف تمام اسلات‌های خالی (null) برای جلوگیری از کرش و پرش آیتم‌ها
    recipes.removeIf(Objects::isNull);

    data.setRecipes(recipes);
    plugin.getShopDataManager().save(data);

    if (player != null && (added + updated + removed > 0)) {
        player.sendMessage("§aPage " + (pageIndex + 1) + " saved §7(§a+"
                + added + " §7new, §e~" + updated + " §7updated, §c-"
                + removed + " §7removed)");
    }
}
}