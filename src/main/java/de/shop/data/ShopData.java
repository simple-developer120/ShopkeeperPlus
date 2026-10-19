package de.shop.data;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class ShopData {
    private String fileName;
    private String customName;
    private UUID entityUuid;
    private EntityType entityType;
    private Location location;
    private List<MerchantRecipe> recipes = new ArrayList<>();
    private int currentPage = 0;
    private boolean tradesOpen = true;
    
    private String profession = "WEAPONSMITH";
    private boolean isBaby = false;
    private String villagerType = "PLAINS";
    private int villagerLevel = 1;

    private Map<org.bukkit.inventory.EquipmentSlot, ItemStack> equipment = new java.util.EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);

    public int getVillagerLevel() { return villagerLevel; }
    public void setVillagerLevel(int villagerLevel) { this.villagerLevel = villagerLevel; }

    public String getVillagerType() { return villagerType; }
    public void setVillagerType(String villagerType) { this.villagerType = villagerType; }
    
    public Map<org.bukkit.inventory.EquipmentSlot, ItemStack> getEquipment() {
        return equipment;
    }
    
    public ShopData(String fileName) { this.fileName = fileName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public UUID getEntityUuid() { return entityUuid; }
    public void setEntityUuid(UUID entityUuid) { this.entityUuid = entityUuid; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public List<MerchantRecipe> getRecipes() { return recipes; }
    public void setRecipes(List<MerchantRecipe> recipes) { this.recipes = recipes; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public boolean isTradesOpen() { return tradesOpen; }
    public void setTradesOpen(boolean tradesOpen) { this.tradesOpen = tradesOpen; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public boolean isBaby() { return isBaby; }
    public void setBaby(boolean baby) { isBaby = baby; }
}
