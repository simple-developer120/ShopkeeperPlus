package de.shop.data;

import de.shop.VillagerShop;
import de.shop.listeners.ShopEditorListener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopDataManager {

    public static final String DEFAULT_NAME = "§6Shopkeeper";

    private final VillagerShop plugin;
    private final File dataFolder;
    private final Map<String, ShopData> shops = new LinkedHashMap<>();
    private final Random random = new Random();

    public ShopDataManager(VillagerShop plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "shopdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        loadAll();
    }

    public String generateUniqueFileName() {
        String name;
        do {
            name = randomString(8);
        } while (new File(dataFolder, name + ".yml").exists());
        return name;
    }

    private String randomString(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    public static String sanitizeFileName(String name) {
        if (name == null) return "";
        String cleaned = name
                .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32);
        if (cleaned.isEmpty()) cleaned = "shop_" + System.currentTimeMillis() % 100000;
        return cleaned;
    }

public void save(ShopData data) {
    File file = new File(dataFolder, data.getFileName() + ".yml");
    YamlConfiguration cfg = new YamlConfiguration();

    cfg.set("customName", data.getCustomName());
    cfg.set("entity.uuid", data.getEntityUuid() != null ? data.getEntityUuid().toString() : null);
    cfg.set("entity.type", data.getEntityType() != null ? data.getEntityType().name() : null);
    cfg.set("tradesOpen", data.isTradesOpen());
    cfg.set("profession", data.getProfession());
    cfg.set("isBaby", data.isBaby());
    cfg.set("villagerType", data.getVillagerType());
    cfg.set("villagerLevel", data.getVillagerLevel());

    if (data.getLocation() != null) {
        Location loc = data.getLocation();
        cfg.set("entity.location.world", loc.getWorld().getName());
        cfg.set("entity.location.x", loc.getX());
        cfg.set("entity.location.y", loc.getY());
        cfg.set("entity.location.z", loc.getZ());
        cfg.set("entity.location.yaw", loc.getYaw());
        cfg.set("entity.location.pitch", loc.getPitch());
    }

    cfg.set("currentPage", data.getCurrentPage());

    List<Map<String, Object>> tradeList = new ArrayList<>();
    for (MerchantRecipe recipe : data.getRecipes()) {
        if (recipe == null) continue;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("resultBytes", itemToBase64(recipe.getResult()));
        List<ItemStack> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            map.put("ingredientBytes" + i, itemToBase64(ingredients.get(i)));
        }
        map.put("maxUses", recipe.getMaxUses());
        tradeList.add(map);
    }
    cfg.set("trades", tradeList);

    for (Map.Entry<org.bukkit.inventory.EquipmentSlot, ItemStack> entry : data.getEquipment().entrySet()) {
        cfg.set("equipment." + entry.getKey().name(), itemToBase64(entry.getValue()));
    }

    try {
        cfg.save(file);
    } catch (IOException e) {
        plugin.getLogger().severe("Failed to save " + file.getName() + ": " + e.getMessage());
    }
}

    public void delete(ShopData data) {
        File file = new File(dataFolder, data.getFileName() + ".yml");
        if (file.exists()) file.delete();
        shops.remove(data.getFileName());
    }

    public boolean renameFile(ShopData data, String newBaseName) {
        String sanitized = sanitizeFileName(newBaseName);
        if (sanitized.isEmpty()) return false;
        if (sanitized.equals(data.getFileName())) return true;

        if (shops.containsKey(sanitized)) {
            sanitized = sanitized + "_" + (System.currentTimeMillis() % 1000);
            if (shops.containsKey(sanitized)) return false;
        }

        String oldFileName = data.getFileName();
        File oldFile = new File(dataFolder, oldFileName + ".yml");

        shops.remove(oldFileName);
        data.setFileName(sanitized);
        shops.put(sanitized, data);

        save(data);
        if (oldFile.exists()) oldFile.delete();
        return true;
    }

    public void loadAll() {
        shops.clear();
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String fileName = file.getName().replace(".yml", "");
            ShopData data = loadFromFile(fileName, file);
            if (data != null) shops.put(fileName, data);
        }
    }

    private ShopData loadFromFile(String fileName, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ShopData data = new ShopData(fileName);
        
        data.setTradesOpen(cfg.getBoolean("tradesOpen", true));
        data.setCustomName(cfg.getString("customName"));
        data.setProfession(cfg.getString("profession", "WEAPONSMITH"));
        data.setBaby(cfg.getBoolean("isBaby", false));
        data.setVillagerType(cfg.getString("villagerType", "PLAINS"));
        data.setVillagerLevel(cfg.getInt("villagerLevel", 1));
        
        String uuidStr = cfg.getString("entity.uuid");
        if (uuidStr != null) {
            try { data.setEntityUuid(UUID.fromString(uuidStr)); }
            catch (IllegalArgumentException ignored) {}
        }

        String typeStr = cfg.getString("entity.type");
        if (typeStr != null) {
            try { data.setEntityType(EntityType.valueOf(typeStr)); }
            catch (IllegalArgumentException ignored) {}
        }

        if (cfg.contains("entity.location.world")) {
            World world = Bukkit.getWorld(cfg.getString("entity.location.world"));
            if (world != null) {
                data.setLocation(new Location(world,
                        cfg.getDouble("entity.location.x"),
                        cfg.getDouble("entity.location.y"),
                        cfg.getDouble("entity.location.z"),
                        (float) cfg.getDouble("entity.location.yaw"),
                        (float) cfg.getDouble("entity.location.pitch")));
            }
        }

        data.setCurrentPage(cfg.getInt("currentPage", 0));

        for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
            String b64 = cfg.getString("equipment." + slot.name());
            if (b64 != null) {
                ItemStack item = itemFromBase64(b64);
                if (item != null) data.getEquipment().put(slot, item);
            }
        }
        
        List<Map<?, ?>> rawTrades = cfg.getMapList("trades");
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (Map<?, ?> raw : rawTrades) {
            ItemStack result = null;
            if (raw.containsKey("resultBytes")) result = itemFromBase64((String) raw.get("resultBytes"));
            else if (raw.containsKey("result")) result = (ItemStack) raw.get("result");
            if (result == null) continue;

            MerchantRecipe recipe = new MerchantRecipe(result, raw.get("maxUses") instanceof Integer i ? i : Integer.MAX_VALUE);
            int idx = 0;
            while (raw.containsKey("ingredientBytes" + idx)) {
                ItemStack ing = itemFromBase64((String) raw.get("ingredientBytes" + idx));
                if (ing != null) recipe.addIngredient(ing);
                idx++;
            }
            if (idx == 0) {
                idx = 0;
                while (raw.containsKey("ingredient" + idx)) {
                    ItemStack ing = (ItemStack) raw.get("ingredient" + idx);
                    if (ing != null) recipe.addIngredient(ing);
                    idx++;
                }
            }
            recipes.add(recipe);
        }
        data.setRecipes(recipes);
        return data;
    }

    public void respawnMissing() {
        Set<UUID> existingUuids = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (ShopEditorListener.isShopkeeper(e)) existingUuids.add(e.getUniqueId());
            }
        }
        for (ShopData data : shops.values()) {
            Location loc = data.getLocation();
            if (loc == null || loc.getWorld() == null) continue;
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            if (data.getEntityUuid() != null && existingUuids.contains(data.getEntityUuid())) continue;
            spawnEntity(data);
        }
    }

 public void spawnEntity(ShopData data) {
    Location loc = data.getLocation();
    if (loc == null || loc.getWorld() == null) return;

    EntityType type = data.getEntityType() != null ? data.getEntityType() : EntityType.VILLAGER;

    try {
        Entity entity = loc.getWorld().spawnEntity(loc, type, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
        entity.setPersistent(true);
        ShopEditorListener.markAsShopkeeper(entity, plugin);

        if (entity instanceof Mob mob) {
            mob.setAI(false);
            mob.setInvulnerable(true);
            mob.setSilent(true);
            mob.setCollidable(false);
            mob.setTarget(null);
            String name = data.getCustomName() != null ? data.getCustomName() : DEFAULT_NAME;
            mob.setCustomName(name);
            mob.setCustomNameVisible(false);
            mob.setRemoveWhenFarAway(false);
            
            if (mob instanceof ZombieVillager zv) {
                zv.setShouldBurnInDay(false);
                zv.setCanBreakDoors(false);
            }
        }
        
        if (entity instanceof org.bukkit.entity.LivingEntity living && !(entity instanceof org.bukkit.entity.WanderingTrader)) {
            org.bukkit.inventory.EntityEquipment eq = living.getEquipment();
            if (eq != null) {
                for (Map.Entry<org.bukkit.inventory.EquipmentSlot, ItemStack> entry : data.getEquipment().entrySet()) {
                    eq.setItem(entry.getKey(), entry.getValue());
                }
            }
        }
        
        if (entity instanceof Villager villager) {
            try {
                villager.setProfession(Villager.Profession.valueOf(data.getProfession()));
            } catch (Exception ignored) {
                villager.setProfession(Villager.Profession.WEAPONSMITH);
            }

            try {
                villager.setVillagerType(Villager.Type.valueOf(data.getVillagerType()));
            } catch (Exception ignored) {
                villager.setVillagerType(Villager.Type.PLAINS);
            }

            villager.setVillagerLevel(data.getVillagerLevel());
            if (villager.getVillagerExperience() == 0) {
                villager.setVillagerExperience(1); 
            }

            if (data.isBaby()) villager.setBaby();
            else villager.setAdult();
        }
        else if (entity instanceof ZombieVillager zv) {
            try {
                zv.setVillagerProfession(Villager.Profession.valueOf(data.getProfession()));
            } catch (Exception ignored) {
                zv.setVillagerProfession(Villager.Profession.WEAPONSMITH);
            }

            if (data.isBaby()) zv.setBaby();
            else zv.setAdult();
        }

        if (entity instanceof Merchant merchant) {
            merchant.setRecipes(data.getRecipes());
        }
        
        if (entity instanceof org.bukkit.entity.WanderingTrader wt) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (wt.isDead()) return;
                
                org.bukkit.inventory.EntityEquipment equipment = wt.getEquipment();
                if (equipment != null) {
                    equipment.setItemInMainHand(new ItemStack(Material.AIR));
                    equipment.setItemInOffHand(new ItemStack(Material.AIR));
                    
                    ItemStack mainHand = data.getEquipment().get(org.bukkit.inventory.EquipmentSlot.HAND);
                    if (mainHand != null && !mainHand.getType().isAir()) {
                        equipment.setItemInMainHand(mainHand);
                    }
                    
                    ItemStack offHand = data.getEquipment().get(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
                    if (offHand != null && !offHand.getType().isAir()) {
                        equipment.setItemInOffHand(offHand);
                    }
                }
            });
        }

        data.setEntityUuid(entity.getUniqueId());
        save(data);
    } catch (Exception ex) {
        plugin.getLogger().severe("Failed to spawn " + data.getFileName() + ": " + ex.getMessage());
    }
}

    public ShopData getByEntity(UUID uuid) {
        if (uuid == null) return null;
        for (ShopData data : shops.values()) {
            if (uuid.equals(data.getEntityUuid())) return data;
        }
        return null;
    }

    public ShopData getByName(String fileName) {
        if (fileName == null) return null;
        return shops.get(fileName);
    }

    public Collection<String> getAllNames() { return new ArrayList<>(shops.keySet()); }
    public void register(ShopData data) { shops.put(data.getFileName(), data); }
    public Collection<ShopData> getAll() { return shops.values(); }

    public static net.kyori.adventure.text.Component nameToComponent(String name) {
        String safe = name != null ? name : DEFAULT_NAME;
        return LegacyComponentSerializer.legacySection().deserialize(safe);
    }
    
    private static String itemToBase64(ItemStack item) {
        if (item == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos);
            out.writeObject(item);
            out.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) { return null; }
    }

    private static ItemStack itemFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream in = new BukkitObjectInputStream(bais);
            ItemStack item = (ItemStack) in.readObject();
            in.close();
            return item;
        } catch (Exception e) { return null; }
    }
}
