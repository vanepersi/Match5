package dev.genesi.match5.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class ItemFactory {

    private final JavaPlugin plugin;

    public ItemFactory(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createHiddenTile() {
        return createConfiguredItem("displays.hidden", Material.MAP);
    }

    public ItemStack createBlankTile() {
        return createConfiguredItem("displays.blank", Material.LIGHT_GRAY_CONCRETE);
    }

    public ItemStack createMobTile(Material mob) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("displays.mob");
        Material fallback = mob == null ? Material.PIG_SPAWN_EGG : mob;
        if (section == null) {
            return new ItemStack(fallback);
        }

        String itemsAdderId = section.getString("itemsadder-id", "");
        if (itemsAdderId != null && !itemsAdderId.isBlank()) {
            ItemStack ia = tryItemsAdder(itemsAdderId);
            if (ia != null) {
                applyDisplay(ia, section);
                return ia;
            }
        }

        // Prefer the actual spawn-egg material so each mob reads clearly without custom models.
        boolean useEgg = section.getBoolean("use-spawn-egg", true);
        Material material = useEgg ? fallback : Material.matchMaterial(section.getString("material", fallback.name()));
        if (material == null) {
            material = fallback;
        }
        ItemStack stack = new ItemStack(material);
        applyDisplay(stack, section);
        applyModel(stack, section);
        return stack;
    }

    private ItemStack createConfiguredItem(String sectionName, Material fallback) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionName);
        if (section == null) {
            return new ItemStack(fallback);
        }

        String itemsAdderId = section.getString("itemsadder-id", "");
        if (itemsAdderId != null && !itemsAdderId.isBlank()) {
            ItemStack ia = tryItemsAdder(itemsAdderId);
            if (ia != null) {
                applyDisplay(ia, section);
                return ia;
            }
        }

        Material material = Material.matchMaterial(section.getString("material", fallback.name()));
        if (material == null) {
            material = fallback;
        }
        ItemStack stack = new ItemStack(material);
        applyDisplay(stack, section);
        applyModel(stack, section);
        return stack;
    }

    private void applyDisplay(ItemStack stack, ConfigurationSection section) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        String name = section.getString("display-name");
        if (name != null && !name.isBlank()) {
            meta.displayName(legacy(name));
        }
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(this::legacy).toList());
        }
        stack.setItemMeta(meta);
    }

    private void applyModel(ItemStack stack, ConfigurationSection section) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String itemModel = section.getString("item-model", "");
        if (itemModel != null && !itemModel.isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(itemModel.toLowerCase(Locale.ROOT));
            if (key != null) {
                trySetItemModel(meta, key);
            }
        }

        int cmd = section.getInt("custom-model-data", 0);
        if (cmd > 0) {
            meta.setCustomModelData(cmd);
        }
        stack.setItemMeta(meta);
    }

    private void trySetItemModel(ItemMeta meta, NamespacedKey key) {
        try {
            Method method = meta.getClass().getMethod("setItemModel", NamespacedKey.class);
            method.invoke(meta, key);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private ItemStack tryItemsAdder(String id) {
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return null;
        }
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstance = customStack.getMethod("getInstance", String.class);
            Object stackWrapper = getInstance.invoke(null, id);
            if (stackWrapper == null) {
                return null;
            }
            Method getItemStack = stackWrapper.getClass().getMethod("getItemStack");
            Object result = getItemStack.invoke(stackWrapper);
            if (result instanceof ItemStack itemStack) {
                return itemStack.clone();
            }
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load ItemsAdder item '" + id + "'", e);
        }
        return null;
    }

    private Component legacy(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input == null ? "" : input);
    }
}
