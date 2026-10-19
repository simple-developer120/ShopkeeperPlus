package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportListener implements Listener {

    private final VillagerShop plugin;
    private final Map<UUID, UUID> awaitingTeleport = new HashMap<>();

    public TeleportListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    public void startTeleportMode(Player player, UUID shopkeeperUuid) {
        awaitingTeleport.put(player.getUniqueId(), shopkeeperUuid);
        player.sendMessage("§eRight-click to teleport the shopkeeper to the block you look at.");
        player.sendMessage("§cLeft-click to cancel.");
    }

    public void cancelTeleportMode(Player player) {
        awaitingTeleport.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!awaitingTeleport.containsKey(player.getUniqueId())) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            UUID shopkeeperUuid = awaitingTeleport.get(player.getUniqueId());
            // ✅ جستجوی مطمئن‌تر
            Entity found = Bukkit.getEntity(shopkeeperUuid);
            if (found == null || found.isDead()) {
                player.sendMessage("§cShopkeeper not found!");
                cancelTeleportMode(player);
                return;
            }

            Location target = getLookedAtLocation(player, 10);
            if (target == null) {
                player.sendMessage("§cNo target block found.");
                return;
            }

            found.teleport(target);

            ShopData data = plugin.getShopDataManager().getByEntity(found.getUniqueId());
            if (data != null) {
                data.setLocation(target);
                plugin.getShopDataManager().save(data);
            }

            player.sendMessage("§aShopkeeper teleported!");
            cancelTeleportMode(player);
            return;
        }

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            cancelTeleportMode(player);
            player.sendMessage("§cTeleport cancelled.");
        }
    }

    private Location getLookedAtLocation(Player player, int maxDistance) {
        RayTraceResult result = player.rayTraceBlocks(maxDistance);
        if (result != null && result.getHitBlock() != null) {
            Block block = result.getHitBlock();
            return block.getLocation().add(0.5, 1, 0.5);
        }
        return null;
    }
}