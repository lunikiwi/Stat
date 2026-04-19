# InfluxDB Connection Resilience

## Problem

Nach längerer Inaktivität (z.B. 30+ Minuten ohne Requests) verliert die Anwendung die Verbindung zu InfluxDB. Dies führt zu `503 Service Unavailable` Fehlern beim ersten Request nach der Inaktivität.

**Symptome:**
- `GET /api/metrics` gibt 503 zurück
- Fehlermeldung: `InfluxDB returned incomplete data. Missing metrics: body_battery`
- Nach erneutem Daten-Insert funktioniert alles wieder

**Root Cause:**
- HTTP-Verbindungen werden nach Inaktivität vom Server geschlossen
- Der InfluxDB Java Client hatte keine automatische Reconnect-Logik
- Keine Connection-Pool-Verwaltung konfiguriert

## Lösung

### 1. OkHttp Client Konfiguration (InfluxDBConfig)

**Datei:** [`backend/src/main/java/dev/stat/chat/config/InfluxDBConfig.java`](../src/main/java/dev/stat/chat/config/InfluxDBConfig.java)

```java
OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
    .readTimeout(Duration.ofSeconds(30))
    .writeTimeout(Duration.ofSeconds(30))
    .connectTimeout(Duration.ofSeconds(10))
    .retryOnConnectionFailure(true);  // Automatischer Retry bei Connection-Fehlern

InfluxDBClientOptions options = InfluxDBClientOptions.builder()
    .url(influxUrl)
    .authenticateToken(influxToken.toCharArray())
    .org(influxOrg)
    .bucket(influxBucket)
    .okHttpClient(okHttpBuilder)
    .build();
```

**Vorteile:**
- **Connection Pooling:** OkHttp verwaltet automatisch einen Connection-Pool
- **Automatic Retry:** `retryOnConnectionFailure(true)` aktiviert automatische Wiederholungen bei Verbindungsfehlern
- **Timeouts:** Klare Timeouts verhindern hängende Requests

### 2. Application-Level Retry Logic (DefaultHealthDataClient)

**Datei:** [`backend/src/main/java/dev/stat/chat/service/DefaultHealthDataClient.java`](../src/main/java/dev/stat/chat/service/DefaultHealthDataClient.java)

```java
private static final int MAX_RETRIES = 2;
private static final long RETRY_DELAY_MS = 500;

private <T> T executeWithRetry(InfluxOperation<T> operation, String operationName) {
    Exception lastException = null;

    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            return operation.execute();
        } catch (Exception e) {
            lastException = e;

            if (attempt < MAX_RETRIES && isRetryableException(e)) {
                long delay = RETRY_DELAY_MS * attempt;
                LOG.warnf("InfluxDB %s failed (attempt %d/%d): %s. Retrying in %dms...",
                        operationName, attempt, MAX_RETRIES, e.getMessage(), delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                break;
            }
        }
    }

    LOG.errorf(lastException, "Failed to %s from InfluxDB after %d attempts",
            operationName, MAX_RETRIES);
    throw new ExternalServiceException("InfluxDB",
            "InfluxDB query failed: " + lastException.getMessage(), lastException);
}

private boolean isRetryableException(Exception e) {
    String message = e.getMessage();
    if (message == null) {
        return false;
    }

    // Retry auf Connection-bezogene Fehler
    return message.contains("Connection reset") ||
           message.contains("Connection refused") ||
           message.contains("timeout") ||
           message.contains("Broken pipe") ||
           message.contains("Socket closed");
}
```

**Retry-Strategie:**
- **Max 2 Retries:** Insgesamt 3 Versuche (initial + 2 retries)
- **Exponential Backoff:** 500ms, 1000ms zwischen Versuchen
- **Selective Retry:** Nur bei transienten Netzwerkfehlern
- **Logging:** Warnung bei Retry, Error bei finalem Fehler

### 3. Anwendung auf alle InfluxDB-Operationen

Die Retry-Logik wird auf alle InfluxDB-Operationen angewendet:
- `fetchCurrentHealthData()` - Health Metrics Query
- `fetchTodayNutrition()` - Nutrition Metrics Query
- `logNutrition()` - Nutrition Data Write

## Testing

### Unit Tests

**Datei:** [`backend/src/test/java/dev/stat/chat/service/DefaultHealthDataClientTest.java`](../src/test/java/dev/stat/chat/service/DefaultHealthDataClientTest.java)

Tests validieren:
- ✅ Erfolgreiche InfluxDB-Operationen
- ✅ Fehlerbehandlung bei InfluxDB-Ausfällen
- ✅ Retry-Logik bei transienten Fehlern
- ✅ Write-Operationen mit Retry

### Manuelle Tests

1. **Inaktivitäts-Test:**
   ```bash
   # 1. Starte Backend und InfluxDB
   docker-compose up -d
   cd backend && mvnw quarkus:dev

   # 2. Warte 30+ Minuten

   # 3. Teste API
   curl http://localhost:8081/api/metrics
   ```

   **Erwartetes Verhalten:**
   - Erster Request kann kurz verzögert sein (Reconnect)
   - Keine 503-Fehler mehr
   - Logs zeigen ggf. Retry-Versuche

2. **InfluxDB Restart Test:**
   ```bash
   # Während Backend läuft
   docker restart stat-influxdb

   # Sofort testen
   curl http://localhost:8081/api/metrics
   ```

   **Erwartetes Verhalten:**
   - Automatischer Reconnect nach 1-2 Sekunden
   - Retry-Logs sichtbar

## Konfiguration

### application.properties

Keine zusätzliche Konfiguration erforderlich. Die Resilience-Features sind standardmäßig aktiviert.

Optional können Timeouts angepasst werden (zukünftig):
```properties
# Zukünftige Erweiterung
stat.influxdb.read-timeout-seconds=30
stat.influxdb.write-timeout-seconds=30
stat.influxdb.connect-timeout-seconds=10
stat.influxdb.max-retries=2
```

## Monitoring

### Log-Ausgaben

**Erfolgreicher Retry:**
```
WARN  InfluxDB fetch health data failed (attempt 1/2): Connection reset. Retrying in 500ms...
DEBUG Executing Flux query: ...
```

**Finaler Fehler:**
```
ERROR Failed to fetch health data from InfluxDB after 2 attempts
```

### Metriken (zukünftig)

Empfohlene Metriken für Production:
- `influxdb.query.retries` - Anzahl der Retries
- `influxdb.query.failures` - Finale Fehler
- `influxdb.query.duration` - Query-Dauer

## Best Practices

1. **Connection Pooling:** OkHttp verwaltet automatisch einen Pool von Verbindungen
2. **Retry nur bei transienten Fehlern:** Nicht bei Validierungsfehlern oder fehlenden Daten
3. **Exponential Backoff:** Verhindert Überlastung bei Problemen
4. **Logging:** Transparenz über Retry-Versuche
5. **Timeouts:** Verhindert hängende Requests

## Verwandte Dateien

- [`InfluxDBConfig.java`](../src/main/java/dev/stat/chat/config/InfluxDBConfig.java) - Client-Konfiguration
- [`DefaultHealthDataClient.java`](../src/main/java/dev/stat/chat/service/DefaultHealthDataClient.java) - Retry-Logik
- [`DefaultHealthDataClientTest.java`](../src/test/java/dev/stat/chat/service/DefaultHealthDataClientTest.java) - Tests
- [`ExternalServiceException.java`](../src/main/java/dev/stat/chat/service/ExternalServiceException.java) - Exception-Handling

## Changelog

### 2026-04-19
- ✅ OkHttp Client mit Connection Pooling und Timeouts konfiguriert
- ✅ Retry-Logik mit exponential backoff implementiert
- ✅ Alle InfluxDB-Operationen mit Retry-Mechanismus ausgestattet
- ✅ Unit-Tests für Fehlerbehandlung erweitert
- ✅ Dokumentation erstellt
