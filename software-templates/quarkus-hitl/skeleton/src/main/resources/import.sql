-- Seed fleet. Explicit ids so Civic is #7 and the 12-year-old Ford Focus is #5
-- (same as the Miles of Smiles HITL demo). H2-compatible (no Postgres nextval).
INSERT INTO car_info (id, make, model, year, condition, status) VALUES
    (1, 'Mercedes-Benz', 'C-Class', 2024, 'Minor dent on passenger door', 'RENTED'),
    (2, 'BMW', 'X5', 2025, 'Recently serviced, excellent condition', 'AT_MAINTENANCE'),
    (3, 'Audi', 'Q4', 2025, 'Brake pads recently replaced', 'RENTED'),
    (4, 'Nissan', 'Altima', 2018, 'Interior needs cleaning', 'AT_CLEANING'),
    (5, 'Ford', 'Focus', 2014, 'High mileage, engine issues', 'RENTED'),
    (6, 'Toyota', 'Corolla', 2023, 'Like new, no issues', 'RENTED'),
    (7, 'Honda', 'Civic', 2022, 'Good condition, minor wear and tear', 'RENTED'),
    (8, 'Ford', 'F-150', 2024, 'Small scratch on rear bumper', 'AT_MAINTENANCE');
