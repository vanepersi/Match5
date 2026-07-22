package dev.genesi.match5.manager;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.board.BoardGeometry;
import dev.genesi.match5.model.Arena;
import dev.genesi.match5.model.GameSession;
import dev.genesi.match5.model.PlayerState;
import dev.genesi.match5.model.Seat;
import dev.genesi.match5.model.TileContent;
import dev.genesi.match5.util.IconDef;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Flat ItemDisplays lying on top of each board block (not upright signs).
 */
public final class DisplayService {

    private final Match5Plugin plugin;
    /** Preview displays from /m5admin buildboard (cleared on match start). */
    private final Map<String, ItemDisplay[]> previews = new HashMap<>();

    public DisplayService(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("displays.enabled", true);
    }

    /**
     * Places flat hidden-tile displays on the grid so admins can see alignment.
     * Also removes leftover upright signs from older builds.
     */
    public int buildBoard(Arena arena) {
        clearPreview(arena.getName());
        BoardGeometry geometry = new BoardGeometry(arena);
        int count = arena.cellCount();
        ItemDisplay[] displays = new ItemDisplay[count];
        ItemStack hidden = plugin.getItemFactory().createHiddenTile();
        float scale = scale();
        double yOffset = yOffset();

        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                Block block = geometry.cellBlock(column, row);
                stripUprightSign(block);
                int index = geometry.index(column, row);
                Location at = geometry.displayLocation(column, row, yOffset);
                displays[index] = spawnFlat(at, hidden, scale);
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
        ItemDisplay[] displays = new ItemDisplay[arena.cellCount()];
        session.setDisplays(displays);

        ItemStack hidden = plugin.getItemFactory().createHiddenTile();
        float scale = scale();
        double yOffset = yOffset();

        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                int index = geometry.index(column, row);
                stripUprightSign(geometry.cellBlock(column, row));
                Location at = geometry.displayLocation(column, row, yOffset);
                displays[index] = spawnFlat(at, hidden, scale);
            }
        }
    }

    public void revealCell(GameSession session, Arena arena, int column, int row) {
        if (!enabled() || session.getDisplays() == null) {
            return;
        }
        BoardGeometry geometry = new BoardGeometry(arena);
        int index = geometry.index(column, row);
        ItemDisplay[] displays = session.getDisplays();
        if (index < 0 || index >= displays.length) {
            return;
        }

        ItemStack item = itemFor(session, index);
        ItemDisplay existing = displays[index];
        if (existing != null && !existing.isDead()) {
            existing.setItemStack(item);
            return;
        }
        Location at = geometry.displayLocation(column, row, yOffset());
        displays[index] = spawnFlat(at, item, scale());
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

    private ItemStack itemFor(GameSession session, int index) {
        TileContent content = session.contentAt(index);
        if (content == TileContent.BLANK) {
            return plugin.getItemFactory().createBlankTile();
        }
        Seat seat = content == TileContent.A ? Seat.A : Seat.B;
        PlayerState owner = session.playerWithSeat(seat);
        if (owner == null || owner.getIcon() == null) {
            return plugin.getItemFactory().createBlankTile();
        }
        return plugin.getItemFactory().createIconTile(owner.getIcon());
    }

    private ItemDisplay spawnFlat(Location at, ItemStack item, float scale) {
        return at.getWorld().spawn(at, ItemDisplay.class, entity -> configureFlat(entity, item, scale));
    }

    private void configureFlat(ItemDisplay entity, ItemStack item, float scale) {
        entity.setItemStack(item);
        entity.setBillboard(Display.Billboard.FIXED);
        entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        // Rotate flat onto the block top (90° around X).
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

    private void removeAll(ItemDisplay[] displays) {
        if (displays == null) {
            return;
        }
        for (int i = 0; i < displays.length; i++) {
            ItemDisplay display = displays[i];
            if (display != null && !display.isDead()) {
                display.remove();
            }
            displays[i] = null;
        }
    }

    private float scale() {
        return (float) plugin.getConfig().getDouble("displays.scale", 0.9);
    }

    private double yOffset() {
        // Sit just above the block top face
        return plugin.getConfig().getDouble("displays.y-offset", 1.02);
    }
}
