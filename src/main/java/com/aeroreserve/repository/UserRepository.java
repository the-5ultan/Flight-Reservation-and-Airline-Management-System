package com.aeroreserve.repository;

import com.aeroreserve.exception.DuplicateUserException;
import com.aeroreserve.model.User;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Stores all users and provides email-based lookup for authentication. */
public class UserRepository extends SerializedRepository<User> {

    public UserRepository(Path file) {
        super(file);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        return findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(key))
                .findFirst();
    }

    public boolean existsEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public void add(User user) throws DuplicateUserException {
        if (existsEmail(user.getEmail())) {
            throw new DuplicateUserException("An account already exists for " + user.getEmail());
        }
        put(user.getUserId(), user);
    }

    public List<User> findAllPassengers() {
        return findAll().stream()
                .filter(user -> user.getRole() == com.aeroreserve.enums.UserRole.PASSENGER)
                .toList();
    }
}
