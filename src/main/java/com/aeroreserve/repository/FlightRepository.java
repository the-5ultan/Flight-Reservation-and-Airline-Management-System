package com.aeroreserve.repository;

import com.aeroreserve.model.Flight;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Flight persistence and schedule-oriented queries. */
public class FlightRepository extends SerializedRepository<Flight> {

    public FlightRepository(Path file) {
        super(file);
    }

    public Optional<Flight> findByFlightNumber(String flightNumber) {
        if (flightNumber == null) {
            return Optional.empty();
        }
        String key = flightNumber.trim().toUpperCase();
        return findAll().stream()
                .filter(flight -> flight.getFlightNumber().equalsIgnoreCase(key))
                .findFirst();
    }

    public List<Flight> findAllByDeparture() {
        return findAll().stream()
                .sorted(Comparator.comparing(Flight::getDepartureDateTime))
                .toList();
    }

    public List<Flight> findUpcoming() {
        return findAll().stream()
                .filter(flight -> flight.getDepartureDateTime().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Flight::getDepartureDateTime))
                .toList();
    }
}
