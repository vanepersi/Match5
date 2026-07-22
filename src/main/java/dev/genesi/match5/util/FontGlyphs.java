package dev.genesi.match5.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Builds Adventure text components using the ItemsAdder {@code minigame1} font.
 */
public final class FontGlyphs {

    private FontGlyphs() {
    }

    public static Key fontKey(FileConfiguration config) {
        String raw = config.getString("font", "minecraft:minigame1");
        Key key = Key.key(raw == null || raw.isBlank() ? "minecraft:minigame1" : raw.trim());
        return key;
    }

    public static String hiddenChar(FileConfiguration config) {
        String ch = config.getString("glyphs.hidden", "ꀆ");
        return ch == null || ch.isBlank() ? "ꀆ" : ch;
    }

    public static String blankChar(FileConfiguration config) {
        String ch = config.getString("glyphs.blank", "ꀆ");
        return ch == null || ch.isBlank() ? "ꀆ" : ch;
    }

    public static Component glyph(FileConfiguration config, String character) {
        String text = character == null || character.isBlank() ? hiddenChar(config) : character;
        return Component.text(text).font(fontKey(config));
    }
}
