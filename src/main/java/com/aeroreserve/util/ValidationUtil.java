package com.aeroreserve.util;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** Shared validation rules for domain input. */
public final class ValidationUtil {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern AIRPORT = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9 ()-]{7,20}$");

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidAirportCode(String code) {
        return code != null && AIRPORT.matcher(code.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone == null || phone.isBlank() || PHONE.matcher(phone.trim()).matches();
    }

    public static boolean isPositive(double value) {
        return Double.isFinite(value) && value > 0;
    }

    public static boolean isValidSchedule(LocalDateTime departure, LocalDateTime arrival) {
        return departure != null && arrival != null && arrival.isAfter(departure);
    }

    /** Throws an IllegalArgumentException when the password policy is not satisfied. */
    public static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain an uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain a lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain a digit");
        }
    }

    public static String requireText(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public static String normaliseAirport(String code) {
        String normalised = requireText(code, "Airport code").toUpperCase();
        if (!isValidAirportCode(normalised)) {
            throw new IllegalArgumentException("Airport code must be three letters, e.g. JFK");
        }
        return normalised;
    }

    public static String normaliseEmail(String email) {
        String normalised = requireText(email, "Email").toLowerCase();
        if (!isValidEmail(normalised)) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        return normalised;
    }
}
