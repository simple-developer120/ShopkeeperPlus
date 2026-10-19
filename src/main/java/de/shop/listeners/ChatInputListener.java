package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import de.shop.data.ShopDataManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatInputListener implements Listener {

    private final VillagerShop plugin;

    private static final Map<UUID, UUID> awaitingNameInput = new HashMap<>();

    public ChatInputListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    public static void awaitNameInput(Player player, UUID entityUuid) {
        awaitingNameInput.put(player.getUniqueId(), entityUuid);
    }

    public static void cancelAwait(Player player) {
        awaitingNameInput.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID entityUuid = awaitingNameInput.get(player.getUniqueId());
        if (entityUuid == null) return;

        event.setCancelled(true);
        awaitingNameInput.remove(player.getUniqueId());

        String input = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cName change cancelled.");
            return;
        }

        final String colored = ChatColor.translateAlternateColorCodes('&', input);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity == null || entity.isDead()) {
                player.sendMessage("§cShopkeeper not found!");
                return;
            }

            ShopData data = plugin.getShopDataManager().getByEntity(entity.getUniqueId());
            if (data == null) {
                player.sendMessage("§cShopData not found!");
                return;
            }

            data.setCustomName(colored);

            if (entity instanceof Mob mob) {
                mob.setCustomName(colored);
                mob.setCustomNameVisible(false);
            }

            boolean renamed = plugin.getShopDataManager().renameFile(data, colored);

            
            plugin.getShopDataManager().save(data);

            if (renamed) {
                player.sendMessage("§aShopkeeper renamed to: " + colored);
                player.sendMessage("§7File: §f" + data.getFileName() + ".yml");
            } else {
                player.sendMessage("§eName applied but file rename failed (conflict).");
            }
        });
    }
}
