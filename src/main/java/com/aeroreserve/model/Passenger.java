package com.aeroreserve.model;

import com.aeroreserve.enums.UserRole;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** A customer who can search flights and create reservations. */
public class Passenger extends User {

    private static final long serialVersionUID = 1L;

    private String passportNumber;
    private String nationality;
    private LocalDate dateOfBirth;
    private final Set<String> bookingIds = new HashSet<>();

    public Passenger(String userId, String fullName, String email, String passwordHash,
                     String phone, String passportNumber, String nationality) {
        super(userId, fullName, email, passwordHash, UserRole.PASSENGER, phone);
        this.passportNumber = passportNumber == null ? "" : passportNumber.trim();
        this.nationality = nationality == null ? "" : nationality.trim();
    }

    @Override
    public String getDisplayName() {
        return getFullName();
    }

    @Override
    public boolean canManageFlights() {
        return false;
    }

    @Override
    public boolean canBookFlights() {
        return true;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber == null ? "" : passportNumber.trim();
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality == null ? "" : nationality.trim();
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        this.dateOfBirth = dateOfBirth;
    }

    public Set<String> getBookingIds() {
        return bookingIds;
    }

    public void addBookingId(String bookingId) {
        if (bookingId != null) {
            bookingIds.add(bookingId);
        }
    }

    public void removeBookingId(String bookingId) {
        bookingIds.remove(bookingId);
    }
}
