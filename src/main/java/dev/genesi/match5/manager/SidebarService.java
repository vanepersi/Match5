package dev.genesi.match5.manager;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.model.GameSession;
import dev.genesi.match5.model.PlayerState;
import dev.genesi.match5.model.Seat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

/**
 * Per-player sidebar showing your mob, score, and whose turn it is.
 */
public final class SidebarService {

    private final Match5Plugin plugin;

    public SidebarService(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    public void update(GameSession session) {
        if (!plugin.getConfig().getBoolean("sidebar.enabled", true)) {
            return;
        }
        int target = plugin.getConfig().getInt("match-to", 5);
        for (PlayerState state : session.getPlayers().values()) {
            Player player = Bukkit.getPlayer(state.getUuid());
            if (player == null) {
                continue;
            }
            render(player, session, state, target);
        }
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void clearSession(GameSession session) {
        for (UUID uuid : session.getPlayers().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                clear(player);
            }
        }
    }

    private void render(Player player, GameSession session, PlayerState self, int target) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String title = color(plugin.getConfig().getString("sidebar.title", "&6Match 5"));
        Objective objective = board.registerNewObjective("match5", Criteria.DUMMY, legacy(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        PlayerState opponent = session.playerWithSeat(self.getSeat() == null ? Seat.A : self.getSeat().opposite());
        String turnName = "?";
        PlayerState current = session.playerWithSeat(session.getCurrentTurn());
        if (current != null) {
            turnName = current.getName();
        }

        int line = 10;
        set(objective, color("&7────────────"), line--);
        set(objective, color("&fYour mob"), line--);
        set(objective, color("&e" + self.getMobLabel()), line--);
        set(objective, color("&fScore &a" + self.getScore() + "&7/&f" + target), line--);
        set(objective, color(" "), line--);
        if (opponent != null) {
            set(objective, color("&fOpponent"), line--);
            set(objective, color("&7" + opponent.getName()), line--);
            set(objective, color("&fTheir mob &e" + opponent.getMobLabel()), line--);
            set(objective, color("&fScore &c" + opponent.getScore() + "&7/&f" + target), line--);
            set(objective, color("  "), line--);
        }
        set(objective, color("&fTurn &b" + turnName), line);

        player.setScoreboard(board);
    }

    private static void set(Objective objective, String text, int score) {
        objective.getScore(text).setScore(score);
    }

    private static String color(String input) {
        return LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(input == null ? "" : input)
        );
    }

    private static net.kyori.adventure.text.Component legacy(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input == null ? "" : input);
    }
}
