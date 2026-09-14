package com.carmanagement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.BindToRegistry;

@BindToRegistry("fleetStore")
public class FleetStore {

    private final Map<Integer, Map<String, Object>> cars = new ConcurrentHashMap<>();

    public FleetStore() {
        seed();
    }

    public synchronized List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>(cars.values());
        result.sort((a, b) -> Integer.compare((Integer) a.get("id"), (Integer) b.get("id")));
        return result;
    }

    public Map<String, Object> find(Object carNumber) {
        Map<String, Object> car = cars.get(toId(carNumber));
        if (car == null) {
            throw new IllegalArgumentException("Car not found: " + carNumber);
        }
        return car;
    }

    public synchronized Map<String, Object> applyIntake(Object carNumber, Map<String, Object> intake) {
        Map<String, Object> car = find(carNumber);
        if (intake != null && intake.get("condition") != null) {
            car.put("condition", String.valueOf(intake.get("condition")));
        }
        car.put("status", WorkflowSupport.statusFor(intake));
        return car;
    }

    public synchronized void reset() {
        cars.clear();
        seed();
    }

    private void seed() {
        int year = java.time.Year.now().getValue();
        put(1, "Mercedes-Benz", "C-Class", year - 2, "RENTED", "Minor dent on passenger door");
        put(2, "BMW", "X5", year - 1, "AT_MAINTENANCE", "Recently serviced, excellent condition");
        put(3, "Audi", "Q4", year - 1, "RENTED", "Brake pads recently replaced");
        put(4, "Nissan", "Altima", year - 8, "AT_CLEANING", "Interior needs cleaning");
        put(5, "Ford", "Focus", year - 12, "RENTED", "High mileage, engine issues");
        put(6, "Toyota", "Corolla", year - 3, "RENTED", "Like new, no issues");
        put(7, "Honda", "Civic", year - 4, "RENTED", "Good condition, minor wear and tear");
        put(8, "Ford", "F-150", year - 2, "AT_MAINTENANCE", "Small scratch on rear bumper");
    }

    private void put(int id, String make, String model, int year, String status, String condition) {
        Map<String, Object> car = new LinkedHashMap<>();
        car.put("id", id);
        car.put("make", make);
        car.put("model", model);
        car.put("year", year);
        car.put("status", status);
        car.put("condition", condition);
        cars.put(id, car);
    }

    private static int toId(Object carNumber) {
        return Integer.parseInt(String.valueOf(carNumber));
    }
}
