package dev.genesi.match5.model;

import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class GameSession {

    public enum State {
        WAITING,
        LOBBY_COUNTDOWN,
        START_COUNTDOWN,
        PLAYING,
        ENDING
    }

    private final String arenaName;
    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();
    private final Map<Seat, UUID> seats = new LinkedHashMap<>();
    private TileContent[] hidden;
    private boolean[] revealed;
    private TextDisplay[] displays;
    private int columns;
    private int rows;
    private State state = State.WAITING;
    private Seat currentTurn = Seat.A;
    private BukkitTask countdownTask;
    private BukkitTask sidebarTask;

    public GameSession(String arenaName) {
        this.arenaName = arenaName.toLowerCase();
    }

    public String getArenaName() {
        return arenaName;
    }

    public Map<UUID, PlayerState> getPlayers() {
        return players;
    }

    public PlayerState getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public int playerCount() {
        return players.size();
    }

    public Map<Seat, UUID> getSeats() {
        return seats;
    }

    public UUID seatOwner(Seat seat) {
        return seats.get(seat);
    }

    public PlayerState playerWithSeat(Seat seat) {
        UUID uuid = seats.get(seat);
        return uuid == null ? null : players.get(uuid);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Seat getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(Seat currentTurn) {
        this.currentTurn = currentTurn;
    }

    public TileContent[] getHidden() {
        return hidden;
    }

    public void setHidden(TileContent[] hidden) {
        this.hidden = hidden;
    }

    public boolean[] getRevealed() {
        return revealed;
    }

    public void setRevealed(boolean[] revealed) {
        this.revealed = revealed;
    }

    public TextDisplay[] getDisplays() {
        return displays;
    }

    public void setDisplays(TextDisplay[] displays) {
        this.displays = displays;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public void setSize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public int index(int column, int row) {
        return row * columns + column;
    }

    public boolean isRevealed(int index) {
        return revealed != null && index >= 0 && index < revealed.length && revealed[index];
    }

    public TileContent contentAt(int index) {
        if (hidden == null || index < 0 || index >= hidden.length) {
            return TileContent.BLANK;
        }
        return hidden[index];
    }

    public BukkitTask getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(BukkitTask countdownTask) {
        this.countdownTask = countdownTask;
    }

    public BukkitTask getSidebarTask() {
        return sidebarTask;
    }

    public void setSidebarTask(BukkitTask sidebarTask) {
        this.sidebarTask = sidebarTask;
    }

    public void cancelTasks() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (sidebarTask != null) {
            sidebarTask.cancel();
            sidebarTask = null;
        }
    }
}
