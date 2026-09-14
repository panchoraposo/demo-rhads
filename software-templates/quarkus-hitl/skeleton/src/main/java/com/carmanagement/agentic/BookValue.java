package com.carmanagement.agentic;

import com.carmanagement.model.CarInfo;

/**
 * Pre-incident book value so HITL does not depend on the LLM under-pricing a wreck.
 */
public final class BookValue {

    private BookValue() {
    }

    public static int currentYear() {
        return java.time.Year.now().getValue();
    }

    public static int age(Integer year) {
        int modelYear = year == null ? 0 : year;
        return Math.max(0, currentYear() - modelYear);
    }

    public static String estimate(CarInfo car) {
        if (car == null) {
            return "$15,000";
        }
        return estimate(car.make, car.model, car.year == null ? 0 : car.year);
    }

    public static String estimate(String make, String model, int year) {
        int age = age(year);
        String brand = make == null ? "" : make.toLowerCase();
        String name = model == null ? "" : model.toLowerCase();
        int value;
        if (brand.contains("mercedes") || brand.contains("bmw") || brand.contains("audi")) {
            value = Math.max(40_000, 60_000 - age * 5_000);
        } else if (name.contains("focus") && year <= 2016) {
            value = Math.min(8_000, Math.max(2_500, 9_000 - age * 400));
        } else if (brand.contains("honda") && name.contains("civic")) {
            value = Math.max(18_000, 28_000 - age * 2_000);
        } else if (name.contains("f-150") || name.contains("f150")) {
            value = Math.max(25_000, 50_000 - age * 4_000);
        } else {
            value = Math.max(8_000, 25_000 - age * 2_000);
        }
        return "$" + String.format("%,d", value);
    }
}
