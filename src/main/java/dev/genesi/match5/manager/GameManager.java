package dev.genesi.match5.manager;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.board.BoardGeometry;
import dev.genesi.match5.board.BoardLogic;
import dev.genesi.match5.model.Arena;
import dev.genesi.match5.model.GameSession;
import dev.genesi.match5.model.PlayerState;
import dev.genesi.match5.model.Seat;
import dev.genesi.match5.model.TileContent;
import dev.genesi.match5.util.IconDef;
import dev.genesi.match5.util.IconPalette;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GameManager {

    private final Match5Plugin plugin;
    private final Map<String, GameSession> byArena = new HashMap<>();
    private final Map<UUID, GameSession> byPlayer = new HashMap<>();

    public GameManager(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    public Optional<GameSession> getByPlayer(UUID uuid) {
        return Optional.ofNullable(byPlayer.get(uuid));
    }

    public Optional<GameSession> getByArena(String arenaName) {
        return Optional.ofNullable(byArena.get(arenaName.toLowerCase(Locale.ROOT)));
    }

    public boolean isBusy(String arenaName) {
        GameSession session = byArena.get(arenaName.toLowerCase(Locale.ROOT));
        return session != null
                && session.getState() != GameSession.State.WAITING
                && session.getState() != GameSession.State.LOBBY_COUNTDOWN;
    }

    public boolean isActive(String arenaName) {
        return byArena.containsKey(arenaName.toLowerCase(Locale.ROOT));
    }

    public String join(Player player, Arena arena) {
        return join(player, arena, null);
    }

    public String join(Player player, Arena arena, Seat preferredSeat) {
        if (byPlayer.containsKey(player.getUniqueId())) {
            return "already-playing";
        }
        if (!arena.isReady()) {
            return "arena-not-ready";
        }

        GameSession session = byArena.computeIfAbsent(arena.getName(), GameSession::new);
        if (session.getState() != GameSession.State.WAITING
                && session.getState() != GameSession.State.LOBBY_COUNTDOWN) {
            return "arena-busy";
        }
        if (session.playerCount() >= 2) {
            plugin.getMessageService().send(player, "arena-full", Map.of("arena", arena.getName()));
            return "handled";
        }

        Seat seat = preferredSeat;
        if (seat != null && session.seatOwner(seat) != null) {
            plugin.getMessageService().send(player, "seat-taken", Map.of("seat", seat.display()));
            return "handled";
        }
        if (seat == null) {
            seat = session.seatOwner(Seat.A) == null ? Seat.A : Seat.B;
        }

        double fee = plugin.getArenaManager().resolveEntryFee(arena);
        if (fee > 0) {
            if (!plugin.getEconomyService().isReady()) {
                return "economy-missing";
            }
            if (!plugin.getEconomyService().has(player, fee) && !player.hasPermission("match5.bypass.fee")) {
                plugin.getMessageService().send(player, "not-enough-money", Map.of(
                        "amount", plugin.getEconomyService().format(fee),
                        "balance", plugin.getEconomyService().format(plugin.getEconomyService().getBalance(player))
                ));
                return "handled";
            }
            if (!plugin.getEconomyService().charge(player, fee)) {
                plugin.getMessageService().send(player, "not-enough-money", Map.of(
                        "amount", plugin.getEconomyService().format(fee),
                        "balance", plugin.getEconomyService().format(plugin.getEconomyService().getBalance(player))
                ));
                return "handled";
            }
        }

        PlayerState state = new PlayerState(player);
        state.setSeat(seat);
        snapshotPlayer(player, state);
        session.getPlayers().put(player.getUniqueId(), state);
        session.getSeats().put(seat, player.getUniqueId());
        byPlayer.put(player.getUniqueId(), session);

        Location lobby = arena.getLobby();
        if (lobby != null) {
            player.teleport(lobby);
        }

        plugin.getMessageService().send(player, "joined-waiting", Map.of(
                "arena", arena.getName(),
                "count", String.valueOf(session.playerCount()),
                "seat", seat.display()
        ));
        broadcast(session, "joined-waiting", Map.of(
                "arena", arena.getName(),
                "count", String.valueOf(session.playerCount()),
                "seat", seat.display()
        ), player.getUniqueId());

        maybeStartLobbyCountdown(session, arena);
        return "ok";
    }

    public boolean leave(Player player, boolean announce) {
        GameSession session = byPlayer.get(player.getUniqueId());
        if (session == null) {
            if (announce) {
                plugin.getMessageService().send(player, "not-playing");
            }
            return false;
        }

        if (session.getState() == GameSession.State.PLAYING
                || session.getState() == GameSession.State.START_COUNTDOWN) {
            PlayerState quitter = session.getPlayer(player.getUniqueId());
            Seat winnerSeat = quitter == null || quitter.getSeat() == null ? null : quitter.getSeat().opposite();
            if (winnerSeat != null && session.playerWithSeat(winnerSeat) != null) {
                Player winnerPlayer = Bukkit.getPlayer(session.playerWithSeat(winnerSeat).getUuid());
                if (winnerPlayer != null) {
                    plugin.getMessageService().send(winnerPlayer, "opponent-quit", Map.of("player", player.getName()));
                }
                endGame(session, winnerSeat, false);
            } else {
                endGame(session, null, false);
            }
            return true;
        }

        PlayerState state = session.getPlayers().remove(player.getUniqueId());
        byPlayer.remove(player.getUniqueId());
        if (state != null && state.getSeat() != null) {
            session.getSeats().remove(state.getSeat());
        }
        plugin.getSidebarService().clear(player);
        if (state != null) {
            restorePlayer(player, state);
        }
        if (announce) {
            plugin.getMessageService().send(player, "left");
        }
        broadcast(session, "player-left", Map.of("player", player.getName()), player.getUniqueId());

        if (session.getState() == GameSession.State.LOBBY_COUNTDOWN && session.playerCount() < 2) {
            session.cancelTasks();
            session.setState(GameSession.State.WAITING);
        }
        if (session.playerCount() == 0) {
            cleanupSession(session);
        }
        return true;
    }

    public void tryStart(Player player) {
        GameSession session = byPlayer.get(player.getUniqueId());
        if (session == null) {
            plugin.getMessageService().send(player, "not-playing");
            return;
        }
        Optional<Arena> arena = plugin.getArenaManager().get(session.getArenaName());
        if (arena.isEmpty()) {
            return;
        }
        if (session.playerCount() < 2) {
            plugin.getMessageService().send(player, "need-more-players");
            return;
        }
        if (session.getState() != GameSession.State.WAITING
                && session.getState() != GameSession.State.LOBBY_COUNTDOWN) {
            plugin.getMessageService().send(player, "arena-busy", Map.of("arena", arena.get().getName()));
            return;
        }
        session.cancelTasks();
        beginMatch(session, arena.get());
    }

    public void forceStart(Arena arena) {
        GameSession session = byArena.get(arena.getName());
        if (session == null || session.playerCount() < 2) {
            return;
        }
        session.cancelTasks();
        beginMatch(session, arena);
    }

    public void forceStop(String arenaName) {
        GameSession session = byArena.get(arenaName.toLowerCase(Locale.ROOT));
        if (session != null) {
            endGame(session, null, true);
        }
    }

    public boolean handleInteract(Player player, Block block) {
        Optional<Arena> joinArena = plugin.getArenaManager().findByJoinBlock(block);
        if (joinArena.isPresent()) {
            Seat seat = joinArena.get().findJoinSeat(block);
            String result = join(player, joinArena.get(), seat);
            switch (result) {
                case "ok", "handled" -> {
                }
                case "already-playing" -> plugin.getMessageService().send(player, "already-playing");
                case "arena-not-ready" -> plugin.getMessageService().send(player, "arena-not-ready",
                        Map.of("arena", joinArena.get().getName()));
                case "arena-busy" -> plugin.getMessageService().send(player, "arena-busy",
                        Map.of("arena", joinArena.get().getName()));
                case "economy-missing" -> plugin.getMessageService().send(player, "economy-missing");
                default -> {
                }
            }
            return true;
        }

        GameSession session = byPlayer.get(player.getUniqueId());
        if (session == null || session.getState() != GameSession.State.PLAYING) {
            return false;
        }

        Optional<Arena> arenaOpt = plugin.getArenaManager().get(session.getArenaName());
        if (arenaOpt.isEmpty()) {
            return false;
        }
        Arena arena = arenaOpt.get();
        BoardGeometry geometry;
        try {
            geometry = new BoardGeometry(arena);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        int[] cell = geometry.cellAt(block);
        if (cell == null) {
            return false;
        }

        PlayerState state = session.getPlayer(player.getUniqueId());
        if (state == null || state.getSeat() == null) {
            return true;
        }
        if (state.getSeat() != session.getCurrentTurn()) {
            plugin.getMessageService().send(player, "not-your-turn");
            return true;
        }
        if (state.getChances() <= 0) {
            plugin.getMessageService().send(player, "no-chances");
            passTurn(session, state);
            return true;
        }

        int index = geometry.index(cell[0], cell[1]);
        if (session.isRevealed(index)) {
            plugin.getMessageService().send(player, "already-revealed");
            return true;
        }

        // Spend one chance per reveal
        state.useChance();
        session.getRevealed()[index] = true;
        TileContent content = session.contentAt(index);
        plugin.getDisplayService().revealCell(session, arena, cell[0], cell[1]);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.15f);

        int target = Math.max(1, plugin.getConfig().getInt("match-to", 5));
        boolean hit = content.belongsTo(state.getSeat());

        if (hit) {
            state.addScore(1);
            plugin.getMessageService().send(player, "found-yours", Map.of(
                    "icon", state.getIconLabel(),
                    "score", String.valueOf(state.getScore()),
                    "target", String.valueOf(target),
                    "chances", String.valueOf(state.getChances())
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
            if (state.getScore() >= target) {
                plugin.getSidebarService().update(session);
                endGame(session, state.getSeat(), false);
                return true;
            }
        } else if (content == TileContent.BLANK) {
            plugin.getMessageService().send(player, "found-blank", Map.of(
                    "chances", String.valueOf(state.getChances())
            ));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
        } else {
            PlayerState owner = session.playerWithSeat(content == TileContent.A ? Seat.A : Seat.B);
            String icon = owner == null ? "icon" : owner.getIconLabel();
            plugin.getMessageService().send(player, "found-opponent", Map.of(
                    "icon", icon,
                    "chances", String.valueOf(state.getChances())
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }

        if (BoardLogic.allRevealed(session.getRevealed())) {
            endGame(session, resolveScoreWinner(session), false);
            return true;
        }

        // Out of chances without reaching target → lose if opponent can still win, else score compare
        if (state.getChances() <= 0 && state.getScore() < target) {
            plugin.getMessageService().send(player, "out-of-chances");
            PlayerState opponent = session.playerWithSeat(state.getSeat().opposite());
            if (opponent != null && opponent.getChances() <= 0) {
                endGame(session, resolveScoreWinner(session), false);
                return true;
            }
            // Opponent keeps playing; this player can no longer take turns
            passTurn(session, state);
            plugin.getSidebarService().update(session);
            return true;
        }

        boolean keepTurn = hit && plugin.getConfig().getBoolean("keep-turn-on-hit", true);
        if (keepTurn) {
            plugin.getMessageService().send(player, "keep-turn");
        } else {
            passTurn(session, state);
        }
        plugin.getSidebarService().update(session);
        return true;
    }

    public void shutdown() {
        for (GameSession session : new ArrayList<>(byArena.values())) {
            endGame(session, null, true);
        }
        plugin.getDisplayService().clearAllPreviews();
    }

    private void passTurn(GameSession session, PlayerState current) {
        Seat next = current.getSeat().opposite();
        PlayerState opponent = session.playerWithSeat(next);
        if (opponent != null && opponent.getChances() > 0) {
            session.setCurrentTurn(next);
            notifyTurn(session);
            return;
        }
        // Opponent also out — end by score
        if (opponent == null || opponent.getChances() <= 0) {
            endGame(session, resolveScoreWinner(session), false);
        }
    }

    private void maybeStartLobbyCountdown(GameSession session, Arena arena) {
        if (session.playerCount() < 2 || session.getState() == GameSession.State.LOBBY_COUNTDOWN) {
            return;
        }
        session.setState(GameSession.State.LOBBY_COUNTDOWN);
        int seconds = Math.max(1, plugin.getConfig().getInt("lobby-countdown-seconds", 5));
        final int[] remaining = {seconds};
        session.setCountdownTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (session.playerCount() < 2) {
                session.cancelTasks();
                session.setState(GameSession.State.WAITING);
                return;
            }
            if (remaining[0] <= 0) {
                session.cancelTasks();
                beginMatch(session, arena);
                return;
            }
            broadcast(session, "lobby-countdown", Map.of("seconds", String.valueOf(remaining[0])), null);
            remaining[0]--;
        }, 0L, 20L));
    }

    private void beginMatch(GameSession session, Arena arena) {
        session.setState(GameSession.State.START_COUNTDOWN);
        session.setSize(arena.getColumns(), arena.getRows());

        int target = Math.max(1, plugin.getConfig().getInt("match-to", 5));
        int copies = Math.max(target, plugin.getConfig().getInt("copies-per-icon", target));
        int maxCopies = arena.cellCount() / 2;
        if (copies > maxCopies) {
            copies = maxCopies;
        }
        session.setHidden(BoardLogic.generate(arena.getColumns(), arena.getRows(), copies));
        session.setRevealed(new boolean[arena.cellCount()]);

        assignIcons(session);
        plugin.getDisplayService().spawnBoard(session, arena);
        plugin.getSidebarService().update(session);

        for (PlayerState state : session.getPlayers().values()) {
            Player player = Bukkit.getPlayer(state.getUuid());
            if (player != null) {
                plugin.getMessageService().send(player, "your-icon", Map.of(
                        "icon", state.getIconLabel(),
                        "target", String.valueOf(target),
                        "chances", String.valueOf(state.getChances())
                ));
            }
        }

        int seconds = Math.max(0, plugin.getConfig().getInt("start-countdown-seconds", 3));
        if (seconds == 0) {
            startPlaying(session);
            return;
        }
        final int[] remaining = {seconds};
        session.setCountdownTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] <= 0) {
                session.cancelTasks();
                for (UUID uuid : session.getPlayers().keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        plugin.getMessageService().title(player,
                                plugin.getConfig().getString("messages.go", "&a&lGO!"), "");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    }
                }
                startPlaying(session);
                return;
            }
            for (UUID uuid : session.getPlayers().keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    plugin.getMessageService().title(player, "&e" + remaining[0], "");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }
            }
            remaining[0]--;
        }, 0L, 20L));
    }

    private void startPlaying(GameSession session) {
        session.setState(GameSession.State.PLAYING);
        String firstTurn = plugin.getConfig().getString("first-turn", "seat-a");
        if ("random".equalsIgnoreCase(firstTurn) && ThreadLocalRandom.current().nextBoolean()) {
            session.setCurrentTurn(Seat.B);
        } else if ("seat-b".equalsIgnoreCase(firstTurn)) {
            session.setCurrentTurn(Seat.B);
        } else {
            session.setCurrentTurn(Seat.A);
        }
        notifyTurn(session);
        plugin.getSidebarService().update(session);
        session.setSidebarTask(Bukkit.getScheduler().runTaskTimer(plugin,
                () -> plugin.getSidebarService().update(session), 40L, 40L));
    }

    private void assignIcons(GameSession session) {
        IconDef[] picks = IconPalette.pickTwo(IconPalette.load(plugin.getConfig()));
        int chances = Math.max(1, plugin.getConfig().getInt("chances", 10));
        PlayerState a = session.playerWithSeat(Seat.A);
        PlayerState b = session.playerWithSeat(Seat.B);
        if (a != null) {
            a.setIcon(picks[0]);
            a.setScore(0);
            a.setChances(chances);
        }
        if (b != null) {
            b.setIcon(picks[1]);
            b.setScore(0);
            b.setChances(chances);
        }
    }

    private void notifyTurn(GameSession session) {
        PlayerState current = session.playerWithSeat(session.getCurrentTurn());
        for (PlayerState state : session.getPlayers().values()) {
            Player player = Bukkit.getPlayer(state.getUuid());
            if (player == null) {
                continue;
            }
            if (current != null && state.getUuid().equals(current.getUuid())) {
                plugin.getMessageService().send(player, "your-turn");
            } else if (current != null) {
                plugin.getMessageService().send(player, "opponent-turn", Map.of("player", current.getName()));
            }
        }
    }

    private Seat resolveScoreWinner(GameSession session) {
        PlayerState a = session.playerWithSeat(Seat.A);
        PlayerState b = session.playerWithSeat(Seat.B);
        if (a == null || b == null) {
            return null;
        }
        if (a.getScore() > b.getScore()) {
            return Seat.A;
        }
        if (b.getScore() > a.getScore()) {
            return Seat.B;
        }
        return null;
    }

    private void endGame(GameSession session, Seat winnerSeat, boolean forced) {
        if (session.getState() == GameSession.State.ENDING) {
            return;
        }
        session.setState(GameSession.State.ENDING);
        session.cancelTasks();
        plugin.getSidebarService().clearSession(session);
        plugin.getDisplayService().clearDisplays(session);

        if (!forced) {
            if (winnerSeat == null) {
                broadcast(session, "draw", Map.of(), null);
                int drawPoints = plugin.getConfig().getInt("points-draw", 1);
                for (PlayerState state : session.getPlayers().values()) {
                    Player player = Bukkit.getPlayer(state.getUuid());
                    if (player == null) {
                        continue;
                    }
                    if (drawPoints > 0) {
                        plugin.getPointsService().addPoints(player, drawPoints);
                        runRewardCommands(player, session.getArenaName(), "draw", drawPoints);
                    }
                    plugin.getMessageService().send(player, "you-draw", Map.of("points", String.valueOf(drawPoints)));
                }
            } else {
                PlayerState winner = session.playerWithSeat(winnerSeat);
                String winnerName = winner == null ? "Unknown" : winner.getName();
                broadcast(session, "win", Map.of("player", winnerName), null);
                int winPoints = plugin.getConfig().getInt("points-win", 3);
                for (PlayerState state : session.getPlayers().values()) {
                    Player player = Bukkit.getPlayer(state.getUuid());
                    if (player == null) {
                        continue;
                    }
                    if (state.getSeat() == winnerSeat) {
                        if (winPoints > 0) {
                            plugin.getPointsService().addPoints(player, winPoints);
                            runRewardCommands(player, session.getArenaName(), "win", winPoints);
                        }
                        plugin.getMessageService().send(player, "you-win", Map.of("points", String.valueOf(winPoints)));
                        plugin.getMessageService().title(player, "&aVictory!", "&e+" + winPoints + " points");
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    } else {
                        plugin.getMessageService().send(player, "you-lose");
                        plugin.getMessageService().title(player, "&cDefeat", "");
                    }
                }
                broadcast(session, "game-over", Map.of("winner", winnerName), null);
            }
        }

        for (UUID uuid : new ArrayList<>(session.getPlayers().keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            PlayerState state = session.getPlayers().get(uuid);
            byPlayer.remove(uuid);
            if (player != null && state != null) {
                restorePlayer(player, state);
            }
        }
        cleanupSession(session);
    }

    private void cleanupSession(GameSession session) {
        session.cancelTasks();
        plugin.getDisplayService().clearDisplays(session);
        session.getPlayers().clear();
        session.getSeats().clear();
        byArena.remove(session.getArenaName());
    }

    private void runRewardCommands(Player player, String arena, String result, int points) {
        if (plugin.getConfig().getBoolean("reward.deposit-to-vault", false)
                && plugin.getEconomyService().isReady()) {
            plugin.getEconomyService().deposit(player, points);
        }
        for (String command : plugin.getConfig().getStringList("reward.commands")) {
            if (command == null || command.isBlank()) {
                continue;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("{points}", String.valueOf(points))
                    .replace("{arena}", arena)
                    .replace("{result}", result));
        }
    }

    private void broadcast(GameSession session, String key, Map<String, String> placeholders, UUID exclude) {
        for (UUID uuid : session.getPlayers().keySet()) {
            if (exclude != null && exclude.equals(uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getMessageService().send(player, key, placeholders);
            }
        }
    }

    private void snapshotPlayer(Player player, PlayerState state) {
        state.setReturnLocation(player.getLocation());
        state.setGameMode(player.getGameMode());
        state.setFlying(player.isFlying());
        state.setAllowFlight(player.getAllowFlight());
        state.setHealth(player.getHealth());
        state.setFoodLevel(player.getFoodLevel());
        state.setSaturation(player.getSaturation());
        state.setExhaustion(player.getExhaustion());
        state.setInventory(player.getInventory().getContents().clone());
        state.setArmor(player.getInventory().getArmorContents().clone());
        state.setOffhand(player.getInventory().getItemInOffHand().clone());
        state.setEffects(player.getActivePotionEffects());
    }

    private void restorePlayer(Player player, PlayerState state) {
        plugin.getSidebarService().clear(player);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        for (PotionEffect effect : state.getEffects()) {
            player.addPotionEffect(effect);
        }
        if (state.getInventory() != null) {
            player.getInventory().setContents(state.getInventory());
        }
        if (state.getArmor() != null) {
            player.getInventory().setArmorContents(state.getArmor());
        }
        if (state.getOffhand() != null) {
            player.getInventory().setItemInOffHand(state.getOffhand());
        }
        player.setGameMode(state.getGameMode() == null ? GameMode.SURVIVAL : state.getGameMode());
        player.setAllowFlight(state.isAllowFlight());
        player.setFlying(state.isFlying() && state.isAllowFlight());
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0 : maxHealth.getValue();
        player.setHealth(Math.min(state.getHealth(), max));
        player.setFoodLevel(state.getFoodLevel());
        player.setSaturation(state.getSaturation());
        player.setExhaustion(state.getExhaustion());
        if (plugin.getConfig().getBoolean("teleport-on-end", true) && state.getReturnLocation() != null) {
            player.teleport(state.getReturnLocation());
        }
    }

    public static BlockFace yawToFacing(float yaw) {
        float rot = (yaw % 360 + 360) % 360;
        if (rot >= 315 || rot < 45) {
            return BlockFace.SOUTH;
        }
        if (rot < 135) {
            return BlockFace.WEST;
        }
        if (rot < 225) {
            return BlockFace.NORTH;
        }
        return BlockFace.EAST;
    }
}
