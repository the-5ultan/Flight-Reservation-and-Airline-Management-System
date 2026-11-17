package com.aeroreserve.enums;

/** Cabin classes and their polymorphic fare multipliers. */
public enum SeatClass {
    ECONOMY("Economy", 1.0, "EC"),
    BUSINESS("Business", 1.75, "BS"),
    FIRST("First Class", 2.6, "FC");

    private final String displayName;
    private final double fareMultiplier;
    private final String code;

    SeatClass(String displayName, double fareMultiplier, String code) {
        this.displayName = displayName;
        this.fareMultiplier = fareMultiplier;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getFareMultiplier() {
        return fareMultiplier;
    }

    public String getCode() {
        return code;
    }

    /** Base fare for this cabin given the flight's economy base price. */
    public double calculateFare(double basePrice) {
        return Math.round(basePrice * fareMultiplier * 100.0) / 100.0;
    }
}
