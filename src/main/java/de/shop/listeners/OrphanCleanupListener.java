package de.shop.listeners;

import de.shop.VillagerShop;
import de.shop.data.ShopData;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

public class OrphanCleanupListener implements Listener {

    private final VillagerShop plugin;

    public OrphanCleanupListener(VillagerShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        int removed = 0;
        for (Entity e : event.getEntities()) {
            if (ShopEditorListener.isShopkeeper(e)) {
                ShopData data = plugin.getShopDataManager().getByEntity(e.getUniqueId());
                if (data == null) {
                    e.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed
                    + " orphaned shopkeepers from loading chunk.");
        }
    }
}