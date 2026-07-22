package dev.genesi.match5.util;

/**
 * An ItemsAdder (or fallback) icon used as a Match 5 tile / player target.
 */
public record IconDef(String itemsAdderId, String label, org.bukkit.Material fallback) {

    public IconDef {
        if (label == null || label.isBlank()) {
            label = "Icon";
        }
        if (fallback == null) {
            fallback = org.bukkit.Material.PAPER;
        }
        if (itemsAdderId != null && itemsAdderId.isBlank()) {
            itemsAdderId = null;
        }
    }
}
