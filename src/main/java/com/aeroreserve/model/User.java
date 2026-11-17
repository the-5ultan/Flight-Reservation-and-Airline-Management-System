package com.aeroreserve.model;

import com.aeroreserve.enums.UserRole;
import com.aeroreserve.util.ValidationUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Abstract base for every authenticated person in the system. */
public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String email;
    private final UserRole role;
    private final LocalDateTime createdAt;

    private String fullName;
    private String passwordHash;
    private String phone;
    private boolean active = true;
    private LocalDateTime lastLoginAt;

    protected User(String userId, String fullName, String email, String passwordHash,
                   UserRole role, String phone) {
        this.userId = ValidationUtil.requireText(userId, "User ID");
        this.email = ValidationUtil.normaliseEmail(email);
        this.role = Objects.requireNonNull(role, "role");
        this.fullName = ValidationUtil.requireText(fullName, "Full name");
        if (ValidationUtil.isBlank(passwordHash)) {
            throw new IllegalArgumentException("Password hash is required");
        }
        this.passwordHash = passwordHash;
        this.phone = phone == null ? "" : phone.trim();
        this.createdAt = LocalDateTime.now();
    }

    public abstract String getDisplayName();

    public abstract boolean canManageFlights();

    public abstract boolean canBookFlights();

    public String getInitials() {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.toString();
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = ValidationUtil.requireText(fullName, "Full name");
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (ValidationUtil.isBlank(passwordHash)) {
            throw new IllegalArgumentException("Password hash is required");
        }
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("Enter a valid phone number");
        }
        this.phone = phone == null ? "" : phone.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return fullName + " <" + email + ">";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
