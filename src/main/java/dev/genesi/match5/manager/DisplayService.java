package dev.genesi.match5.manager;

import dev.genesi.match5.Match5Plugin;
import dev.genesi.match5.model.Arena;
import dev.genesi.match5.model.GameSession;

/**
 * Board visuals are floor signs ({@link SignService}).
 * Kept as a thin facade so call sites stay stable.
 */
public final class DisplayService {

    private final Match5Plugin plugin;

    public DisplayService(Match5Plugin plugin) {
        this.plugin = plugin;
    }

    public int buildBoard(Arena arena) {
        return plugin.getSignService().buildBoard(arena);
    }

    public void spawnBoard(GameSession session, Arena arena) {
        plugin.getSignService().resetBoard(arena);
    }

    public void revealCell(GameSession session, Arena arena, int column, int row) {
        plugin.getSignService().revealCell(session, arena, column, row);
    }

    public void clearDisplays(GameSession session) {
        // Signs stay in the world between rounds; reset text on next spawn/reset.
    }

    public void clearPreview(String arenaName) {
        // no-op — signs are world blocks
    }

    public void clearAllPreviews() {
        // no-op
    }
}
