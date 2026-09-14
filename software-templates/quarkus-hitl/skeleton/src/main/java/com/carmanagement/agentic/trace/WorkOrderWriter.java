package com.carmanagement.agentic.trace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;

/**
 * Writes the same department JSON files as Camel {@code file:work-orders}.
 */
final class WorkOrderWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private WorkOrderWriter() {
    }

    static void cleaning(Integer carNumber, Map<String, Object> car, Map<String, Object> intake) {
        write("cleaning", departmentOrder(carNumber, car, intake, "cleaning", "cleaningServices", "cleaningNotes"));
    }

    static void maintenance(Integer carNumber, Map<String, Object> car, Map<String, Object> intake) {
        write("maintenance", departmentOrder(carNumber, car, intake, "maintenance", "maintenanceServices", "maintenanceNotes"));
    }

    static void disposition(Integer carNumber, Map<String, Object> car, Map<String, Object> intake) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("department", "disposition");
        order.put("carNumber", carNumber);
        order.put("make", car.get("make"));
        order.put("model", car.get("model"));
        order.put("year", car.get("year"));
        order.put("action", intake.get("dispositionAction"));
        order.put("estimatedValue", intake.get("carValue"));
        order.put("notes", intake.get("dispositionNotes"));
        order.put("requestedAt", Instant.now().toString());
        write("disposition", order);
    }

    private static Map<String, Object> departmentOrder(
            Integer carNumber,
            Map<String, Object> car,
            Map<String, Object> intake,
            String department,
            String servicesKey,
            String notesKey) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("department", department);
        order.put("carNumber", carNumber);
        order.put("make", car.get("make"));
        order.put("model", car.get("model"));
        order.put("year", car.get("year"));
        order.put("services", intake.getOrDefault(servicesKey, List.of()));
        order.put("notes", intake.get(notesKey));
        order.put("requestedAt", Instant.now().toString());
        return order;
    }

    private static void write(String department, Map<String, Object> order) {
        try {
            Path dir = Path.of("work-orders");
            Files.createDirectories(dir);
            String fileName = department + "-car-" + order.get("carNumber") + "-"
                    + LocalDateTime.now().format(FILE_TS) + ".json";
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(dir.resolve(fileName).toFile(), order);
        } catch (IOException e) {
            Log.warn("Could not write work-order: " + e.getMessage());
        }
    }
}
