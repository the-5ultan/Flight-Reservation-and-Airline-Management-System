package com.aeroreserve.service;

import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.exception.FlightNotFoundException;
import com.aeroreserve.exception.InsufficientSeatsException;
import com.aeroreserve.exception.SeatAlreadyBookedException;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.Seat;
import com.aeroreserve.repository.FlightRepository;

/** Owns seat inventory mutations. Synchronization prevents double bookings. */
public class SeatAllocationService {

    private final FlightRepository flightRepository;

    public SeatAllocationService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public CustomList<Seat> getSeatMap(String flightId) throws FlightNotFoundException {
        return getFlight(flightId).getSeatsSortedForDisplay();
    }

    public synchronized void reserveSeats(String flightId, CustomList<String> seatNumbers, String bookingId)
            throws FlightNotFoundException, SeatAlreadyBookedException, InsufficientSeatsException {
        Flight flight = getFlight(flightId);
        if (flight.getStatus() != FlightStatus.SCHEDULED && flight.getStatus() != FlightStatus.DELAYED) {
            throw new SeatAlreadyBookedException("Seats cannot be reserved on a " + flight.getStatus().getDisplayName() + " flight");
        }
        if (seatNumbers.isEmpty()) {
            throw new InsufficientSeatsException("Select at least one seat");
        }
        if (flight.getAvailableSeatCount() < seatNumbers.size()) {
            throw new InsufficientSeatsException("Only " + flight.getAvailableSeatCount() + " seat(s) remain");
        }
        for (String seatNumber : seatNumbers) {
            if (!flight.isSeatAvailable(seatNumber)) {
                throw new SeatAlreadyBookedException("Seat " + seatNumber + " was just taken by another passenger");
            }
        }
        for (String seatNumber : seatNumbers) {
            flight.reserveSeat(seatNumber, bookingId);
        }
        flightRepository.put(flightId, flight);
    }

    public synchronized void releaseSeats(String flightId, CustomList<String> seatNumbers, String bookingId)
            throws FlightNotFoundException {
        Flight flight = getFlight(flightId);
        flight.releaseSeats(seatNumbers, bookingId);
        flightRepository.put(flightId, flight);
    }

    public SeatClass resolveSeatClass(Flight flight, CustomList<String> seatNumbers)
            throws FlightNotFoundException {
        SeatClass seatClass = null;
        for (String seatNumber : seatNumbers) {
            Seat seat = flight.getSeat(seatNumber);
            if (seat == null) {
                throw new FlightNotFoundException("Seat " + seatNumber + " does not exist on this flight");
            }
            if (seatClass == null) {
                seatClass = seat.getSeatClass();
            } else if (seatClass != seat.getSeatClass()) {
                throw new IllegalArgumentException("All selected seats must be in the same cabin class");
            }
        }
        if (seatClass == null) {
            throw new IllegalArgumentException("Select at least one seat");
        }
        return seatClass;
    }

    public double calculateFare(Flight flight, SeatClass seatClass, int seatCount) {
        if (seatCount < 1) {
            throw new IllegalArgumentException("Seat count must be positive");
        }
        return Math.round(flight.getFare(seatClass) * seatCount * 100.0) / 100.0;
    }

    public int getAvailableSeatCount(String flightId) throws FlightNotFoundException {
        return getFlight(flightId).getAvailableSeatCount();
    }

    private Flight getFlight(String flightId) throws FlightNotFoundException {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found"));
    }
}
