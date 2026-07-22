package dev.genesi.match5.util;

import dev.genesi.games.itemsadder.ItemsAdderItems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Builds ItemStacks for hidden / blank / icon tiles.
 */
public final class ItemFactory {

    private final JavaPlugin plugin;

    public ItemFactory(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createHiddenTile() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("displays.hidden");
        String ia = section == null ? "" : section.getString("itemsadder-id", "");
        if (ia != null && !ia.isBlank()) {
            ItemStack stack = ItemsAdderItems.getItem(plugin, ia);
            if (stack != null) {
                return stack;
            }
        }
        Material material = Material.PAPER;
        if (section != null) {
            Material matched = Material.matchMaterial(section.getString("material", "PAPER"));
            if (matched != null && matched != Material.MAP && matched != Material.FILLED_MAP) {
                material = matched;
            }
        }
        return new ItemStack(material);
    }

    public ItemStack createBlankTile() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("displays.blank");
        String ia = section == null ? "" : section.getString("itemsadder-id", "");
        if (ia != null && !ia.isBlank()) {
            ItemStack stack = ItemsAdderItems.getItem(plugin, ia);
            if (stack != null) {
                return stack;
            }
        }
        Material material = Material.LIGHT_GRAY_CONCRETE;
        if (section != null) {
            Material matched = Material.matchMaterial(section.getString("material", "LIGHT_GRAY_CONCRETE"));
            if (matched != null) {
                material = matched;
            }
        }
        return new ItemStack(material);
    }

    public ItemStack createIconTile(IconDef icon) {
        if (icon == null) {
            return createBlankTile();
        }
        if (icon.itemsAdderId() != null) {
            ItemStack stack = ItemsAdderItems.getItem(plugin, icon.itemsAdderId());
            if (stack != null) {
                return stack;
            }
        }
        return new ItemStack(icon.fallback());
    }
}
