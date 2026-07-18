package dev.genesi.match5.model;

import java.util.Locale;

/** Seat / side for a Match 5 player. */
public enum Seat {
    A,
    B;

    public Seat opposite() {
        return this == A ? B : A;
    }

    public String display() {
        return name();
    }

    public static Seat fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Seat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
