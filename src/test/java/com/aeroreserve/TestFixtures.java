package com.aeroreserve;

import com.aeroreserve.model.Aircraft;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.repository.RepositoryContainer;
import com.aeroreserve.service.AircraftService;
import com.aeroreserve.service.AuthenticationService;
import com.aeroreserve.service.BookingService;
import com.aeroreserve.service.FlightService;
import com.aeroreserve.service.PaymentService;
import com.aeroreserve.service.ReportService;
import com.aeroreserve.service.SeatAllocationService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

final class TestFixtures {

    private TestFixtures() {
    }

    static Env env(Path directory) throws IOException {
        return new Env(directory);
    }

    static Passenger passenger(Env env, String email) throws Exception {
        return env.auth.registerPassenger("Test Passenger", email, "Passw0rd!", "+1 555 0100",
                "P-" + UUID.randomUUID(), "Testland");
    }

    static Aircraft aircraft(Env env) throws Exception {
        return env.aircraft.addAircraft("A320", "Airbus", "TEST-" + UUID.randomUUID(),
                1, "AC", 1, "AC", 3, "ABCDEF");
    }

    static Flight flight(Env env, String number, String from, String to, int daysAhead) throws Exception {
        Aircraft aircraft = aircraft(env);
        LocalDateTime departure = LocalDateTime.now().plusDays(daysAhead);
        return env.flights.createFlight(number, aircraft.getAircraftId(), from, to,
                departure, departure.plusHours(2), 400.0, com.aeroreserve.enums.FlightStatus.SCHEDULED);
    }

    static final class Env {
        final Path directory;
        final RepositoryContainer repositories;
        final AuthenticationService auth;
        final AircraftService aircraft;
        final FlightService flights;
        final SeatAllocationService seats;
        final PaymentService payments;
        final BookingService bookings;
        final ReportService reports;

        Env(Path directory) throws IOException {
            this.directory = directory;
            this.repositories = new RepositoryContainer(directory);
            this.auth = new AuthenticationService(repositories.users());
            this.aircraft = new AircraftService(repositories.aircraft(), repositories.flights());
            this.flights = new FlightService(repositories.flights(), repositories.aircraft(),
                    repositories.bookings(), repositories.payments(), repositories.users());
            this.seats = new SeatAllocationService(repositories.flights());
            this.payments = new PaymentService(repositories.payments());
            this.bookings = new BookingService(repositories.bookings(), repositories.users(),
                    flights, seats, payments);
            this.reports = new ReportService(repositories.flights(), repositories.bookings(),
                    repositories.payments(), repositories.users());
        }

        void flush() {
            repositories.flushAll();
        }
    }
}
