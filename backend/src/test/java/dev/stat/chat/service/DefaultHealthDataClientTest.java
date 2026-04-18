package dev.stat.chat.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private WriteApiBlocking writeApi;

    @BeforeEach
    void setUp() {
        queryApi = mock(QueryApi.class);
        writeApi = mock(WriteApiBlocking.class);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApi);
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

    @Test
    void shouldFetchNutritionDataFromInfluxDB() {
        // Arrange: Mock InfluxDB response with nutrition metrics from today (since 00:00)
        FluxTable caloriesTable = createMockFluxTable("nutrition_calories", 2100.0);
        FluxTable proteinTable = createMockFluxTable("nutrition_protein", 150.0);
        FluxTable carbsTable = createMockFluxTable("nutrition_carbs", 220.0);
        FluxTable fatTable = createMockFluxTable("nutrition_fat", 70.0);

        when(queryApi.query(anyString(), anyString()))
                .thenReturn(List.of(caloriesTable, proteinTable, carbsTable, fatTable));

        // Act
        NutritionData result = healthDataClient.fetchTodayNutrition();

        // Assert
        assertNotNull(result);
        assertEquals(2100, result.calories());
        assertEquals(150, result.proteinGrams());
        assertEquals(220, result.carbsGrams());
        assertEquals(70, result.fatGrams());

        // Verify that a Flux query was executed
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryApi).query(queryCaptor.capture(), anyString());

        String executedQuery = queryCaptor.getValue();
        assertNotNull(executedQuery);
        assertTrue(executedQuery.contains("nutrition_calories"), "Query should fetch calories");
        assertTrue(executedQuery.contains("nutrition_protein"), "Query should fetch protein");
        assertTrue(executedQuery.contains("nutrition_carbs"), "Query should fetch carbs");
        assertTrue(executedQuery.contains("nutrition_fat"), "Query should fetch fat");
        assertTrue(executedQuery.contains("sum()"), "Query should aggregate with sum()");
    }

    @Test
    void shouldThrowExceptionWhenNutritionDataIsMissing() {
        // Arrange: Return incomplete nutrition data (missing fat)
        FluxTable caloriesTable = createMockFluxTable("nutrition_calories", 2100.0);
        FluxTable proteinTable = createMockFluxTable("nutrition_protein", 150.0);
        FluxTable carbsTable = createMockFluxTable("nutrition_carbs", 220.0);

        when(queryApi.query(anyString(), anyString()))
                .thenReturn(List.of(caloriesTable, proteinTable, carbsTable));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.fetchTodayNutrition()
        );

        assertTrue(exception.getMessage().contains("incomplete") ||
                   exception.getMessage().contains("missing"));
    }

    @Test
    void shouldThrowExceptionWhenNutritionQueryFails() {
        // Arrange: Simulate InfluxDB failure
        when(queryApi.query(anyString(), anyString()))
                .thenThrow(new RuntimeException("InfluxDB connection failed"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.fetchTodayNutrition()
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    @Test
    void shouldLogNutritionToInfluxDB() {
        // Arrange
        int calories = 500;
        int protein = 30;
        int carbs = 60;
        int fat = 20;

        // Act
        healthDataClient.logNutrition(calories, protein, carbs, fat);

        // Assert: Verify that 4 line protocol strings were written (one per metric)
        ArgumentCaptor<String> lineProtocolCaptor = ArgumentCaptor.forClass(String.class);
        verify(writeApi, times(4)).writeRecord(any(WritePrecision.class), lineProtocolCaptor.capture());

        List<String> writtenRecords = lineProtocolCaptor.getAllValues();
        assertEquals(4, writtenRecords.size());

        // Verify each metric was written with correct format
        assertTrue(writtenRecords.stream().anyMatch(r -> r.contains("nutrition_calories") && r.contains("value=" + calories)),
                "Should write calories");
        assertTrue(writtenRecords.stream().anyMatch(r -> r.contains("nutrition_protein") && r.contains("value=" + protein)),
                "Should write protein");
        assertTrue(writtenRecords.stream().anyMatch(r -> r.contains("nutrition_carbs") && r.contains("value=" + carbs)),
                "Should write carbs");
        assertTrue(writtenRecords.stream().anyMatch(r -> r.contains("nutrition_fat") && r.contains("value=" + fat)),
                "Should write fat");
    }

    @Test
    void shouldThrowExceptionWhenWriteFails() {
        // Arrange: Simulate InfluxDB write failure
        doThrow(new RuntimeException("InfluxDB write failed"))
                .when(writeApi).writeRecord(any(WritePrecision.class), anyString());

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.logNutrition(500, 30, 60, 20)
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    /**
     * Helper method to create a mock FluxTable with a single record.
     * Uses Mockito's lenient mode to handle final classes.
     */
    private FluxTable createMockFluxTable(String metricName, Double value) {
        FluxTable table = mock(FluxTable.class, withSettings().lenient());
        FluxRecord record = mock(FluxRecord.class, withSettings().lenient());

        // Mock the getValueByKey method to return the metric name
        when(record.getValueByKey("metric")).thenReturn(metricName);
        when(record.getValue()).thenReturn(value);
        when(record.getTime()).thenReturn(Instant.now());

        when(table.getRecords()).thenReturn(List.of(record));

        return table;
    }
}
