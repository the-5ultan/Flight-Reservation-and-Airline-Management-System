package com.aeroreserve.service;

import com.aeroreserve.exception.AircraftUnavailableException;
import com.aeroreserve.model.Aircraft;
import com.aeroreserve.repository.AircraftRepository;
import com.aeroreserve.repository.FlightRepository;
import com.aeroreserve.util.IdGenerator;

import java.util.List;
import java.util.Optional;

/** Aircraft CRUD with configuration validation and safe-deletion rules. */
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;

    public AircraftService(AircraftRepository aircraftRepository, FlightRepository flightRepository) {
        this.aircraftRepository = aircraftRepository;
        this.flightRepository = flightRepository;
    }

    public Aircraft addAircraft(String model, String manufacturer, String registration,
                                int firstRows, String firstColumns,
                                int businessRows, String businessColumns,
                                int economyRows, String economyColumns) {
        if (aircraftRepository.findByRegistration(registration).isPresent()) {
            throw new IllegalArgumentException("Registration number " + registration + " is already in use");
        }
        Aircraft aircraft = new Aircraft(IdGenerator.generate("AC"), model, manufacturer, registration,
                firstRows, firstColumns, businessRows, businessColumns, economyRows, economyColumns);
        aircraftRepository.put(aircraft.getAircraftId(), aircraft);
        return aircraft;
    }

    public void updateAircraft(String aircraftId, String model, String manufacturer, String registration,
                               int firstRows, String firstColumns,
                               int businessRows, String businessColumns,
                               int economyRows, String economyColumns) throws AircraftUnavailableException {
        Aircraft aircraft = getAircraft(aircraftId);
        Optional<Aircraft> duplicate = aircraftRepository.findByRegistration(registration);
        if (duplicate.isPresent() && !duplicate.get().getAircraftId().equals(aircraftId)) {
            throw new AircraftUnavailableException("Registration number " + registration + " is already in use");
        }
        aircraft.setModel(model);
        aircraft.setManufacturer(manufacturer);
        aircraft.setRegistrationNumber(registration);
        aircraft.setCabinConfiguration(firstRows, firstColumns, businessRows, businessColumns,
                economyRows, economyColumns);
        aircraftRepository.put(aircraftId, aircraft);
    }

    public void deleteAircraft(String aircraftId) throws AircraftUnavailableException {
        Aircraft aircraft = getAircraft(aircraftId);
        boolean inUse = flightRepository.findAll().stream()
                .anyMatch(flight -> flight.getAircraftId().equals(aircraftId));
        if (inUse) {
            throw new AircraftUnavailableException(aircraft.getModel()
                    + " is assigned to a flight and cannot be deleted");
        }
        aircraftRepository.remove(aircraftId);
    }

    public Aircraft getAircraft(String aircraftId) throws AircraftUnavailableException {
        return aircraftRepository.findById(aircraftId)
                .orElseThrow(() -> new AircraftUnavailableException("Aircraft not found"));
    }

    public List<Aircraft> getAllAircraft() {
        return aircraftRepository.findAllSorted();
    }
}
