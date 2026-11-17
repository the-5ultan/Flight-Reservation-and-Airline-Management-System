package com.aeroreserve.repository;

import com.aeroreserve.util.FileStorageUtil;

import java.io.IOException;
import java.nio.file.Path;

/** Wires every repository to a single data directory and coordinates flushing. */
public final class RepositoryContainer {

    private final UserRepository users;
    private final AircraftRepository aircraft;
    private final FlightRepository flights;
    private final BookingRepository bookings;
    private final PaymentRepository payments;

    public RepositoryContainer(Path dataDirectory) throws IOException {
        FileStorageUtil.ensureDirectory(dataDirectory);
        this.users = new UserRepository(dataDirectory.resolve("users.ser"));
        this.aircraft = new AircraftRepository(dataDirectory.resolve("aircraft.ser"));
        this.flights = new FlightRepository(dataDirectory.resolve("flights.ser"));
        this.bookings = new BookingRepository(dataDirectory.resolve("bookings.ser"));
        this.payments = new PaymentRepository(dataDirectory.resolve("payments.ser"));
    }

    public UserRepository users() {
        return users;
    }

    public AircraftRepository aircraft() {
        return aircraft;
    }

    public FlightRepository flights() {
        return flights;
    }

    public BookingRepository bookings() {
        return bookings;
    }

    public PaymentRepository payments() {
        return payments;
    }

    public void flushAll() {
        users.flush();
        aircraft.flush();
        flights.flush();
        bookings.flush();
        payments.flush();
    }
}
