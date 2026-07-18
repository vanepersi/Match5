package dev.genesi.match5.model;

/**
 * What is hidden under a board tile.
 * {@link #BLANK} is empty luck; {@link #A}/{@link #B} belong to each seat's mob.
 */
public enum TileContent {
    BLANK,
    A,
    B;

    public boolean belongsTo(Seat seat) {
        return seat != null && name().equals(seat.name());
    }

    public static TileContent forSeat(Seat seat) {
        return seat == Seat.A ? A : B;
    }
}
