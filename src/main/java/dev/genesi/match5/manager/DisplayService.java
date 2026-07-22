package dev.genesi.match5.manager;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.board.BoardGeometry;
import dev.genesi.match5.model.Arena;
import dev.genesi.match5.model.GameSession;
import dev.genesi.match5.model.PlayerState;
import dev.genesi.match5.model.Seat;
import dev.genesi.match5.model.TileContent;
import dev.genesi.match5.util.FontGlyphs;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Flat TextDisplays on each board block using the ItemsAdder {@code minigame1} font.
 * Hidden = dark hole ꀆ; revealed icons = yellow ꀈ / red ꀉ / green ꀊ.
 */
public final class DisplayService {

    private final Match5Plugin plugin;
    private final Map<String, TextDisplay[]> previews = new HashMap<>();

    public DisplayService(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("displays.enabled", true);
    }

    public int buildBoard(Arena arena) {
        clearPreview(arena.getName());
        BoardGeometry geometry = new BoardGeometry(arena);
        int count = arena.cellCount();
        TextDisplay[] displays = new TextDisplay[count];
        Component hidden = FontGlyphs.glyph(plugin.getConfig(), FontGlyphs.hiddenChar(plugin.getConfig()));

        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                stripUprightSign(geometry.cellBlock(column, row));
                int index = geometry.index(column, row);
                Location at = geometry.displayLocation(column, row, yOffset());
                displays[index] = spawnFlat(at, hidden);
            }
        }
        previews.put(arena.getName(), displays);
        return count;
    }

    public void spawnBoard(GameSession session, Arena arena) {
        clearPreview(arena.getName());
        clearDisplays(session);
        if (!enabled()) {
            return;
        }

        BoardGeometry geometry = new BoardGeometry(arena);
        TextDisplay[] displays = new TextDisplay[arena.cellCount()];
        session.setDisplays(displays);
        Component hidden = FontGlyphs.glyph(plugin.getConfig(), FontGlyphs.hiddenChar(plugin.getConfig()));

        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                int index = geometry.index(column, row);
                stripUprightSign(geometry.cellBlock(column, row));
                Location at = geometry.displayLocation(column, row, yOffset());
                displays[index] = spawnFlat(at, hidden);
            }
        }
    }

    public void revealCell(GameSession session, Arena arena, int column, int row) {
        if (!enabled() || session.getDisplays() == null) {
            return;
        }
        BoardGeometry geometry = new BoardGeometry(arena);
        int index = geometry.index(column, row);
        TextDisplay[] displays = session.getDisplays();
        if (index < 0 || index >= displays.length) {
            return;
        }

        Component text = textFor(session, index);
        TextDisplay existing = displays[index];
        if (existing != null && !existing.isDead()) {
            existing.text(text);
            return;
        }
        Location at = geometry.displayLocation(column, row, yOffset());
        displays[index] = spawnFlat(at, text);
    }

    public void clearDisplays(GameSession session) {
        removeAll(session.getDisplays());
        session.setDisplays(null);
    }

    public void clearPreview(String arenaName) {
        removeAll(previews.remove(arenaName.toLowerCase()));
    }

    public void clearAllPreviews() {
        for (String key : previews.keySet().toArray(String[]::new)) {
            clearPreview(key);
        }
    }

    private Component textFor(GameSession session, int index) {
        TileContent content = session.contentAt(index);
        if (content == TileContent.BLANK) {
            return FontGlyphs.glyph(plugin.getConfig(), FontGlyphs.blankChar(plugin.getConfig()));
        }
        Seat seat = content == TileContent.A ? Seat.A : Seat.B;
        PlayerState owner = session.playerWithSeat(seat);
        if (owner == null || owner.getIcon() == null) {
            return FontGlyphs.glyph(plugin.getConfig(), FontGlyphs.blankChar(plugin.getConfig()));
        }
        return FontGlyphs.glyph(plugin.getConfig(), owner.getIcon().character());
    }

    private TextDisplay spawnFlat(Location at, Component text) {
        return at.getWorld().spawn(at, TextDisplay.class, entity -> configureFlat(entity, text));
    }

    private void configureFlat(TextDisplay entity, Component text) {
        entity.text(text);
        entity.setBillboard(Display.Billboard.FIXED);
        entity.setAlignment(TextDisplay.TextAlignment.CENTER);
        entity.setSeeThrough(false);
        entity.setDefaultBackground(false);
        entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        entity.setShadowed(false);
        entity.setLineWidth(64);
        float scale = scale();
        // Flat on the block top
        entity.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f((float) (Math.PI / 2.0), 1f, 0f, 0f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        ));
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.setShadowRadius(0f);
        entity.setShadowStrength(0f);
    }

    private void stripUprightSign(Block block) {
        if (block == null) {
            return;
        }
        String name = block.getType().name();
        if (name.endsWith("_SIGN") && !name.contains("WALL") && !name.contains("HANGING")) {
            block.setType(Material.AIR, false);
        }
    }

    private void removeAll(TextDisplay[] displays) {
        if (displays == null) {
            return;
        }
        for (int i = 0; i < displays.length; i++) {
            TextDisplay display = displays[i];
            if (display != null && !display.isDead()) {
                display.remove();
            }
            displays[i] = null;
        }
    }

    private float scale() {
        return (float) plugin.getConfig().getDouble("displays.scale", 1.6);
    }

    private double yOffset() {
        return plugin.getConfig().getDouble("displays.y-offset", 1.02);
    }
}
