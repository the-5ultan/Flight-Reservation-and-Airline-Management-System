package com.aeroreserve.enums;

/** Lifecycle of a reservation. */
public enum BookingStatus {
    PENDING("Pending payment"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == PENDING || this == CONFIRMED || this == COMPLETED;
    }
}
