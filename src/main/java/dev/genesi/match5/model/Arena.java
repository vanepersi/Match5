package dev.genesi.match5.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Tabletop Match 5 arena.
 * Board is a flat grid from {@code origin} across the floor (not a wall).
 * {@code facing} is the direction the near edge faces toward players.
 */
public final class Arena {

    public static final int DEFAULT_COLUMNS = 6;
    public static final int DEFAULT_ROWS = 6;

    private final String name;
    private Location lobby;
    private Location origin;
    private BlockFace facing = BlockFace.NORTH;
    private Location seatAJoin;
    private Location seatBJoin;
    private int columns = DEFAULT_COLUMNS;
    private int rows = DEFAULT_ROWS;
    private int cellSize = 1;
    private int gap = 0;
    private Double entryFeeOverride;

    public Arena(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    public Location getLobby() {
        return cloneLocation(lobby);
    }

    public void setLobby(Location lobby) {
        this.lobby = cloneLocation(lobby);
    }

    public Location getOrigin() {
        return cloneLocation(origin);
    }

    public void setOrigin(Location origin) {
        this.origin = cloneLocation(origin);
    }

    public BlockFace getFacing() {
        return facing;
    }

    public void setFacing(BlockFace facing) {
        this.facing = facing == null ? BlockFace.NORTH : facing;
    }

    public Location getSeatAJoin() {
        return cloneLocation(seatAJoin);
    }

    public void setSeatAJoin(Location seatAJoin) {
        this.seatAJoin = cloneLocation(seatAJoin);
    }

    public Location getSeatBJoin() {
        return cloneLocation(seatBJoin);
    }

    public void setSeatBJoin(Location seatBJoin) {
        this.seatBJoin = cloneLocation(seatBJoin);
    }

    public Location getJoin(Seat seat) {
        return seat == Seat.A ? getSeatAJoin() : getSeatBJoin();
    }

    public void setJoin(Seat seat, Location location) {
        if (seat == Seat.A) {
            setSeatAJoin(location);
        } else {
            setSeatBJoin(location);
        }
    }

    public Seat findJoinSeat(Block block) {
        if (matchesBlock(seatAJoin, block)) {
            return Seat.A;
        }
        if (matchesBlock(seatBJoin, block)) {
            return Seat.B;
        }
        return null;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = Math.max(2, columns);
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = Math.max(2, rows);
    }

    public int getCellSize() {
        return cellSize;
    }

    public void setCellSize(int cellSize) {
        this.cellSize = Math.max(1, cellSize);
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = Math.max(0, gap);
    }

    public int cellCount() {
        return columns * rows;
    }

    public Double getEntryFeeOverride() {
        return entryFeeOverride;
    }

    public void setEntryFeeOverride(Double entryFeeOverride) {
        this.entryFeeOverride = entryFeeOverride;
    }

    public boolean isReady() {
        return lobby != null && lobby.getWorld() != null
                && origin != null && origin.getWorld() != null
                && facing != null && facing.isCartesian() && facing != BlockFace.UP && facing != BlockFace.DOWN
                && seatAJoin != null && seatAJoin.getWorld() != null
                && seatBJoin != null && seatBJoin.getWorld() != null;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("lobby", serializeLocation(lobby));
        map.put("origin", serializeLocation(origin));
        map.put("facing", facing.name());
        map.put("seat-a-join", serializeLocation(seatAJoin));
        map.put("seat-b-join", serializeLocation(seatBJoin));
        map.put("columns", columns);
        map.put("rows", rows);
        map.put("cell-size", cellSize);
        map.put("gap", gap);
        if (entryFeeOverride != null) {
            map.put("entry-fee", entryFeeOverride);
        }
        return map;
    }

    public static Arena deserialize(String name, ConfigurationSection section) {
        Arena arena = new Arena(name);
        if (section == null) {
            return arena;
        }
        arena.lobby = deserializeLocation(section.getConfigurationSection("lobby"));
        arena.origin = deserializeLocation(section.getConfigurationSection("origin"));
        String facingName = section.getString("facing", "NORTH");
        try {
            arena.facing = BlockFace.valueOf(facingName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            arena.facing = BlockFace.NORTH;
        }
        arena.seatAJoin = deserializeLocation(section.getConfigurationSection("seat-a-join"));
        arena.seatBJoin = deserializeLocation(section.getConfigurationSection("seat-b-join"));
        arena.columns = Math.max(2, section.getInt("columns", DEFAULT_COLUMNS));
        arena.rows = Math.max(2, section.getInt("rows", DEFAULT_ROWS));
        arena.cellSize = Math.max(1, section.getInt("cell-size", 1));
        arena.gap = Math.max(0, section.getInt("gap", 0));
        if (section.contains("entry-fee")) {
            arena.entryFeeOverride = section.getDouble("entry-fee");
        }
        return arena;
    }

    private static boolean matchesBlock(Location location, Block block) {
        if (location == null || block == null || location.getWorld() == null || block.getWorld() == null) {
            return false;
        }
        return location.getWorld().equals(block.getWorld())
                && location.getBlockX() == block.getX()
                && location.getBlockY() == block.getY()
                && location.getBlockZ() == block.getZ();
    }

    private static Map<String, Object> serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", location.getWorld().getName());
        map.put("x", location.getBlockX());
        map.put("y", location.getBlockY());
        map.put("z", location.getBlockZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        return map;
    }

    private static Location deserializeLocation(ConfigurationSection section) {
        if (section == null || !section.contains("world")) {
            return null;
        }
        World world = Bukkit.getWorld(section.getString("world"));
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );
    }

    private static Location cloneLocation(Location location) {
        return location == null ? null : location.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Arena arena)) {
            return false;
        }
        return Objects.equals(name, arena.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
