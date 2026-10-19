package de.shop;

import de.shop.commands.ShopKeepCommand;
import de.shop.data.ShopDataManager;
import de.shop.listeners.*;
import de.shop.util.PaginationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class VillagerShop extends JavaPlugin {

    private PaginationManager paginationManager;
    private ShopDataManager shopDataManager;
    private TeleportListener teleportListener;
    private ShopEditorListener shopEditorListener;
    private EditorGUIListener editorGUIListener;
    private EquipmentGUIListener equipmentGUIListener;
    private BukkitTask respawnTask;
    private BukkitTask lookTask;
    private boolean respawnRunning = false;

    @Override
    public void onEnable() {
        this.paginationManager = new PaginationManager();
        this.shopDataManager = new ShopDataManager(this);

        this.teleportListener = new TeleportListener(this);
        this.shopEditorListener = new ShopEditorListener(this);
        this.editorGUIListener = new EditorGUIListener(this);
        this.equipmentGUIListener = new EquipmentGUIListener(this);
        
        ShopKeepCommand cmd = new ShopKeepCommand(this);
        if (getCommand("shopkeep") != null) {
            getCommand("shopkeep").setExecutor(cmd);
            getCommand("shopkeep").setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(equipmentGUIListener, this);
        getServer().getPluginManager().registerEvents(shopEditorListener, this);
        getServer().getPluginManager().registerEvents(editorGUIListener, this);
        getServer().getPluginManager().registerEvents(new ShopkeeperGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        getServer().getPluginManager().registerEvents(new OrphanCleanupListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);

        getServer().getScheduler().runTaskLater(this, this::runRespawn, 100L);
        respawnTask = getServer().getScheduler().runTaskTimer(this, this::runRespawn, 600L, 600L);

        lookTask = getServer().getScheduler().runTaskTimer(this, this::lookAtNearestPlayer, 5L, 5L);

        getLogger().info("VillagerShop enabled!");
    }

    private void lookAtNearestPlayer() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getPlayers().isEmpty()) continue;

            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Mob mob)) continue; 
                if (!ShopEditorListener.isShopkeeper(mob)) continue;

                Player nearest = null;
                double nearestDist = 64.0; 

                Location vLoc = mob.getLocation();
                for (Player p : world.getPlayers()) {
                    if (p.getWorld() != vLoc.getWorld()) continue;
                    if (p.isInvisible()) continue;
                    double dist = p.getLocation().distanceSquared(vLoc);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                    }
                }

                if (nearest != null) {
                    Location pLoc = nearest.getLocation();
                    double dx = pLoc.getX() - vLoc.getX();
                    double dz = pLoc.getZ() - vLoc.getZ();
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    mob.setRotation(yaw, 0);
                }
            }
        }
    }

    private void runRespawn() {
        if (respawnRunning) return;
        respawnRunning = true;
        try { shopDataManager.respawnMissing(); }
        catch (Throwable t) { getLogger().severe("Respawn error: " + t.getMessage()); }
        finally { respawnRunning = false; }
    }

    @Override
    public void onDisable() {
        if (respawnTask != null) respawnTask.cancel();
        if (lookTask != null) lookTask.cancel();

        if (shopDataManager != null)
            for (var data : shopDataManager.getAll()) shopDataManager.save(data);

        for (World world : Bukkit.getWorlds())
            for (Entity e : world.getEntities())
                if (ShopEditorListener.isShopkeeper(e)) e.remove();

        getLogger().info("VillagerShop disabled.");
    }

    public EquipmentGUIListener getEquipmentGUIListener() { return equipmentGUIListener; }
    public PaginationManager getPaginationManager() { return paginationManager; }
    public ShopDataManager getShopDataManager() { return shopDataManager; }
    public TeleportListener getTeleportListener() { return teleportListener; }
    public ShopEditorListener getShopEditorListener() { return shopEditorListener; }
    public EditorGUIListener getEditorGUIListener() { return editorGUIListener; }
          }
