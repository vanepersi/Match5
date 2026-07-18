package dev.genesi.match5.board;

import dev.genesi.match5.model.TileContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure RNG board setup and win checks — no Bukkit dependency.
 */
public final class BoardLogic {

    private BoardLogic() {
    }

    /**
     * Builds a shuffled board with {@code copiesPerSeat} tiles for A and B,
     * and the rest blank. Throws if the grid is too small.
     */
    public static TileContent[] generate(int columns, int rows, int copiesPerSeat) {
        int size = columns * rows;
        if (copiesPerSeat < 0) {
            throw new IllegalArgumentException("copiesPerSeat must be >= 0");
        }
        if (copiesPerSeat * 2 > size) {
            throw new IllegalArgumentException("grid too small for " + copiesPerSeat + " of each seat");
        }

        List<TileContent> tiles = new ArrayList<>(size);
        for (int i = 0; i < copiesPerSeat; i++) {
            tiles.add(TileContent.A);
            tiles.add(TileContent.B);
        }
        while (tiles.size() < size) {
            tiles.add(TileContent.BLANK);
        }
        Collections.shuffle(tiles, ThreadLocalRandom.current());
        return tiles.toArray(TileContent[]::new);
    }

    public static boolean allRevealed(boolean[] revealed) {
        if (revealed == null) {
            return true;
        }
        for (boolean value : revealed) {
            if (!value) {
                return false;
            }
        }
        return true;
    }

    public static int countRemaining(TileContent[] hidden, boolean[] revealed, TileContent content) {
        if (hidden == null || revealed == null) {
            return 0;
        }
        int count = 0;
        int len = Math.min(hidden.length, revealed.length);
        for (int i = 0; i < len; i++) {
            if (!revealed[i] && hidden[i] == content) {
                count++;
            }
        }
        return count;
    }
}
