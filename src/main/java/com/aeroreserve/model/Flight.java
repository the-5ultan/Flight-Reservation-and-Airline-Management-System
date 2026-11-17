package com.aeroreserve.model;

import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.enums.SeatStatus;
import com.aeroreserve.exception.InvalidFlightScheduleException;
import com.aeroreserve.exception.SeatAlreadyBookedException;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.util.IdGenerator;
import com.aeroreserve.util.ValidationUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** A scheduled flight, its per-flight seat inventory and its reservations. */
public class Flight implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String flightId;
    private String flightNumber;
    private String aircraftId;
    private String aircraftModel;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private double basePrice;
    private FlightStatus status;
    private final CustomList<Seat> seats = new CustomList<>();
    private final Set<String> bookingIds = new HashSet<>();

    public Flight(String flightNumber, String aircraftId, String departureAirport, String arrivalAirport,
                  LocalDateTime departureDateTime, LocalDateTime arrivalDateTime,
                  double basePrice, FlightStatus status) throws InvalidFlightScheduleException {
        this(IdGenerator.generate("FL"), flightNumber, aircraftId, departureAirport, arrivalAirport,
                departureDateTime, arrivalDateTime, basePrice, status);
    }

    public Flight(String flightId, String flightNumber, String aircraftId, String departureAirport,
                  String arrivalAirport, LocalDateTime departureDateTime, LocalDateTime arrivalDateTime,
                  double basePrice, FlightStatus status) throws InvalidFlightScheduleException {
        this.flightId = ValidationUtil.requireText(flightId, "Flight ID");
        validateSchedule(departureAirport, arrivalAirport, departureDateTime, arrivalDateTime, basePrice);
        this.flightNumber = ValidationUtil.requireText(flightNumber, "Flight number").toUpperCase();
        this.aircraftId = ValidationUtil.requireText(aircraftId, "Aircraft ID");
        this.departureAirport = ValidationUtil.normaliseAirport(departureAirport);
        this.arrivalAirport = ValidationUtil.normaliseAirport(arrivalAirport);
        this.departureDateTime = departureDateTime;
        this.arrivalDateTime = arrivalDateTime;
        this.basePrice = Math.round(basePrice * 100.0) / 100.0;
        this.status = status == null ? FlightStatus.SCHEDULED : status;
    }

    public static void validateSchedule(String departureAirport, String arrivalAirport,
                                        LocalDateTime departure, LocalDateTime arrival,
                                        double basePrice) throws InvalidFlightScheduleException {
        String from = ValidationUtil.normaliseAirport(departureAirport);
        String to = ValidationUtil.normaliseAirport(arrivalAirport);
        if (from.equals(to)) {
            throw new InvalidFlightScheduleException("Departure and arrival airports must be different");
        }
        if (!ValidationUtil.isValidSchedule(departure, arrival)) {
            throw new InvalidFlightScheduleException("Arrival must be after departure");
        }
        if (!ValidationUtil.isPositive(basePrice)) {
            throw new InvalidFlightScheduleException("Base price must be greater than zero");
        }
    }

    public void updateSchedule(String flightNumber, String departureAirport, String arrivalAirport,
                               LocalDateTime departure, LocalDateTime arrival, double basePrice,
                               FlightStatus newStatus) throws InvalidFlightScheduleException {
        validateSchedule(departureAirport, arrivalAirport, departure, arrival, basePrice);
        this.flightNumber = ValidationUtil.requireText(flightNumber, "Flight number").toUpperCase();
        this.departureAirport = ValidationUtil.normaliseAirport(departureAirport);
        this.arrivalAirport = ValidationUtil.normaliseAirport(arrivalAirport);
        this.departureDateTime = departure;
        this.arrivalDateTime = arrival;
        this.basePrice = Math.round(basePrice * 100.0) / 100.0;
        this.status = newStatus;
    }

    public void assignAircraft(Aircraft aircraft) {
        Objects.requireNonNull(aircraft, "aircraft");
        this.aircraftId = aircraft.getAircraftId();
        this.aircraftModel = aircraft.getModel();
        this.seats.clear();
        this.seats.addAll(aircraft.createFlightSeats());
    }

    public Seat getSeat(String seatNumber) {
        for (Seat seat : seats) {
            if (seat.getSeatNumber().equalsIgnoreCase(seatNumber)) {
                return seat;
            }
        }
        return null;
    }

    public boolean isSeatAvailable(String seatNumber) {
        Seat seat = getSeat(seatNumber);
        return seat != null && seat.isAvailable();
    }

    public void reserveSeat(String seatNumber, String bookingId) throws SeatAlreadyBookedException {
        Seat seat = getSeat(seatNumber);
        if (seat == null) {
            throw new SeatAlreadyBookedException("Seat " + seatNumber + " does not exist on this flight");
        }
        seat.reserve(bookingId);
        bookingIds.add(bookingId);
    }

    public void releaseSeats(CustomList<String> seatNumbers, String bookingId) {
        for (String seatNumber : seatNumbers) {
            Seat seat = getSeat(seatNumber);
            if (seat != null && (seat.isOccupiedBy(bookingId) || seat.getStatus() == SeatStatus.OCCUPIED)) {
                seat.release();
            }
        }
        bookingIds.remove(bookingId);
    }

    public CustomList<Seat> getAvailableSeats() {
        return seats.filter(Seat::isAvailable);
    }

    public CustomList<Seat> getSeatsByClass(SeatClass seatClass) {
        return seats.filter(seat -> seat.getSeatClass() == seatClass);
    }

    public CustomList<Seat> getSeatsSortedForDisplay() {
        java.util.List<Seat> ordered = seats.toList();
        ordered.sort(Comparator.comparingInt(Seat::getRow)
                .thenComparing(seat -> switch (seat.getSeatClass()) {
                    case FIRST -> 0;
                    case BUSINESS -> 1;
                    case ECONOMY -> 2;
                })
                .thenComparing(Seat::getColumn));
        return new CustomList<>(ordered);
    }

    public int getCapacity() {
        return seats.size();
    }

    public int getAvailableSeatCount() {
        return getAvailableSeats().size();
    }

    public int getOccupiedSeatCount() {
        int count = 0;
        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.OCCUPIED) {
                count++;
            }
        }
        return count;
    }

    public double getOccupancyRate() {
        return getCapacity() == 0 ? 0 : (getOccupiedSeatCount() * 100.0) / getCapacity();
    }

    public double getFare(SeatClass seatClass) {
        return seatClass.calculateFare(basePrice);
    }

    public boolean isBookable() {
        return status.isBookable()
                && departureDateTime.isAfter(LocalDateTime.now())
                && getAvailableSeatCount() > 0
                && getCapacity() > 0;
    }

    public String getRoute() {
        return departureAirport + " → " + arrivalAirport;
    }

    public String getFlightId() {
        return flightId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public String getAircraftModel() {
        return aircraftModel == null ? "Not assigned" : aircraftModel;
    }

    public void setAircraftModel(String aircraftModel) {
        this.aircraftModel = aircraftModel;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public LocalDateTime getDepartureDateTime() {
        return departureDateTime;
    }

    public LocalDateTime getArrivalDateTime() {
        return arrivalDateTime;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public CustomList<Seat> getSeats() {
        return new CustomList<>(seats.toList());
    }

    public Set<String> getBookingIds() {
        return new HashSet<>(bookingIds);
    }

    public void registerBookingId(String bookingId) {
        bookingIds.add(bookingId);
    }

    @Override
    public String toString() {
        return flightNumber + "  " + getRoute();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Flight flight && flightId.equals(flight.flightId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightId);
    }
}
