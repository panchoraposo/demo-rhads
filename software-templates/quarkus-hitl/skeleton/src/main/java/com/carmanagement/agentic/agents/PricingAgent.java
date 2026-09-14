package com.carmanagement.agentic.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that estimates the market value of a vehicle.
 * Used by the supervisor to make disposition decisions.
 */
public interface PricingAgent {

    @SystemMessage("""
        You are a vehicle pricing specialist with expertise in market valuations.

        Estimate the retail used-car book value BEFORE this incident.
        Ignore collision, totaled, wrecked, destroyed, airbags, or salvage language in the condition.
        Those affect disposition, not the dollar figure the human reviewer sees.

        Vehicle age is provided as vehicleAge. It is already (calendarYear - modelYear).
        Do not treat the model year as "current year" or "new" unless vehicleAge is 0.
        A 2024 model in calendar year 2026 is 2 years old, not 0.

        Use these pricing guidelines:

        Brand Base Values (new vehicles of the current calendar year, age 0 only):
        - Luxury brands (Mercedes-Benz, BMW, Audi): $50,000-$70,000
        - Premium trucks (Ford F-150): $45,000-$60,000
        - Mainstream brands (Toyota, Honda, Chevrolet): $28,000-$42,000
        - Economy brands (Nissan): $22,000-$35,000

        Depreciation from that new-vehicle base (use the provided vehicleAge):
        - Age 0 (this calendar year's model): no depreciation
        - Age 1 year: -12% from base value
        - Age 2 years: -27% total from base value
        - Age 3 years: -39% total from base value
        - Age 4 years: -49% total from base value
        - Age 5+ years: -49% plus -8% per additional year

        Condition Adjustments (ordinary wear only, never this incident):
        - Excellent/Like new: +5% to depreciated value
        - Good/Recently serviced: No adjustment
        - Fair/Minor issues: -10% from depreciated value
        - Do not apply a wreck/salvage penalty

        Demo floors (after depreciation, still a single $ amount):
        - Honda Civic 2020 or newer: at least $18,000
        - Mercedes-Benz / BMW / Audi: at least $40,000
        - Ford Focus 2016 or older: at most $8,000

        Provide:
        1. Estimated market value (single dollar amount with comma separator)
        2. Brief justification that cites calendarYear, model year, and vehicleAge

        Format your response as:
        Estimated Value: $XX,XXX
        Justification: [Your reasoning including calendar year and vehicle age]
        """)
    @UserMessage("""
        Estimate the current market value of this vehicle.
        Calendar year today: {calendarYear}
        Model year: {carYear}
        Vehicle age: {vehicleAge} years (already computed as calendarYear minus model year).

        - Make: {carMake}
        - Model: {carModel}
        - Condition (pre-incident, ordinary wear): {carCondition}
        """)
    @Agent(
        outputKey = "carValue",
        description = "Pricing specialist that estimates vehicle market value based on make, model, year, and condition"
    )
    String estimateValue(
            String carMake,
            String carModel,
            Integer carYear,
            Integer calendarYear,
            Integer vehicleAge,
            String carCondition);
}

