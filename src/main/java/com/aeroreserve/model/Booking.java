package com.aeroreserve.model;

import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.util.IdGenerator;
import com.aeroreserve.util.ValidationUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** A reservation linking a passenger, a flight, selected seats and a payment. */
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double TAX_RATE = 0.075;
    private static final double SERVICE_FEE = 12.0;

    private final String bookingId;
    private final String ticketNumber;
    private final String passengerId;
    private final String flightId;
    private final CustomList<String> seatNumbers;
    private final SeatClass seatClass;
    private final double baseFare;
    private final double taxesAndFees;
    private final LocalDateTime bookingDate;

    private String passengerName;
    private String passengerEmail;
    private String flightNumber;
    private BookingStatus status = BookingStatus.PENDING;
    private String paymentId;
    private double cancellationFee;
    private double refundAmount;
    private LocalDateTime cancellationDate;

    public Booking(String passengerId, String flightId, CustomList<String> seatNumbers,
                   SeatClass seatClass, double baseFare, String passengerName,
                   String passengerEmail, String flightNumber) {
        this(IdGenerator.generate("BK"), passengerId, flightId, seatNumbers, seatClass,
                baseFare, passengerName, passengerEmail, flightNumber);
    }

    public Booking(String bookingId, String passengerId, String flightId, CustomList<String> seatNumbers,
                   SeatClass seatClass, double baseFare, String passengerName,
                   String passengerEmail, String flightNumber) {
        this.bookingId = ValidationUtil.requireText(bookingId, "Booking ID");
        this.ticketNumber = "TKT-" + this.bookingId.substring(this.bookingId.indexOf('-') + 1);
        this.passengerId = ValidationUtil.requireText(passengerId, "Passenger ID");
        this.flightId = ValidationUtil.requireText(flightId, "Flight ID");
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }
        this.seatNumbers = new CustomList<>(seatNumbers.toList());
        this.seatClass = Objects.requireNonNull(seatClass, "seatClass");
        if (!ValidationUtil.isPositive(baseFare)) {
            throw new IllegalArgumentException("Fare must be positive");
        }
        this.baseFare = Math.round(baseFare * 100.0) / 100.0;
        this.taxesAndFees = Math.round((this.baseFare * TAX_RATE + SERVICE_FEE) * 100.0) / 100.0;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.flightNumber = flightNumber;
        this.bookingDate = LocalDateTime.now();
    }

    public void confirm(String paymentId) {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId");
        this.status = BookingStatus.CONFIRMED;
    }

    public void cancel(double fee, double refund) {
        if (!status.isActive()) {
            throw new IllegalStateException("This booking is already cancelled");
        }
        if (fee < 0 || refund < 0) {
            throw new IllegalArgumentException("Charges cannot be negative");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancellationFee = Math.round(fee * 100.0) / 100.0;
        this.refundAmount = Math.round(refund * 100.0) / 100.0;
        this.cancellationDate = LocalDateTime.now();
    }

    public void complete() {
        if (status == BookingStatus.CONFIRMED) {
            this.status = BookingStatus.COMPLETED;
        }
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean belongsTo(String candidatePassengerId) {
        return passengerId.equals(candidatePassengerId);
    }

    public double getTotalAmount() {
        return Math.round((baseFare + taxesAndFees) * 100.0) / 100.0;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getFlightId() {
        return flightId;
    }

    public CustomList<String> getSeatNumbers() {
        return new CustomList<>(seatNumbers.toList());
    }

    public String getSeatList() {
        return String.join(", ", seatNumbers.toList());
    }

    public SeatClass getSeatClass() {
        return seatClass;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public double getTaxesAndFees() {
        return taxesAndFees;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public double getCancellationFee() {
        return cancellationFee;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public LocalDateTime getCancellationDate() {
        return cancellationDate;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public void setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    @Override
    public String toString() {
        return bookingId + " · " + flightNumber + " · " + status.getDisplayName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Booking booking && bookingId.equals(booking.bookingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId);
    }
}
