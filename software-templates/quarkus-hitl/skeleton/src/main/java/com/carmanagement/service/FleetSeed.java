package com.carmanagement.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import com.carmanagement.model.CarInfo;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;

/**
 * H2 is in-memory. Each process start (cluster Recreate, quarkus:dev restart)
 * reloads {@code import.sql}. Log that so a demo rollout is obviously a clean fleet.
 */
@ApplicationScoped
public class FleetSeed {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        long cars = CarInfo.count();
        Log.debugf("Fleet reseeded from import.sql (%d cars). In-memory H2 resets on every deploy.", cars);
    }
}
