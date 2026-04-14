package dev.stat.chat.service;

import dev.stat.chat.domain.HealthData;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default InfluxDB-backed implementation of HealthDataClient.
 * Stub for now - will be implemented when InfluxDB integration is built.
 */
@ApplicationScoped
public class DefaultHealthDataClient implements HealthDataClient {

    @Override
    public HealthData fetchCurrentHealthData() {
        // TODO: implement InfluxDB queries
        throw new UnsupportedOperationException("InfluxDB client not yet implemented");
    }
}
