package com.aeroreserve;

import com.aeroreserve.enums.BookingStatus;
import com.aeroreserve.enums.FlightStatus;
import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.model.Booking;
import com.aeroreserve.model.FlexibleCancellationPolicy;
import com.aeroreserve.model.Flight;
import com.aeroreserve.model.StandardCancellationPolicy;
import com.aeroreserve.model.Ticket;
import com.aeroreserve.repository.SerializedRepository;
import com.aeroreserve.util.PasswordUtil;
import com.aeroreserve.util.ValidationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreAndModelTest {

    @TempDir
    Path tempDir;

    @Test
    void passwordHashIsSaltedAndVerifiable() {
        String hash = PasswordUtil.hash("Valid1Pass");
        assertEquals(3, hash.split(":").length);
        assertTrue(PasswordUtil.verify("Valid1Pass", hash));
        assertFalse(PasswordUtil.verify("Wrong1Pass", hash));
        assertFalse(PasswordUtil.verify("Valid1Pass", null));
        assertFalse(PasswordUtil.verify(null, hash));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
    }

    @Test
    void validationRulesRejectWeakPasswordsAndBadCodes() {
        assertEquals("Password must contain at least 8 characters",
                assertThrows(IllegalArgumentException.class,
                        () -> ValidationUtil.validatePassword("Abc1")).getMessage());
        assertEquals("Password must contain an uppercase letter",
                assertThrows(IllegalArgumentException.class,
                        () -> ValidationUtil.validatePassword("password1")).getMessage());
        assertEquals("Password must contain a lowercase letter",
                assertThrows(IllegalArgumentException.class,
                        () -> ValidationUtil.validatePassword("PASSWORD1")).getMessage());
        assertEquals("Password must contain a digit",
                assertThrows(IllegalArgumentException.class,
                        () -> ValidationUtil.validatePassword("Password")).getMessage());
        assertEquals("user@example.com", ValidationUtil.normaliseEmail("  User@Example.COM "));
        assertEquals("JFK", ValidationUtil.normaliseAirport(" jfk "));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.normaliseAirport("JJ"));
        assertFalse(ValidationUtil.isPositive(Double.NaN));
        assertFalse(ValidationUtil.isPositive(0));
    }

    @Test
    void customListSupportsMutationFilteringAndSafeSnapshots() {
        CustomList<String> list = new CustomList<>();
        list.add("alpha");
        list.add("beta");
        list.add("alpha");

        assertEquals(3, list.size());
        assertTrue(list.remove("alpha"));
        assertEquals("beta", list.get(0));
        assertEquals(1, list.indexOf("alpha"));

        CustomList<String> filtered = list.filter(value -> value.equals("alpha"));
        assertEquals(1, filtered.size());
        assertEquals(2, list.size());

        List<String> snapshot = list.toList();
        snapshot.clear();
        assertEquals(2, list.size());
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(99));
    }

    @Test
    void serializedRepositoryRoundTripsEntities() {
        Path file = tempDir.resolve("items.ser");
        SerializedRepository<String> repository = new SerializedRepository<>(file);
        assertTrue(repository.findAll().isEmpty());

        repository.put("a", "alpha");
        assertTrue(Files.exists(file));
        assertEquals(Optional.of("alpha"), new SerializedRepository<String>(file).findById("a"));

        assertTrue(repository.remove("a"));
        assertFalse(repository.exists("a"));
        assertFalse(repository.remove("a"));
    }

    @Test
    void ticketCopiesSeatsAndRendersBothFormats() {
        CustomList<String> seats = new CustomList<>(List.of("3A", "3B"));
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket("TKT-1", "BK-1", "<script>&", "p@example.com", "AR201",
                "JFK", "LHR", now, now.plusHours(7), seats, SeatClass.ECONOMY,
                100.0, 19.5, BookingStatus.CONFIRMED);
        seats.add("9Z");

        assertEquals("3A, 3B", ticket.getSeatList());
        assertEquals(119.5, ticket.getTotalFare(), 0.001);
        assertTrue(ticket.toText().contains("AERORESERVE ELECTRONIC TICKET"));
        assertTrue(ticket.toHtml().contains("&lt;script&gt;&amp;"));
    }

    @Test
    void cancellationPoliciesApplyTheirTiers() throws Exception {
        CustomList<String> seats = new CustomList<>(List.of("3A"));
        Booking booking = new Booking("BK-1", "USR-1", "FL-1", seats, SeatClass.ECONOMY,
                100.0, "Test Passenger", "p@example.com", "AR201");
        LocalDateTime now = LocalDateTime.now();
        Flight farFlight = new Flight("FL-1", "AR201", "AC-1", "JFK", "LHR",
                now.plusDays(10), now.plusDays(10).plusHours(7), 400.0, FlightStatus.SCHEDULED);
        Flight soonFlight = new Flight("FL-2", "AR202", "AC-1", "JFK", "LHR",
                now.plusHours(12), now.plusHours(14), 400.0, FlightStatus.SCHEDULED);

        assertEquals(11.95, new StandardCancellationPolicy().calculateFee(booking, farFlight), 0.001);
        assertEquals(0.0, new FlexibleCancellationPolicy().calculateFee(booking, farFlight), 0.001);
        assertEquals(71.7, new StandardCancellationPolicy().calculateFee(booking, soonFlight), 0.001);
        assertEquals(11.95, new FlexibleCancellationPolicy().calculateFee(booking, soonFlight), 0.001);
    }
}
