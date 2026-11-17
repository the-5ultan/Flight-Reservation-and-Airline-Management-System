package com.aeroreserve.model;

import com.aeroreserve.enums.SeatClass;
import com.aeroreserve.generic.CustomList;
import com.aeroreserve.util.ValidationUtil;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Aircraft type with its own configurable cabin layout and generated seat template. */
public class Aircraft implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String aircraftId;
    private String model;
    private String manufacturer;
    private String registrationNumber;
    private int firstRows;
    private int businessRows;
    private int economyRows;
    private String firstColumns;
    private String businessColumns;
    private String economyColumns;
    private final CustomList<Seat> seatTemplate = new CustomList<>();

    public Aircraft(String aircraftId, String model, String manufacturer, String registrationNumber,
                    int firstRows, String firstColumns, int businessRows, String businessColumns,
                    int economyRows, String economyColumns) {
        this.aircraftId = ValidationUtil.requireText(aircraftId, "Aircraft ID");
        setModel(model);
        setManufacturer(manufacturer);
        setRegistrationNumber(registrationNumber);
        setCabinConfiguration(firstRows, firstColumns, businessRows, businessColumns,
                economyRows, economyColumns);
        generateSeats();
    }

    public final void setCabinConfiguration(int firstRows, String firstColumns,
                                            int businessRows, String businessColumns,
                                            int economyRows, String economyColumns) {
        if (firstRows < 0 || businessRows < 0 || economyRows < 1) {
            throw new IllegalArgumentException("Economy must have at least one row; other rows cannot be negative");
        }
        this.firstColumns = validateColumns(firstColumns, "First class columns", firstRows);
        this.businessColumns = validateColumns(businessColumns, "Business columns", businessRows);
        this.economyColumns = validateColumns(economyColumns, "Economy columns", economyRows);
        this.firstRows = firstRows;
        this.businessRows = businessRows;
        this.economyRows = economyRows;
        generateSeats();
    }

    private static String validateColumns(String columns, String field, int rows) {
        if (rows == 0) {
            return columns == null ? "" : columns.trim().toUpperCase();
        }
        String value = ValidationUtil.requireText(columns, field).toUpperCase();
        if (!value.matches("[A-H]{2,8}")) {
            throw new IllegalArgumentException(field + " must contain 2-8 distinct letters A-H");
        }
        Set<Character> unique = new HashSet<>();
        for (char c : value.toCharArray()) {
            if (!unique.add(c)) {
                throw new IllegalArgumentException(field + " contains duplicate letters");
            }
        }
        return value;
    }

    /** Rebuilds the seat template from the current configuration. */
    public final void generateSeats() {
        seatTemplate.clear();
        int startRow = 1;
        startRow = addSeats(SeatClass.FIRST, firstRows, firstColumns, startRow);
        startRow = addSeats(SeatClass.BUSINESS, businessRows, businessColumns, startRow);
        addSeats(SeatClass.ECONOMY, economyRows, economyColumns, startRow);
    }

    private int addSeats(SeatClass seatClass, int rows, String columns, int startRow) {
        for (int row = startRow; row < startRow + rows; row++) {
            for (int column = 0; column < columns.length(); column++) {
                seatTemplate.add(new Seat(row, String.valueOf(columns.charAt(column)), seatClass, columns));
            }
        }
        return startRow + rows;
    }

    /** Creates independent seat instances for a flight assigned to this aircraft. */
    public CustomList<Seat> createFlightSeats() {
        CustomList<Seat> seats = new CustomList<>();
        for (Seat template : seatTemplate) {
            seats.add(new Seat(template.getRow(), template.getColumn(),
                    template.getSeatClass(), columnsFor(template.getSeatClass())));
        }
        return seats;
    }

    private String columnsFor(SeatClass seatClass) {
        return switch (seatClass) {
            case FIRST -> firstColumns;
            case BUSINESS -> businessColumns;
            case ECONOMY -> economyColumns;
        };
    }

    public int getFirstCapacity() {
        return firstRows * firstColumns.length();
    }

    public int getBusinessCapacity() {
        return businessRows * businessColumns.length();
    }

    public int getEconomyCapacity() {
        return economyRows * economyColumns.length();
    }

    public int getTotalCapacity() {
        return getFirstCapacity() + getBusinessCapacity() + getEconomyCapacity();
    }

    public String getLayoutSummary() {
        return "F " + getFirstCapacity() + " · B " + getBusinessCapacity()
                + " · E " + getEconomyCapacity() + " · Total " + getTotalCapacity();
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = ValidationUtil.requireText(model, "Model");
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = ValidationUtil.requireText(manufacturer, "Manufacturer");
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = ValidationUtil.requireText(registrationNumber, "Registration number").toUpperCase();
    }

    public int getFirstRows() {
        return firstRows;
    }

    public int getBusinessRows() {
        return businessRows;
    }

    public int getEconomyRows() {
        return economyRows;
    }

    public String getFirstColumns() {
        return firstColumns;
    }

    public String getBusinessColumns() {
        return businessColumns;
    }

    public String getEconomyColumns() {
        return economyColumns;
    }

    public CustomList<Seat> getSeatTemplate() {
        return new CustomList<>(seatTemplate.toList());
    }

    @Override
    public String toString() {
        return model + " (" + registrationNumber + ")";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Aircraft aircraft && aircraftId.equals(aircraft.aircraftId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aircraftId);
    }
}
