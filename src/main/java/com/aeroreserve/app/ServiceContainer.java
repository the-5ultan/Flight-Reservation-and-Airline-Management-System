package com.aeroreserve.app;

import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.exception.AircraftUnavailableException;
import com.aeroreserve.exception.AuthenticationException;
import com.aeroreserve.exception.DuplicateUserException;
import com.aeroreserve.exception.InvalidFlightScheduleException;
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
import java.time.LocalTime;

/** Composition root: wires repositories, services and first-run demo data. */
public final class ServiceContainer {

    public static final String DEFAULT_ADMIN_EMAIL = "admin@aeroreserve.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "AdminPass123";
    public static final String DEMO_PASSENGER_EMAIL = "passenger@aeroreserve.com";
    public static final String DEMO_PASSENGER_PASSWORD = "Passenger123";

    private static ServiceContainer instance;

    private final RepositoryContainer repositories;
    private final AuthenticationService authentication;
    private final AircraftService aircraft;
    private final FlightService flights;
    private final SeatAllocationService seats;
    private final PaymentService payments;
    private final BookingService bookings;
    private final ReportService reports;

    public ServiceContainer(Path dataDirectory) throws IOException {
        repositories = new RepositoryContainer(dataDirectory);
        authentication = new AuthenticationService(repositories.users());
        aircraft = new AircraftService(repositories.aircraft(), repositories.flights());
        flights = new FlightService(repositories.flights(), repositories.aircraft(),
                repositories.bookings(), repositories.payments(), repositories.users());
        seats = new SeatAllocationService(repositories.flights());
        payments = new PaymentService(repositories.payments());
        bookings = new BookingService(repositories.bookings(), repositories.users(),
                flights, seats, payments);
        reports = new ReportService(repositories.flights(), repositories.bookings(),
                repositories.payments(), repositories.users());
        seedFirstRunData();
    }

    public static synchronized void initialize(Path dataDirectory) throws IOException {
        instance = new ServiceContainer(dataDirectory);
    }

    public static ServiceContainer get() {
        if (instance == null) {
            throw new IllegalStateException("Services have not been initialised");
        }
        return instance;
    }

    public AuthenticationService auth() {
        return authentication;
    }

    public AircraftService aircraftService() {
        return aircraft;
    }

    public FlightService flightService() {
        return flights;
    }

    public SeatAllocationService seatService() {
        return seats;
    }

    public PaymentService paymentService() {
        return payments;
    }

    public BookingService bookingService() {
        return bookings;
    }

    public ReportService reportService() {
        return reports;
    }

    public RepositoryContainer repositories() {
        return repositories;
    }

    public void flushAll() {
        repositories.flushAll();
    }

    private void seedFirstRunData() throws IOException {
        boolean needsSave = false;

        if (!repositories.users().existsEmail(DEFAULT_ADMIN_EMAIL)) {
            try {
                authentication.ensureDefaultAdmin(DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD, "Aero Administrator");
                authentication.ensureDefaultAdmin("ops@aeroreserve.com", "OpsPass123", "Flight Operations");
                authentication.registerPassenger("Demo Passenger", DEMO_PASSENGER_EMAIL,
                        DEMO_PASSENGER_PASSWORD, "+1 555 0100", "X1234567", "British");
                needsSave = true;
            } catch (DuplicateUserException | AuthenticationException e) {
                throw new IOException("Could not seed default users", e);
            }
        }

        if (repositories.aircraft().size() == 0) {
            try {
                aircraft.addAircraft("Airbus A320neo", "Airbus", "AR-A320-01",
                        0, "AF", 2, "ACDF", 12, "ABCDEF");
                aircraft.addAircraft("Boeing 787-9 Dreamliner", "Boeing", "AR-B787-02",
                        2, "AF", 4, "ACDF", 16, "ABCDEF");
                aircraft.addAircraft("Airbus A321neo", "Airbus", "AR-A321-03",
                        0, "AF", 3, "ACDF", 16, "ABCDEF");
                needsSave = true;
            } catch (RuntimeException e) {
                throw new IOException("Could not seed aircraft", e);
            }
        }

        if (repositories.flights().size() == 0 && repositories.aircraft().size() >= 3) {
            try {
                var fleet = repositories.aircraft().findAllSorted();
                String a320 = fleet.stream().filter(a -> a.getModel().contains("A320")).findFirst().orElse(fleet.get(0)).getAircraftId();
                String b787 = fleet.stream().filter(a -> a.getModel().contains("787")).findFirst().orElse(fleet.get(0)).getAircraftId();
                String a321 = fleet.stream().filter(a -> a.getModel().contains("A321")).findFirst().orElse(fleet.get(0)).getAircraftId();

                flights.createFlight("AR201", a320, "JFK", "LHR", at(3, 9, 30), at(3, 21, 45), 420.0, FlightStatus.SCHEDULED);
                flights.createFlight("AR318", a321, "LHR", "CDG", at(5, 14, 15), at(5, 15, 40), 180.0, FlightStatus.SCHEDULED);
                flights.createFlight("AR445", b787, "DXB", "SIN", at(7, 2, 20), at(7, 13, 35), 610.0, FlightStatus.SCHEDULED);
                flights.createFlight("AR520", b787, "CDG", "JFK", at(10, 10, 0), at(10, 13, 15), 540.0, FlightStatus.SCHEDULED);
                flights.createFlight("AR610", a321, "SIN", "SYD", at(14, 8, 45), at(14, 18, 30), 390.0, FlightStatus.SCHEDULED);
                needsSave = true;
            } catch (InvalidFlightScheduleException | AircraftUnavailableException e) {
                throw new IOException("Could not seed flights", e);
            }
        }

        if (needsSave) {
            repositories.flushAll();
        }
    }

    private static LocalDateTime at(int daysAhead, int hour, int minute) {
        return LocalDateTime.of(LocalDateTime.now().toLocalDate().plusDays(daysAhead), LocalTime.of(hour, minute));
    }
}
