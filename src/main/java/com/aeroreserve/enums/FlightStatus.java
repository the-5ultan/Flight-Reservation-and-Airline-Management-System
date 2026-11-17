package com.aeroreserve.enums;

/** Lifecycle of a flight in the airline schedule. */
public enum FlightStatus {
    SCHEDULED("Scheduled"),
    DELAYED("Delayed"),
    BOARDING("Boarding"),
    DEPARTED("Departed"),
    ARRIVED("Arrived"),
    CANCELLED("Cancelled");

    private final String displayName;

    FlightStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** A flight in one of these states may still accept reservations. */
    public boolean isBookable() {
        return this == SCHEDULED || this == DELAYED;
    }
}
