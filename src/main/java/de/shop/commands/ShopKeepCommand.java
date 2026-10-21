package de.shop.commands;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import de.shop.data.ShopDataManager;
import de.shop.listeners.ShopEditorListener;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.*;

public class ShopKeepCommand implements CommandExecutor, TabCompleter {

    private final VillagerShop plugin;

    public ShopKeepCommand(VillagerShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadPlugin();
                sender.sendMessage("§aPlugin reloaded!");
                return true;
            }
            sender.sendMessage("§cOnly players can use this!");
            return true;
        }
        
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("shopkeeperplus.admin")) {
                player.sendMessage("§cYou don't have permission!");
                return true;
            }
            plugin.reloadPlugin();
            player.sendMessage("§aShopKeeperPlus reloaded successfully!");
            return true;
        }

        if (args.length == 0) {
            spawnShopkeeper(player, EntityType.VILLAGER);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("type")) {
            EntityType type;
            try { type = EntityType.valueOf(args[1].toUpperCase()); } 
            catch (IllegalArgumentException e) { player.sendMessage("§cInvalid entity: " + args[1]); return true; }
            
            if (!isValidShopEntity(type)) {
                player.sendMessage("§c" + type.name() + " does not support custom trades!");
                return true;
            }
            spawnShopkeeper(player, type);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            deleteShopkeeper(player, args[1]);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("movehere")) {
            moveHere(player, args[1]);
            return true;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("rename")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(" ");
                sb.append(args[i]);
            }
            renameShopkeeper(player, args[1], sb.toString());
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            tpShopkeeper(player, args[1]);
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("changetype")) {
            changeType(player, args[1], args[2]);
            return true;
        }

        player.sendMessage("§e/shopkeep §7- Spawn a Villager shopkeeper");
        player.sendMessage("§e/shopkeep type <entity> §7- Spawn a custom entity shopkeeper");
        player.sendMessage("§e/shopkeep delete <name> §7- Delete a shopkeeper");
        player.sendMessage("§e/shopkeep movehere <name> §7- Move a shopkeeper to you");
        player.sendMessage("§e/shopkeep tp <name> §7- Teleport to a shopkeeper");
        player.sendMessage("§e/shopkeep rename <name> <newName> §7- Rename a shopkeeper");
        player.sendMessage("§e/shopkeep changetype <name> <type> §7- Change entity type of a shopkeeper");
        player.sendMessage("§e/shopkeep reload §7- Reload the plugin");
        return true;
    }

    private void tpShopkeeper(Player player, String name) {
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
        ShopData data = plugin.getShopDataManager().getByName(name);
        if (data == null) { player.sendMessage("§cShopkeeper '" + name + "' not found!"); return; }
        if (data.getLocation() == null) { player.sendMessage("§cNo location found for this shopkeeper!"); return; }
        
        player.teleport(data.getLocation());
        player.sendMessage("§aTeleported to shopkeeper '" + name + "'.");
    }

    private void changeType(Player player, String name, String typeStr) {
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
        ShopData data = plugin.getShopDataManager().getByName(name);
        if (data == null) { player.sendMessage("§cShopkeeper '" + name + "' not found!"); return; }

        EntityType type;
        try { type = EntityType.valueOf(typeStr.toUpperCase()); } 
        catch (Exception e) { player.sendMessage("§cInvalid entity type!"); return; }

        if (!isValidShopEntity(type)) { player.sendMessage("§cThis entity cannot be a shopkeeper!"); return; }

        if (data.getEntityUuid() != null) {
            Entity old = Bukkit.getEntity(data.getEntityUuid());
            if (old != null) old.remove();
        }
        
        data.setEntityType(type);
        plugin.getShopDataManager().spawnEntity(data);
        player.sendMessage("§aChanged type to " + type.name() + " and respawned!");
    }

    private void moveHere(Player player, String name) {
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
        ShopData data = plugin.getShopDataManager().getByName(name);
        if (data == null) { player.sendMessage("§cShopkeeper '" + name + "' not found!"); return; }

        if (data.getEntityUuid() != null) {
            Entity old = Bukkit.getEntity(data.getEntityUuid());
            if (old != null) old.remove();
        }

        data.setLocation(player.getLocation());
        plugin.getShopDataManager().spawnEntity(data);
        player.sendMessage("§aShopkeeper '" + name + "' moved to your location!");
    }

    private void renameShopkeeper(Player player, String name, String newName) {
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
        ShopData data = plugin.getShopDataManager().getByName(name);
        if (data == null) { player.sendMessage("§cShopkeeper '" + name + "' not found!"); return; }

        String colored = ChatColor.translateAlternateColorCodes('&', newName);
        data.setCustomName(colored);

        if (data.getEntityUuid() != null) {
            Entity e = Bukkit.getEntity(data.getEntityUuid());
            if (e instanceof Mob mob) {
                mob.setCustomName(colored);
                mob.setCustomNameVisible(false);
            }
        }

        boolean renamed = plugin.getShopDataManager().renameFile(data, colored);
        plugin.getShopDataManager().save(data);

        player.sendMessage("§aShopkeeper renamed to: " + colored);
        if (renamed) player.sendMessage("§7File: §f" + data.getFileName() + ".yml");
        else player.sendMessage("§eFile rename failed — name applied but yml kept.");
    }

    private void deleteShopkeeper(Player player, String name) {
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
        ShopData data = plugin.getShopDataManager().getByName(name);
        if (data == null) { player.sendMessage("§cShopkeeper '" + name + "' not found!"); return; }

        if (data.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity != null) entity.remove();
        }

        plugin.getShopDataManager().delete(data);
        player.sendMessage("§aShopkeeper '" + name + "' deleted!");
    }

    private void spawnShopkeeper(Player player, EntityType type) {
        Location loc = player.getLocation();
        Entity entity = player.getWorld().spawnEntity(loc, type, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
        entity.setPersistent(true);
        ShopEditorListener.markAsShopkeeper(entity, plugin);

        if (entity instanceof Mob mob) {
            mob.setAI(false);
            mob.setInvulnerable(true);
            mob.setSilent(true);
            mob.setCollidable(false);
            mob.setTarget(null);
            mob.setCustomName(ShopDataManager.DEFAULT_NAME);
            mob.setCustomNameVisible(false);
            mob.setRemoveWhenFarAway(false);

            if (mob instanceof org.bukkit.entity.ZombieVillager zv) {
                zv.setShouldBurnInDay(false);
                zv.setCanBreakDoors(false);
            }
        }

        if (entity instanceof Villager villager) {
            villager.setProfession(Villager.Profession.WEAPONSMITH);
            villager.setVillagerLevel(1);
        }

        if (entity instanceof org.bukkit.entity.Ageable ageable) {
            ageable.setAdult();
        }

        List<MerchantRecipe> recipes = new ArrayList<>();
        MerchantRecipe r1 = new MerchantRecipe(new ItemStack(Material.DIAMOND_SWORD), Integer.MAX_VALUE);
        r1.addIngredient(new ItemStack(Material.DIAMOND, 5));
        recipes.add(r1);

        MerchantRecipe r2 = new MerchantRecipe(new ItemStack(Material.ENCHANTED_BOOK), Integer.MAX_VALUE);
        r2.addIngredient(new ItemStack(Material.EMERALD, 3));
        r2.addIngredient(new ItemStack(Material.BOOK, 1));
        recipes.add(r2);

        if (entity instanceof Merchant merchant) {
            merchant.setRecipes(recipes);
        }

        String fileName = plugin.getShopDataManager().generateUniqueFileName();
        ShopData data = new ShopData(fileName);
        data.setEntityUuid(entity.getUniqueId());
        data.setEntityType(type);
        data.setLocation(entity.getLocation());
        data.setRecipes(recipes);
        data.setCurrentPage(0);
        data.setCustomName(null);

        plugin.getShopDataManager().save(data);
        plugin.getShopDataManager().register(data);

        player.sendMessage("§aShopkeeper created!");
        player.sendMessage("§7Saved as: shopdata/" + fileName + ".yml");
    }

    private boolean isValidShopEntity(EntityType type) {
        return type.isAlive() && type.isSpawnable() && type != EntityType.PLAYER;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("type", "delete", "movehere", "tp", "rename", "changetype", "reload").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("type")) {
                return Arrays.stream(EntityType.values())
                        .filter(this::isValidShopEntity)
                        .map(EntityType::name)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("movehere")
                    || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("rename")
                    || args[0].equalsIgnoreCase("changetype")) {
                return plugin.getShopDataManager().getAllNames().stream()
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("changetype")) {
            return Arrays.stream(EntityType.values())
                    .filter(this::isValidShopEntity)
                    .map(EntityType::name)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}