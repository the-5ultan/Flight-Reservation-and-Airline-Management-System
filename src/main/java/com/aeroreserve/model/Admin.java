package com.aeroreserve.model;

import com.aeroreserve.enums.UserRole;
import com.aeroreserve.util.ValidationUtil;

/** An airline staff member with administrative privileges. */
public class Admin extends User {

    private static final long serialVersionUID = 1L;

    private String department;
    private String staffId;

    public Admin(String userId, String fullName, String email, String passwordHash,
                 String phone, String department, String staffId) {
        super(userId, fullName, email, passwordHash, UserRole.ADMIN, phone);
        this.department = ValidationUtil.isBlank(department) ? "Operations" : department.trim();
        this.staffId = staffId == null ? "" : staffId.trim();
    }

    @Override
    public String getDisplayName() {
        return getFullName() + " (Admin)";
    }

    @Override
    public boolean canManageFlights() {
        return true;
    }

    @Override
    public boolean canBookFlights() {
        return false;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = ValidationUtil.requireText(department, "Department");
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId == null ? "" : staffId.trim();
    }
}
