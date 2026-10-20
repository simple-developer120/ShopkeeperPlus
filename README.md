# ShopKeeperPlus

A highly customizable, fully-featured Shopkeeper plugin for PaperMC. It allows you to create custom entities (Villagers, Wandering Traders, Zombie Villagers) with completely custom trades, equipment, and behaviors, without relying on the vanilla Minecraft merchant limitations.

## 📌 Compatibility

*   **Platform:** PaperMC (and forks like Purpur / Pufferfish)
*   **Minecraft Version:** 1.20.5+ up to 26.3+
*   **Java Version:** Java 21 or higher (Java 25 recommended for Paper 26.3+)

---

## 🚀 Features

### 1. Custom Entity Shopkeepers
*   Spawn **Villagers**, **Wandering Traders**, or **Zombie Villagers** as shopkeepers.
*   **Baby Support:** Unlike vanilla Minecraft, babies can trade!
*   **Badge Color:** Change the Villager's badge color (Level 1-5) independently of their profession.
*   **Profession & Biome:** Change the Villager's profession and biome (Villager Type).
*   **Equipment Editor:** Give your shopkeeper custom armor, weapons, or items in their head, chest, legs, feet, main-hand, and off-hand.
*   **Custom Names:** Rename the shopkeeper using color codes (`&`). The name is hidden by default but appears on hover/gaze.
*   **Look at Player:** The shopkeeper dynamically rotates its head to look at nearby players (ignores players in Vanish mode).
*   **WorldGuard Bypass:** Spawns successfully even in regions with `mob-spawning: deny`.

### 2. Advanced Trade Editor GUI
*   Open the editor by **Shift + Right-Clicking** a shopkeeper.
*   **9 Trades per page**, up to **20 pages** (180 custom trades per shop).
*   **Vertical Layout:** Result (top), Cost 1 (middle), Cost 2 (bottom).
*   **DataComponent Protection:** Items are saved and strictly checked using Base64 byte serialization. Mismatched NBT/DataComponent items (e.g., items with different lore or enchantments) will be rejected.
*   **No Item Glitching:** Empty slots are handled perfectly without items "jumping" to other slots.

### 3. Custom Shop GUI
*   A custom-built GUI that mimics the vanilla Minecraft merchant GUI but is not bound by vanilla restrictions.
*   **Auto-Fill:** Clicking a trade automatically moves the required items from your inventory into the cost slots.
*   **Fast Trade:** Buying an item instantly refills the cost slots from your inventory, allowing for rapid consecutive purchases.
*   **Anti-Duplication:** If you don't have enough items, a gray glass pane preview is shown, and trading is blocked to prevent item duplication.
*   **Strict Trading:** Trades are cancelled if the items provided do not exactly match the required DataComponents.

### 4. Data & Protection
*   **YAML Storage:** Every shopkeeper is saved safely in `plugins/ShopKeeperPlus/shopdata/<random_name>.yml`.
*   **Auto-Respawn:** Shopkeepers automatically respawn at their saved locations after a server restart or plugin reload.
*   **Orphan Cleanup:** Ghost entities are automatically removed on chunk load.
*   **Invulnerability:** Shopkeepers are immune to all damage (fire, explosion, `/kill`, `/killall`) and cannot be pushed.
*   **Open/Closed Status:** Temporarily disable trades for specific shopkeepers using a toggle in the editor (Green/Red Banner).

---

## 🎮 Commands

| Command | Description |
| :--- | :--- |
| `/shopkeep` | Spawns a default Villager shopkeeper at your location. |
| `/shopkeep type <entity>` | Spawns a specific entity shopkeeper (e.g., `wandering_trader`, `zombie_villager`). |
| `/shopkeep delete <name>` | Deletes the shopkeeper and removes its YAML file. |
| `/shopkeep movehere <name>` | Teleports the shopkeeper to your current location. |
| `/shopkeep tp <name>` | Teleports you to the shopkeeper's location. |
| `/shopkeep rename <name> <newName>` | Renames the shopkeeper and updates its YAML filename. Supports color codes (`&`). |
| `/shopkeep changetype <name> <type>` | Changes the entity type of an existing shopkeeper and respawns it. |
| `/shopkeep reload` | respawn all shopkeepers |

---

## 🔐 Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `shopkeeperplus.admin` | `op` | Access to the `/shopkeep` command. |
| `shopkeeperplus.editor` | `op` | Access to open the Editor GUI via **Shift + Right-Click**. |

---

## 🛠️ How to Use

### For Admins (Creating a Shop)
1. Stand where you want the shopkeeper.
2. Type `/shopkeep` (or `/shopkeep type <entity>`).
3. **Shift + Right-Click** the shopkeeper to open the Editor.
4. Place your desired **Result** in the top slot.
5. Place the required **Cost 1** (middle) and **Cost 2** (bottom) items.
6. Close the GUI to save the trade.
7. Use the **Next Page** arrow to add more trades.

### For Players (Trading)
1. **Right-Click** the shopkeeper to open the Shop GUI.
2. Browse through the trades (use the arrows to change pages).
3. Click on a trade to auto-fill the cost items from your inventory.
4. Click the **Result** item to purchase. You can click rapidly to buy multiple times!

---

## 📂 Configuration
The plugin does not require manual configuration. All data is dynamically generated and managed:
*   `plugins/ShopKeeperPlus/shopdata/` - Contains the `.yml` files for each shopkeeper.
*   Trades, equipment, and items are serialized safely using Base64 to preserve all NBT/DataComponent data.
