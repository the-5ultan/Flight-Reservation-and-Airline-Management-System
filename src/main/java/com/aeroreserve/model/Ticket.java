package com.aeroreserve.model;

import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.generic.CustomList;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Immutable travel document generated from a confirmed booking. */
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final String ticketNumber;
    private final String bookingReference;
    private final String passengerName;
    private final String passengerEmail;
    private final String flightNumber;
    private final String departureAirport;
    private final String arrivalAirport;
    private final LocalDateTime departureDateTime;
    private final LocalDateTime arrivalDateTime;
    private final CustomList<String> seats;
    private final SeatClass seatClass;
    private final double baseFare;
    private final double taxesAndFees;
    private final double totalFare;
    private final BookingStatus status;
    private final LocalDateTime issuedAt;

    public Ticket(String ticketNumber, String bookingReference, String passengerName,
                  String passengerEmail, String flightNumber, String departureAirport,
                  String arrivalAirport, LocalDateTime departureDateTime, LocalDateTime arrivalDateTime,
                  CustomList<String> seats, SeatClass seatClass, double baseFare,
                  double taxesAndFees, BookingStatus status) {
        this.ticketNumber = ticketNumber;
        this.bookingReference = bookingReference;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureDateTime = departureDateTime;
        this.arrivalDateTime = arrivalDateTime;
        this.seats = new CustomList<>(seats.toList());
        this.seatClass = seatClass;
        this.baseFare = baseFare;
        this.taxesAndFees = taxesAndFees;
        this.totalFare = Math.round((baseFare + taxesAndFees) * 100.0) / 100.0;
        this.status = status;
        this.issuedAt = LocalDateTime.now();
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== AERORESERVE ELECTRONIC TICKET ==========\n");
        sb.append("Ticket Number : ").append(ticketNumber).append('\n');
        sb.append("Booking Ref   : ").append(bookingReference).append('\n');
        sb.append("Passenger     : ").append(passengerName).append('\n');
        sb.append("Email         : ").append(passengerEmail).append('\n');
        sb.append("Flight        : ").append(flightNumber).append('\n');
        sb.append("Route         : ").append(departureAirport).append(" -> ").append(arrivalAirport).append('\n');
        sb.append("Departure     : ").append(FORMAT.format(departureDateTime)).append('\n');
        sb.append("Arrival       : ").append(FORMAT.format(arrivalDateTime)).append('\n');
        sb.append("Seat(s)       : ").append(String.join(", ", seats.toList())).append('\n');
        sb.append("Cabin         : ").append(seatClass.getDisplayName()).append('\n');
        sb.append("Fare          : $").append(String.format("%.2f", baseFare)).append('\n');
        sb.append("Taxes & Fees  : $").append(String.format("%.2f", taxesAndFees)).append('\n');
        sb.append("Total Paid    : $").append(String.format("%.2f", totalFare)).append('\n');
        sb.append("Status        : ").append(status.getDisplayName()).append('\n');
        sb.append("Issued        : ").append(FORMAT.format(issuedAt)).append('\n');
        sb.append("===================================================\n");
        return sb.toString();
    }

    public String toHtml() {
        return "<html><head><title>AeroReserve Ticket " + ticketNumber + "</title>"
                + "<style>body{font-family:'Segoe UI',sans-serif;background:#0b1224;color:#e8f0ff;"
                + "padding:36px}.card{background:rgba(148,178,224,.09);border:1px solid rgba(190,215,255,.2);"
                + "border-radius:20px;padding:32px;max-width:640px;margin:auto}"
                + "h1{color:#67a8ff;margin:0 0 4px}table{width:100%;border-collapse:collapse;margin-top:22px}"
                + "td{padding:9px 0;border-bottom:1px solid rgba(150,180,225,.14)}"
                + "td:first-child{color:#93a7c4;width:38%}.total{color:#6ee7b7;font-weight:700}"
                + "</style></head><body><div class='card'><h1>AeroReserve</h1>"
                + "<div style='color:#93a7c4'>Electronic ticket · " + ticketNumber + "</div><table>"
                + row("Booking reference", bookingReference)
                + row("Passenger", passengerName)
                + row("Flight", flightNumber)
                + row("Route", departureAirport + " → " + arrivalAirport)
                + row("Departure", FORMAT.format(departureDateTime))
                + row("Arrival", FORMAT.format(arrivalDateTime))
                + row("Seat(s)", String.join(", ", seats.toList()))
                + row("Cabin", seatClass.getDisplayName())
                + row("Base fare", "$" + String.format("%.2f", baseFare))
                + row("Taxes & fees", "$" + String.format("%.2f", taxesAndFees))
                + "<tr><td>Total paid</td><td class='total'>$" + String.format("%.2f", totalFare)
                + "</td></tr></table></div></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td>" + label + "</td><td>" + escape(value) + "</td></tr>";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getRoute() {
        return departureAirport + " → " + arrivalAirport;
    }

    public String getSeatList() {
        return String.join(", ", seats.toList());
    }

    public double getTotalFare() {
        return totalFare;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getDepartureDateTime() {
        return departureDateTime;
    }
}
