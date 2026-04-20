package dev.stat.chat.service;

import com.influxdb.client.DeleteApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.domain.NutritionEntry;
import dev.stat.chat.domain.TrendData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    private DeleteApi deleteApi;

    @BeforeEach
    void setUp() {
        queryApi = mock(QueryApi.class);
        writeApi = mock(WriteApiBlocking.class);
        deleteApi = mock(DeleteApi.class);
        when(influxDBClient.getQueryApi()).thenReturn(queryApi);
        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApi);
        when(influxDBClient.getDeleteApi()).thenReturn(deleteApi);
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

    @Test
    void shouldFetch7DayAveragesQueryContainsCorrectTimeRange() {
        // Arrange: Simulate InfluxDB failure to capture the query
        when(queryApi.query(anyString(), anyString()))
                .thenThrow(new RuntimeException("InfluxDB connection failed"));

        // Act & Assert
        assertThrows(ExternalServiceException.class, () -> healthDataClient.fetch7DayAverages());

        // Verify the query contains correct time range (-7d)
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryApi).query(queryCaptor.capture(), anyString());
        String query = queryCaptor.getValue();
        assertTrue(query.contains("-7d"), "Query should use 7-day time range");
        assertTrue(query.contains("mean()"), "Query should calculate mean/average");
        assertTrue(query.contains("avg_sleep_score"), "Query should calculate avg sleep score");
        assertTrue(query.contains("avg_body_battery"), "Query should calculate avg body battery");
        assertTrue(query.contains("avg_calories"), "Query should calculate avg calories");
    }

    @Test
    void shouldThrowExceptionWhen7DayAverageQueryFails() {
        // Arrange: Simulate InfluxDB failure
        when(queryApi.query(anyString(), anyString()))
                .thenThrow(new RuntimeException("InfluxDB connection failed"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.fetch7DayAverages()
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    @Test
    void shouldLogWeightToInfluxDB() {
        // Arrange
        double weightKg = 75.5;

        // Act
        healthDataClient.logWeight(weightKg);

        // Assert: Verify that line protocol string was written
        ArgumentCaptor<String> lineProtocolCaptor = ArgumentCaptor.forClass(String.class);
        verify(writeApi, times(1)).writeRecord(any(WritePrecision.class), lineProtocolCaptor.capture());

        String writtenRecord = lineProtocolCaptor.getValue();
        assertTrue(writtenRecord.contains("body_weight"), "Should write body_weight measurement");
        assertTrue(writtenRecord.contains("value=" + weightKg), "Should write correct weight value");
    }

    @Test
    void shouldLogWaterToInfluxDB() {
        // Arrange
        int waterMl = 500;

        // Act
        healthDataClient.logWater(waterMl);

        // Assert: Verify that line protocol string was written
        ArgumentCaptor<String> lineProtocolCaptor = ArgumentCaptor.forClass(String.class);
        verify(writeApi, times(1)).writeRecord(any(WritePrecision.class), lineProtocolCaptor.capture());

        String writtenRecord = lineProtocolCaptor.getValue();
        assertTrue(writtenRecord.contains("water_intake"), "Should write water_intake measurement");
        assertTrue(writtenRecord.contains("value=" + waterMl), "Should write correct water value");
    }

    @Test
    void shouldThrowExceptionWhenWeightWriteFails() {
        // Arrange: Simulate InfluxDB write failure
        doThrow(new RuntimeException("InfluxDB write failed"))
                .when(writeApi).writeRecord(any(WritePrecision.class), anyString());

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.logWeight(75.5)
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    @Test
    void shouldThrowExceptionWhenWaterWriteFails() {
        // Arrange: Simulate InfluxDB write failure
        doThrow(new RuntimeException("InfluxDB write failed"))
                .when(writeApi).writeRecord(any(WritePrecision.class), anyString());

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.logWater(500)
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    @Test
    void shouldThrowExceptionWhenBodyMetricsQueryFails() {
        // Arrange: Simulate InfluxDB failure
        when(queryApi.query(anyString(), anyString()))
                .thenThrow(new RuntimeException("InfluxDB connection failed"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.fetchBodyMetrics()
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    @Test
    void shouldGetNutritionEntriesQueryContainsCorrectDateRange() {
        // Arrange: Simulate InfluxDB failure to capture the query
        when(queryApi.query(anyString(), anyString()))
                .thenThrow(new RuntimeException("InfluxDB connection failed"));

        // Act & Assert
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        assertThrows(ExternalServiceException.class, () -> healthDataClient.getNutritionEntries(today));

        // Verify the query contains correct date range and filters
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryApi).query(queryCaptor.capture(), anyString());
        String query = queryCaptor.getValue();
        assertTrue(query.contains("range(start:"), "Query should have start time");
        assertTrue(query.contains("stop:"), "Query should have stop time");
        assertTrue(query.contains("nutrition_"), "Query should filter nutrition measurements");
        assertTrue(query.contains("sort"), "Query should sort by time");
    }

    @Test
    void shouldReturnEmptyListWhenNoNutritionEntriesExist() {
        // Arrange: Mock empty InfluxDB response
        when(queryApi.query(anyString(), anyString())).thenReturn(List.of());

        // Act
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<NutritionEntry> entries = healthDataClient.getNutritionEntries(today);

        // Assert
        assertNotNull(entries);
        assertTrue(entries.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenGetNutritionEntriesFails() {
        // Arrange: Simulate InfluxDB failure
        when(queryApi.query(anyString(), anyString()))
                .thenThrow(new RuntimeException("InfluxDB connection failed"));

        // Act & Assert
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.getNutritionEntries(today)
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

    @Test
    void shouldDeleteNutritionEntryByTimestamp() {
        // Arrange
        Instant timestamp = Instant.parse("2026-04-19T08:00:00Z");

        // Act
        healthDataClient.deleteNutritionEntry(timestamp);

        // Assert: Verify delete was called with correct parameters
        ArgumentCaptor<String> predicateCaptor = ArgumentCaptor.forClass(String.class);
        verify(deleteApi, times(1)).delete(
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                predicateCaptor.capture(),
                anyString(),
                anyString()
        );

        String predicate = predicateCaptor.getValue();
        assertTrue(predicate.contains("_measurement"), "Predicate should filter by measurement");
        assertTrue(predicate.contains("nutrition_"), "Predicate should target nutrition measurements");
    }

    @Test
    void shouldThrowExceptionWhenDeleteNutritionEntryFails() {
        // Arrange: Simulate InfluxDB delete failure
        Instant timestamp = Instant.parse("2026-04-19T08:00:00Z");
        doThrow(new RuntimeException("InfluxDB delete failed"))
                .when(deleteApi).delete(any(OffsetDateTime.class), any(OffsetDateTime.class), anyString(), anyString(), anyString());

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> healthDataClient.deleteNutritionEntry(timestamp)
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
    }

}
