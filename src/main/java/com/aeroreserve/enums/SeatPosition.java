package com.aeroreserve.enums;

/** Position of a seat relative to the cabin aisle and windows. */
public enum SeatPosition {
    WINDOW("Window"),
    MIDDLE("Middle"),
    AISLE("Aisle");

    private final String displayName;

    SeatPosition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
