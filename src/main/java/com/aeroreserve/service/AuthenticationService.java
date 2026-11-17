package com.aeroreserve.service;

import com.aeroreserve.enums.UserRole;
import com.aeroreserve.exception.AuthenticationException;
import com.aeroreserve.exception.DuplicateUserException;
import com.aeroreserve.exception.UnauthorizedActionException;
import com.aeroreserve.model.Admin;
import com.aeroreserve.model.Passenger;
import com.aeroreserve.model.User;
import com.aeroreserve.repository.UserRepository;
import com.aeroreserve.util.IdGenerator;
import com.aeroreserve.util.PasswordUtil;
import com.aeroreserve.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/** Authentication, registration, profile and role-based authorization. */
public class AuthenticationService {

    private final UserRepository users;
    private User currentUser;

    public AuthenticationService(UserRepository users) {
        this.users = users;
    }

    public Passenger registerPassenger(String fullName, String email, String password,
                                       String phone, String passportNumber, String nationality)
            throws DuplicateUserException, AuthenticationException {
        String normalisedEmail = ValidationUtil.normaliseEmail(email);
        if (users.existsEmail(normalisedEmail)) {
            throw new DuplicateUserException("An account already exists for " + normalisedEmail);
        }
        try {
            ValidationUtil.validatePassword(password);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException(e.getMessage());
        }
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new AuthenticationException("Enter a valid phone number");
        }
        Passenger passenger = new Passenger(IdGenerator.generate("USR"), fullName, normalisedEmail,
                PasswordUtil.hash(password), phone, passportNumber, nationality);
        users.add(passenger);
        return passenger;
    }

    public User login(String email, String password) throws AuthenticationException {
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            throw new AuthenticationException("No account found for that email address");
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated");
        }
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Incorrect password");
        }
        user.recordLogin();
        users.put(user.getUserId(), user);
        currentUser = user;
        return user;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Authorization is enforced here, not only by hiding UI controls. */
    public void requireRole(UserRole role) throws UnauthorizedActionException {
        if (currentUser == null) {
            throw new UnauthorizedActionException("You must sign in to continue");
        }
        if (currentUser.getRole() != role || !currentUser.isActive()) {
            throw new UnauthorizedActionException("This action requires " + role.getDisplayName() + " privileges");
        }
    }

    public void requireAdmin() throws UnauthorizedActionException {
        requireRole(UserRole.ADMIN);
    }

    public void requirePassenger() throws UnauthorizedActionException {
        requireRole(UserRole.PASSENGER);
    }

    public void changePassword(String currentPassword, String newPassword) throws AuthenticationException {
        if (currentUser == null) {
            throw new AuthenticationException("You must sign in to change your password");
        }
        if (!PasswordUtil.verify(currentPassword, currentUser.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        try {
            ValidationUtil.validatePassword(newPassword);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException(e.getMessage());
        }
        currentUser.setPasswordHash(PasswordUtil.hash(newPassword));
        users.put(currentUser.getUserId(), currentUser);
    }

    public void updateProfile(String fullName, String phone, String passportNumber, String nationality)
            throws AuthenticationException {
        if (currentUser == null) {
            throw new AuthenticationException("You must sign in to update your profile");
        }
        try {
            currentUser.setFullName(fullName);
            currentUser.setPhone(phone);
            if (currentUser instanceof Passenger passenger) {
                passenger.setPassportNumber(passportNumber);
                passenger.setNationality(nationality);
            }
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException(e.getMessage());
        }
        users.put(currentUser.getUserId(), currentUser);
    }

    public void ensureDefaultAdmin(String email, String password, String fullName) {
        if (users.existsEmail(email)) {
            return;
        }
        Admin admin = new Admin(IdGenerator.generate("USR"), fullName, email,
                PasswordUtil.hash(password), "", "Operations", "ADM-001");
        try {
            users.add(admin);
        } catch (DuplicateUserException e) {
            throw new IllegalStateException("Duplicate administrator account", e);
        }
    }

    public List<User> getAllUsers() {
        return users.findAll();
    }
}
