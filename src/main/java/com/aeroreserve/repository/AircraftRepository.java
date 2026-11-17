package com.aeroreserve.repository;

import com.aeroreserve.model.Aircraft;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Aircraft persistence with registration-number uniqueness checks. */
public class AircraftRepository extends SerializedRepository<Aircraft> {

    public AircraftRepository(Path file) {
        super(file);
    }

    public Optional<Aircraft> findByRegistration(String registration) {
        if (registration == null) {
            return Optional.empty();
        }
        String key = registration.trim().toUpperCase();
        return findAll().stream()
                .filter(aircraft -> aircraft.getRegistrationNumber().equalsIgnoreCase(key))
                .findFirst();
    }

    public List<Aircraft> findAllSorted() {
        return findAll().stream()
                .sorted((a, b) -> a.getModel().compareToIgnoreCase(b.getModel()))
                .toList();
    }
}
