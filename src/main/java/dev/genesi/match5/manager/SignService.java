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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

/**
 * Floor signs on the tabletop — click targets with {@code minigame1} font glyphs.
 * Signs sit on top of the board blocks (Y+1), so the colored table stays intact.
 */
public final class SignService {

    private final Match5Plugin plugin;

    public SignService(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSign(Block block) {
        if (block == null) {
            return false;
        }
        String name = block.getType().name();
        return name.endsWith("_SIGN") || name.endsWith("_HANGING_SIGN");
    }

    /**
     * Clears leftover ItemDisplay/TextDisplay junk, then places a full sign grid.
     * @return number of signs placed
     */
    public int buildBoard(Arena arena) {
        clearFloatingDisplays(arena);
        BoardGeometry geometry = new BoardGeometry(arena);
        Material material = signMaterial();
        int placed = 0;
        Component hidden = FontGlyphs.glyph(plugin.getConfig(), FontGlyphs.hiddenChar(plugin.getConfig()));

        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                Block table = geometry.cellBlock(column, row);
                Block signBlock = table.getRelative(BlockFace.UP);
                placeFloorSign(signBlock, arena.getFacing(), material);
                writeGlyph(signBlock, hidden);
                placed++;
            }
        }
        return placed;
    }

    public void resetBoard(Arena arena) {
        BoardGeometry geometry = new BoardGeometry(arena);
        Component hidden = FontGlyphs.glyph(plugin.getConfig(), FontGlyphs.hiddenChar(plugin.getConfig()));
        Material material = signMaterial();
        for (int row = 0; row < geometry.getRows(); row++) {
            for (int column = 0; column < geometry.getColumns(); column++) {
                Block signBlock = geometry.cellBlock(column, row).getRelative(BlockFace.UP);
                if (!isSign(signBlock)) {
                    placeFloorSign(signBlock, arena.getFacing(), material);
                }
                writeGlyph(signBlock, hidden);
            }
        }
    }

    public void revealCell(GameSession session, Arena arena, int column, int row) {
        BoardGeometry geometry = new BoardGeometry(arena);
        Block signBlock = geometry.cellBlock(column, row).getRelative(BlockFace.UP);
        if (!isSign(signBlock)) {
            placeFloorSign(signBlock, arena.getFacing(), signMaterial());
        }
        writeGlyph(signBlock, glyphFor(session, geometry.index(column, row)));
    }

    private Component glyphFor(GameSession session, int index) {
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

    private void placeFloorSign(Block block, BlockFace boardFacing, Material material) {
        block.setType(material, false);
        if (block.getBlockData() instanceof Rotatable rotatable) {
            // Sign front faces players
            rotatable.setRotation(boardFacing);
            block.setBlockData(rotatable, false);
        }
    }

    private void writeGlyph(Block block, Component glyph) {
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }
        var side = sign.getSide(Side.FRONT);
        side.line(0, Component.empty());
        side.line(1, glyph);
        side.line(2, Component.empty());
        side.line(3, Component.empty());
        sign.setWaxed(true);
        sign.update(true, false);
    }

    private Material signMaterial() {
        String raw = plugin.getConfig().getString("signs.material", "OAK_SIGN");
        Material matched = Material.matchMaterial(raw == null ? "OAK_SIGN" : raw);
        if (matched != null && matched.name().endsWith("_SIGN")
                && !matched.name().contains("WALL")
                && !matched.name().contains("HANGING")) {
            return matched;
        }
        return Material.OAK_SIGN;
    }

    /** Remove leftover paper ItemDisplays / TextDisplays from older builds. */
    public void clearFloatingDisplays(Arena arena) {
        Location origin = arena.getOrigin();
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        World world = origin.getWorld();
        BoardGeometry geometry = new BoardGeometry(arena);
        Location far = geometry.cellOrigin(arena.getColumns() - 1, arena.getRows() - 1);
        double minX = Math.min(origin.getX(), far.getX()) - 1;
        double maxX = Math.max(origin.getX(), far.getX()) + 2;
        double minZ = Math.min(origin.getZ(), far.getZ()) - 1;
        double maxZ = Math.max(origin.getZ(), far.getZ()) + 2;
        double minY = origin.getY() - 1;
        double maxY = origin.getY() + 3;

        for (Entity entity : world.getNearbyEntities(
                origin.clone().add(arena.getColumns() / 2.0, 1, arena.getRows() / 2.0),
                arena.getColumns() + 2,
                4,
                arena.getRows() + 2)) {
            if (!(entity instanceof ItemDisplay) && !(entity instanceof TextDisplay)) {
                continue;
            }
            Location loc = entity.getLocation();
            if (loc.getX() >= minX && loc.getX() <= maxX
                    && loc.getY() >= minY && loc.getY() <= maxY
                    && loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                entity.remove();
            }
        }
    }
}
