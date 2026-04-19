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
}
