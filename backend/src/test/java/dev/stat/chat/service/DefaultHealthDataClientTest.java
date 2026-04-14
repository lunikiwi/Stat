package dev.stat.chat.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.stat.chat.domain.HealthData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultHealthDataClient.
 * Uses mocked InfluxDBClient to avoid external dependencies.
 */
@QuarkusTest
class DefaultHealthDataClientTest {

    @Inject
    DefaultHealthDataClient healthDataClient;

    @InjectMock
    InfluxDBClient influxDBClient;

    private QueryApi queryApi;

    @BeforeEach
    void setUp() {
        queryApi = mock(QueryApi.class);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
    }

    @Test
    void shouldFetchHealthDataFromInfluxDB() {
        // Arrange: Mock InfluxDB response with all three metrics
        FluxTable bodyBatteryTable = createMockFluxTable("body_battery", 45.0);
        FluxTable sleepScoreTable = createMockFluxTable("sleep_score", 85.0);
        FluxTable trainingLoadTable = createMockFluxTable("training_minutes", 120.0);

        when(queryApi.query(anyString(), anyString()))
                .thenReturn(List.of(bodyBatteryTable, sleepScoreTable, trainingLoadTable));

        // Act
        HealthData result = healthDataClient.fetchCurrentHealthData();

        // Assert
        assertNotNull(result);
        assertEquals(45, result.bodyBattery());
        assertEquals(85, result.sleepScore());
        assertEquals(120, result.trainingLoadMinutes48h());

        // Verify that a Flux query was executed
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryApi).query(queryCaptor.capture(), anyString());

        String executedQuery = queryCaptor.getValue();
        assertNotNull(executedQuery);
        assertTrue(executedQuery.contains("from(bucket:"), "Query should specify bucket");
        assertTrue(executedQuery.contains("range(start:"), "Query should specify time range");
    }

    @Test
    void shouldThrowExceptionWhenInfluxDBFails() {
        // Arrange: Simulate InfluxDB failure
        when(queryApi.query(anyString(), anyString()))
                .thenThrow(new RuntimeException("InfluxDB connection failed"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.fetchCurrentHealthData()
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    @Test
    void shouldThrowExceptionWhenDataIsMissing() {
        // Arrange: Return incomplete data (missing sleep score)
        FluxTable bodyBatteryTable = createMockFluxTable("body_battery", 45.0);
        FluxTable trainingLoadTable = createMockFluxTable("training_minutes", 120.0);

        when(queryApi.query(anyString(), anyString()))
                .thenReturn(List.of(bodyBatteryTable, trainingLoadTable));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.fetchCurrentHealthData()
        );

        assertTrue(exception.getMessage().contains("incomplete") ||
                   exception.getMessage().contains("missing"));
    }

    /**
     * Helper method to create a mock FluxTable with a single record.
     */
    private FluxTable createMockFluxTable(String measurement, Double value) {
        FluxTable table = mock(FluxTable.class);
        FluxRecord record = mock(FluxRecord.class);

        when(record.getMeasurement()).thenReturn(measurement);
        when(record.getValue()).thenReturn(value);
        when(record.getTime()).thenReturn(Instant.now());

        when(table.getRecords()).thenReturn(List.of(record));

        return table;
    }
}
