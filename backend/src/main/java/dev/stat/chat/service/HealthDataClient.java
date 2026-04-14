package dev.stat.chat.service;

import dev.stat.chat.domain.HealthData;

/**
 * System boundary: fetches aggregated health metrics from InfluxDB.
 * This interface exists to decouple the chat logic from the InfluxDB client
 * and to make the boundary mockable in tests.
 */
public interface HealthDataClient {

    /**
     * Retrieves aggregated health data:
     * - Sleep score from previous night
     * - Training load (minutes) from last 48 hours
     * - Most recent Body Battery value
     *
     * @return aggregated health data
     * @throws ExternalServiceException if InfluxDB is unreachable or query fails
     */
    HealthData fetchCurrentHealthData();
}
