package dev.genesi.match5.manager;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.board.BoardGeometry;
import dev.genesi.match5.model.Arena;
import dev.genesi.match5.model.GameSession;
import dev.genesi.match5.model.PlayerState;
import dev.genesi.match5.model.Seat;
import dev.genesi.match5.model.TileContent;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * ItemDisplays float above each floor sign (click target).
 * Signs carry text; displays show the mob egg / blank tile.
 */
public final class DisplayService {

    private final Match5Plugin plugin;

    public DisplayService(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("displays.enabled", true);
    }

    public void spawnBoard(GameSession session, Arena arena) {
        clearDisplays(session);
        if (!enabled()) {
            return;
        }

        BoardGeometry geometry = new BoardGeometry(arena);
        int size = arena.cellCount();
        ItemDisplay[] displays = new ItemDisplay[size];
        session.setDisplays(displays);

        float scale = scale();
        double yOffset = plugin.getConfig().getDouble("displays.y-offset", 0.85);
        ItemStack hidden = plugin.getItemFactory().createHiddenTile();

        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                int index = geometry.index(column, row);
                Location spawnAt = geometry.displayLocation(column, row, yOffset);
                ItemDisplay display = spawnAt.getWorld().spawn(spawnAt, ItemDisplay.class, entity ->
                        configure(entity, hidden, scale));
                displays[index] = display;
            }
        }
    }

    public void revealCell(GameSession session, Arena arena, int column, int row) {
        BoardGeometry geometry = new BoardGeometry(arena);
        int index = geometry.index(column, row);

        // Always update the sign text — this is what players actually see/click.
        var signBlock = geometry.signBlock(column, row);
        TileContent content = session.contentAt(index);
        String mobLabel = mobLabel(session, content);
        plugin.getSignService().applyReveal(signBlock, content, mobLabel);

        if (!enabled() || session.getDisplays() == null) {
            return;
        }

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

        double yOffset = plugin.getConfig().getDouble("displays.y-offset", 0.85);
        Location spawnAt = geometry.displayLocation(column, row, yOffset);
        float scale = scale();
        displays[index] = spawnAt.getWorld().spawn(spawnAt, ItemDisplay.class, entity ->
                configure(entity, item, scale));
    }

    public void clearDisplays(GameSession session) {
        ItemDisplay[] displays = session.getDisplays();
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
        session.setDisplays(null);
    }

    private String mobLabel(GameSession session, TileContent content) {
        if (content == TileContent.BLANK) {
            return "";
        }
        Seat seat = content == TileContent.A ? Seat.A : Seat.B;
        PlayerState owner = session.playerWithSeat(seat);
        return owner == null ? "Mob" : owner.getMobLabel();
    }

    private ItemStack itemFor(GameSession session, int index) {
        TileContent content = session.contentAt(index);
        if (content == TileContent.BLANK) {
            return plugin.getItemFactory().createBlankTile();
        }
        Seat seat = content == TileContent.A ? Seat.A : Seat.B;
        PlayerState owner = session.playerWithSeat(seat);
        if (owner == null) {
            return plugin.getItemFactory().createBlankTile();
        }
        return plugin.getItemFactory().createMobTile(owner.getMob());
    }

    private void configure(ItemDisplay entity, ItemStack item, float scale) {
        entity.setItemStack(item);
        entity.setBillboard(readBillboard());
        entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
        entity.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 1f, 0f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        ));
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.setShadowRadius(0f);
        entity.setShadowStrength(0f);
    }

    private float scale() {
        return (float) plugin.getConfig().getDouble("displays.scale", 0.7);
    }

    private Display.Billboard readBillboard() {
        String raw = plugin.getConfig().getString("displays.billboard", "CENTER");
        try {
            return Display.Billboard.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Display.Billboard.CENTER;
        }
    }
}
