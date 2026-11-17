package com.aeroreserve.model;

/** Result of one complete booking transaction. */
public class BookingResult {

    private final Booking booking;
    private final Payment payment;
    private final Ticket ticket;

    public BookingResult(Booking booking, Payment payment, Ticket ticket) {
        this.booking = booking;
        this.payment = payment;
        this.ticket = ticket;
    }

    public Booking getBooking() {
        return booking;
    }

    public Payment getPayment() {
        return payment;
    }

    public Ticket getTicket() {
        return ticket;
    }
}
