package com.aeroreserve.model;

import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.enums.SeatPosition;
import com.aeroreserve.enums.SeatStatus;
import com.aeroreserve.exception.SeatAlreadyBookedException;

import java.io.Serializable;
import java.util.Objects;

/** A physical seat. Template seats live on Aircraft; booked seats on Flight. */
public class Seat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String seatNumber;
    private final int row;
    private final String column;
    private final SeatClass seatClass;
    private final SeatPosition position;

    private SeatStatus status = SeatStatus.AVAILABLE;
    private String bookingId;

    public Seat(int row, String column, SeatClass seatClass, String columnLayout) {
        if (row < 1) {
            throw new IllegalArgumentException("Seat row must be positive");
        }
        this.row = row;
        this.column = normalise(column, "column");
        this.seatClass = Objects.requireNonNull(seatClass, "seatClass");
        this.seatNumber = row + this.column;
        this.position = derivePosition(this.column, columnLayout);
    }

    private static String normalise(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Seat " + field + " is required");
        }
        return value.trim().toUpperCase();
    }

    public static SeatPosition derivePosition(String column, String columnLayout) {
        String layout = normalise(columnLayout, "column layout");
        int index = layout.indexOf(normalise(column, "column"));
        if (index < 0) {
            throw new IllegalArgumentException("Column " + column + " is not in layout " + layout);
        }
        int length = layout.length();
        if (length <= 2) {
            return SeatPosition.WINDOW;
        }
        if (index == 0 || index == length - 1) {
            return SeatPosition.WINDOW;
        }
        if (length == 3) {
            return index == length - 2 ? SeatPosition.AISLE : SeatPosition.MIDDLE;
        }
        return (index == length / 2 - 1 || index == length / 2)
                ? SeatPosition.AISLE : SeatPosition.MIDDLE;
    }

    public void reserve(String bookingId) throws SeatAlreadyBookedException {
        if (status != SeatStatus.AVAILABLE) {
            throw new SeatAlreadyBookedException("Seat " + seatNumber + " is no longer available");
        }
        this.status = SeatStatus.OCCUPIED;
        this.bookingId = Objects.requireNonNull(bookingId, "bookingId");
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.bookingId = null;
    }

    public void block() {
        this.status = SeatStatus.BLOCKED;
        this.bookingId = null;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public boolean isOccupiedBy(String candidateBookingId) {
        return bookingId != null && bookingId.equals(candidateBookingId);
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public int getRow() {
        return row;
    }

    public String getColumn() {
        return column;
    }

    public SeatClass getSeatClass() {
        return seatClass;
    }

    public SeatPosition getPosition() {
        return position;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getDescription() {
        return seatNumber + "  ·  " + seatClass.getDisplayName() + "  ·  " + position.getDisplayName();
    }

    @Override
    public String toString() {
        return seatNumber;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Seat seat && seatNumber.equals(seat.seatNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatNumber);
    }
}
