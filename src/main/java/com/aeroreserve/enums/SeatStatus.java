package com.aeroreserve.enums;

/** Occupancy state of a physical seat on a specific flight. */
public enum SeatStatus {
    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    BLOCKED("Blocked");

    private final String displayName;

    SeatStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
