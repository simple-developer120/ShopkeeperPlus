package de.shop.listeners;

import de.shop.VillagerShop;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityProtectionListener implements Listener {

    private final VillagerShop plugin;

    public EntityProtectionListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    // ✅ ۱. محافظت از آسیب دیدن
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isShopkeeper(event.getEntity())) return;
        event.setCancelled(true);
        if (event.getEntity() instanceof LivingEntity living) {
            living.setHealth(living.getMaxHealth());
        }
    }

    // ✅ ۲. محافظت از مرگ
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isShopkeeper(event.getEntity())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    // ✅ ۳. دور زدن WorldGuard موقع اسپاون شدن
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // اگه دلیل اسپاون CUSTOM باشه، یعنی خود پلاگین ما داره اسپاونش میکنه
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            // به WorldGuard میگیم این یکی رو کنسل نکنه
            event.setCancelled(false);
        }
    }

    private boolean isShopkeeper(Entity entity) {
        return ShopEditorListener.isShopkeeper(entity);
    }
}