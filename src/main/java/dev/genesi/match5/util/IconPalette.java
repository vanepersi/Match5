package dev.genesi.match5.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loads Match 5 icons from {@code minigame1} font characters in config.
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
                Object charObj = map.get("character");
                Object labelObj = map.get("label");
                if (charObj == null) {
                    continue;
                }
                String character = String.valueOf(charObj).trim();
                if (character.isEmpty()) {
                    continue;
                }
                String label = labelObj == null ? character : String.valueOf(labelObj).trim();
                icons.add(new IconDef(character, label.isEmpty() ? character : label));
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

    /** Yellow / red-orange / green from genesicore {@code minigame1} font. */
    private static List<IconDef> defaults() {
        return List.of(
                new IconDef("ꀈ", "Yellow"),
                new IconDef("ꀉ", "Red"),
                new IconDef("ꀊ", "Green")
        );
    }
}
