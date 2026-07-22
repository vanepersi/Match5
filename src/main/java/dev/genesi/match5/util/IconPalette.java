package dev.genesi.match5.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loads the Match 5 icon pool and picks two distinct icons for a match.
 */
public final class IconPalette {

    private IconPalette() {
    }

    public static List<IconDef> load(FileConfiguration config) {
        List<IconDef> icons = new ArrayList<>();
        List<?> raw = config.getMapList("icons");
        if (raw != null) {
            for (Object entry : raw) {
                if (!(entry instanceof java.util.Map<?, ?> map)) {
                    continue;
                }
                String ia = stringVal(map.get("itemsadder-id"));
                String label = stringVal(map.get("label"));
                Material fallback = Material.matchMaterial(stringVal(map.get("fallback")).toUpperCase(Locale.ROOT));
                if (fallback == null) {
                    fallback = Material.PAPER;
                }
                if ((ia == null || ia.isBlank()) && fallback == Material.PAPER && (label == null || label.isBlank())) {
                    continue;
                }
                icons.add(new IconDef(ia, label == null || label.isBlank() ? pretty(ia, fallback) : label, fallback));
            }
        }
        if (icons.isEmpty()) {
            icons.addAll(defaults());
        }
        return icons;
    }

    public static IconDef[] pickTwo(List<IconDef> palette) {
        if (palette == null || palette.isEmpty()) {
            List<IconDef> d = defaults();
            return new IconDef[]{d.get(0), d.get(1)};
        }
        if (palette.size() == 1) {
            return new IconDef[]{palette.getFirst(), palette.getFirst()};
        }
        int first = ThreadLocalRandom.current().nextInt(palette.size());
        int second = ThreadLocalRandom.current().nextInt(palette.size() - 1);
        if (second >= first) {
            second++;
        }
        return new IconDef[]{palette.get(first), palette.get(second)};
    }

    private static List<IconDef> defaults() {
        return List.of(
                new IconDef("genesicore:asset1734", "Gem", Material.PAPER),
                new IconDef("genesicore:asset1784", "Market Gem", Material.PAPER),
                new IconDef("genesicore:asset1964", "Blue Coin", Material.PAPER),
                new IconDef("genesicore:asset1716", "Fall Token", Material.PAPER),
                new IconDef("genesicore:asset1908", "Currency", Material.PAPER)
        );
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String pretty(String ia, Material fallback) {
        if (ia != null && ia.contains(":")) {
            return ia.substring(ia.indexOf(':') + 1);
        }
        return fallback.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
