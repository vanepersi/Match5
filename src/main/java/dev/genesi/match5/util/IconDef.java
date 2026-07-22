package dev.genesi.match5.util;

/**
 * A Match 5 icon rendered via the ItemsAdder {@code minigame1} bitmap font.
 */
public record IconDef(String character, String label) {

    public IconDef {
        if (character == null || character.isBlank()) {
            character = "?";
        }
        if (label == null || label.isBlank()) {
            label = "Icon";
        }
    }

    /** Character + label for sidebar / messages, e.g. {@code ꀈ Yellow}. */
    public String display() {
        return character + " " + label;
    }
}
