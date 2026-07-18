package dev.genesi.match5.board;

import dev.genesi.match5.model.TileContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardLogicTest {

    @Test
    void generatePlacesExactCopiesAndBlanks() {
        TileContent[] board = BoardLogic.generate(6, 6, 8);
        assertEquals(36, board.length);

        int a = 0;
        int b = 0;
        int blank = 0;
        for (TileContent content : board) {
            switch (content) {
                case A -> a++;
                case B -> b++;
                case BLANK -> blank++;
            }
        }
        assertEquals(8, a);
        assertEquals(8, b);
        assertEquals(20, blank);
    }

    @Test
    void generateRejectsOversizedCopies() {
        assertThrows(IllegalArgumentException.class, () -> BoardLogic.generate(2, 2, 3));
    }

    @Test
    void allRevealedAndRemainingCounts() {
        TileContent[] hidden = {
                TileContent.A, TileContent.B, TileContent.BLANK, TileContent.A
        };
        boolean[] revealed = {true, false, false, false};
        assertEquals(1, BoardLogic.countRemaining(hidden, revealed, TileContent.A));
        assertEquals(1, BoardLogic.countRemaining(hidden, revealed, TileContent.B));
        revealed[1] = true;
        revealed[2] = true;
        revealed[3] = true;
        assertTrue(BoardLogic.allRevealed(revealed));
    }
}
