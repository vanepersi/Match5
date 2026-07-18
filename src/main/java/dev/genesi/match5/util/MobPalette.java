package dev.genesi.match5.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Picks two distinct mob spawn-eggs from config for a match.
 */
public final class MobPalette {

    public record MobChoice(Material material, String label) {
    }

    private MobPalette() {
    }

    public static List<MobChoice> load(FileConfiguration config) {
        List<MobChoice> choices = new ArrayList<>();
        List<?> raw = config.getMapList("mobs");
        if (raw != null) {
            for (Object entry : raw) {
                if (!(entry instanceof java.util.Map<?, ?> map)) {
                    continue;
                }
                Object materialObj = map.get("material");
                Object labelObj = map.get("label");
                if (materialObj == null) {
                    continue;
                }
                Material material = Material.matchMaterial(String.valueOf(materialObj).toUpperCase(Locale.ROOT));
                if (material == null || !material.name().endsWith("_SPAWN_EGG")) {
                    continue;
                }
                String label = labelObj == null || String.valueOf(labelObj).isBlank()
                        ? pretty(material)
                        : String.valueOf(labelObj);
                choices.add(new MobChoice(material, label));
            }
        }
        if (choices.isEmpty()) {
            choices.add(new MobChoice(Material.PIG_SPAWN_EGG, "Pig"));
            choices.add(new MobChoice(Material.COW_SPAWN_EGG, "Cow"));
            choices.add(new MobChoice(Material.SHEEP_SPAWN_EGG, "Sheep"));
            choices.add(new MobChoice(Material.CHICKEN_SPAWN_EGG, "Chicken"));
            choices.add(new MobChoice(Material.WOLF_SPAWN_EGG, "Wolf"));
            choices.add(new MobChoice(Material.CAT_SPAWN_EGG, "Cat"));
        }
        return choices;
    }

    public static MobChoice[] pickTwo(List<MobChoice> palette) {
        if (palette == null || palette.isEmpty()) {
            return new MobChoice[]{
                    new MobChoice(Material.PIG_SPAWN_EGG, "Pig"),
                    new MobChoice(Material.COW_SPAWN_EGG, "Cow")
            };
        }
        if (palette.size() == 1) {
            MobChoice only = palette.getFirst();
            return new MobChoice[]{only, only};
        }
        int first = ThreadLocalRandom.current().nextInt(palette.size());
        int second = ThreadLocalRandom.current().nextInt(palette.size() - 1);
        if (second >= first) {
            second++;
        }
        return new MobChoice[]{palette.get(first), palette.get(second)};
    }

    private static String pretty(Material material) {
        String name = material.name().replace("_SPAWN_EGG", "").toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String part : name.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
