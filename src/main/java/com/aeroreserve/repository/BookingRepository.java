package com.aeroreserve.repository;

import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.model.Booking;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Reservation persistence and passenger/flight lookup. */
public class BookingRepository extends SerializedRepository<Booking> {

    public BookingRepository(Path file) {
        super(file);
    }

    public List<Booking> findByPassenger(String passengerId) {
        return findAll().stream()
                .filter(booking -> booking.getPassengerId().equals(passengerId))
                .sorted(Comparator.comparing(Booking::getBookingDate).reversed())
                .toList();
    }

    public List<Booking> findByFlight(String flightId) {
        return findAll().stream()
                .filter(booking -> booking.getFlightId().equals(flightId))
                .toList();
    }

    public List<Booking> findActiveByFlight(String flightId) {
        return findByFlight(flightId).stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .toList();
    }

    public List<Booking> findRecent(int limit) {
        return findAll().stream()
                .sorted(Comparator.comparing(Booking::getBookingDate).reversed())
                .limit(limit)
                .toList();
    }
}
