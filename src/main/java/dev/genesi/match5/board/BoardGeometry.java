package dev.genesi.match5.board;

import dev.genesi.match5.model.Arena;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * Flat tabletop grid: columns go right from the player view, rows go away from players.
 * {@code facing} is the direction the near edge faces toward players.
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
        this.forward = facing.getDirection().multiply(-1).normalize();
    }

    public static Vector rightVector(BlockFace facing) {
        return switch (facing) {
            case NORTH -> new Vector(1, 0, 0);
            case SOUTH -> new Vector(-1, 0, 0);
            case EAST -> new Vector(0, 0, 1);
            case WEST -> new Vector(0, 0, -1);
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

    /** Southwest-ish corner block of a cell. */
    public Location cellOrigin(int column, int row) {
        int step = cellSize + gap;
        int x = originX
                + (int) right.getX() * column * step
                + (int) Math.round(forward.getX()) * row * step;
        int y = originY;
        int z = originZ
                + (int) right.getZ() * column * step
                + (int) Math.round(forward.getZ()) * row * step;
        return new Location(world, x, y, z);
    }

    public Location displayLocation(int column, int row, double yOffset) {
        Location base = cellOrigin(column, row);
        double half = cellSize / 2.0;
        return new Location(
                world,
                base.getX() + (int) right.getX() * half + (int) Math.round(forward.getX()) * half + 0.5,
                base.getY() + yOffset,
                base.getZ() + (int) right.getZ() * half + (int) Math.round(forward.getZ()) * half + 0.5,
                yawFrom(facing),
                0f
        );
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
        for (int dy = 0; dy < 1; dy++) {
            for (int dr = 0; dr < cellSize; dr++) {
                for (int dc = 0; dc < cellSize; dc++) {
                    int x = bx + (int) right.getX() * dc + (int) Math.round(forward.getX()) * dr;
                    int z = bz + (int) right.getZ() * dc + (int) Math.round(forward.getZ()) * dr;
                    if (block.getX() == x && block.getY() == by + dy && block.getZ() == z) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isOnBoard(Block block) {
        return cellAt(block) != null;
    }

    private static float yawFrom(BlockFace face) {
        return switch (face) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
    }
}
