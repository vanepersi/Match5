package dev.genesi.match5.board;

import dev.genesi.match5.model.Arena;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * Flat tabletop grid on existing blocks.
 * <p>
 * Origin = near-left corner from the player's view.<br>
 * Columns grow to the player's right.<br>
 * Rows grow away from the player (into the board).<br>
 * {@code facing} = direction the near edge faces toward players.
 */
public final class BoardGeometry {

    private final World world;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final BlockFace facing;
    private final int columns;
    private final int rows;
    private final int cellSize;
    private final int gap;
    private final Vector right;
    private final Vector forward;

    public BoardGeometry(Arena arena) {
        Location origin = arena.getOrigin();
        if (origin == null || origin.getWorld() == null) {
            throw new IllegalArgumentException("arena origin missing");
        }
        this.world = origin.getWorld();
        this.originX = origin.getBlockX();
        this.originY = origin.getBlockY();
        this.originZ = origin.getBlockZ();
        this.facing = arena.getFacing();
        this.columns = arena.getColumns();
        this.rows = arena.getRows();
        this.cellSize = arena.getCellSize();
        this.gap = arena.getGap();
        this.right = rightVector(facing);
        // Into the board = away from players = opposite of facing-toward-players
        this.forward = facing.getOppositeFace().getDirection();
    }

    /**
     * Column step toward the viewer's right when looking at the board.
     */
    public static Vector rightVector(BlockFace facingTowardPlayers) {
        return switch (facingTowardPlayers) {
            case SOUTH -> new Vector(1, 0, 0);   // look north → right = east
            case NORTH -> new Vector(-1, 0, 0);  // look south → right = west
            case WEST -> new Vector(0, 0, 1);    // look east → right = south
            case EAST -> new Vector(0, 0, -1);   // look west → right = north
            default -> new Vector(1, 0, 0);
        };
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public World getWorld() {
        return world;
    }

    public BlockFace getFacing() {
        return facing;
    }

    public int index(int column, int row) {
        return row * columns + column;
    }

    public Location cellOrigin(int column, int row) {
        int step = cellSize + gap;
        int x = originX
                + (int) right.getX() * column * step
                + (int) Math.round(forward.getX()) * row * step;
        int z = originZ
                + (int) right.getZ() * column * step
                + (int) Math.round(forward.getZ()) * row * step;
        return new Location(world, x, originY, z);
    }

    /** Center of the top face — flat ItemDisplays sit here. */
    public Location displayLocation(int column, int row, double yOffset) {
        Location base = cellOrigin(column, row);
        double half = (cellSize - 1) / 2.0;
        return new Location(
                world,
                base.getX() + (int) right.getX() * half + (int) Math.round(forward.getX()) * half + 0.5,
                base.getY() + yOffset,
                base.getZ() + (int) right.getZ() * half + (int) Math.round(forward.getZ()) * half + 0.5
        );
    }

    public Block cellBlock(int column, int row) {
        return cellOrigin(column, row).getBlock();
    }

    public int[] cellAt(Block block) {
        if (block == null || !block.getWorld().equals(world)) {
            return null;
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (contains(block, column, row)) {
                    return new int[]{column, row};
                }
            }
        }
        return null;
    }

    public boolean contains(Block block, int column, int row) {
        Location base = cellOrigin(column, row);
        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();
        for (int dr = 0; dr < cellSize; dr++) {
            for (int dc = 0; dc < cellSize; dc++) {
                int x = bx + (int) right.getX() * dc + (int) Math.round(forward.getX()) * dr;
                int z = bz + (int) right.getZ() * dc + (int) Math.round(forward.getZ()) * dr;
                // Table block (Y) or the floor sign on top (Y+1)
                if (block.getX() == x && block.getZ() == z
                        && (block.getY() == by || block.getY() == by + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOnBoard(Block block) {
        return cellAt(block) != null;
    }
}
