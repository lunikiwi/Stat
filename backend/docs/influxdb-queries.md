# InfluxDB Flux Queries

## Health Data Aggregation Query

This document describes the Flux query used in [`DefaultHealthDataClient`](../src/main/java/dev/stat/chat/service/DefaultHealthDataClient.java) to aggregate health metrics from InfluxDB.

### Query Structure

The query aggregates three metrics according to the specification in [`specs/api_chat.md`](../../specs/api_chat.md):

1. **Body Battery**: Most recent value (last 24 hours - wider window to handle infrequent updates)
2. **Sleep Score**: Previous night's score (last 24 hours)
3. **Training Load**: Sum of training minutes (last 48 hours)

### Full Flux Query

```flux
// 1. Body Battery - Most recent value (24h window for reliability)
bodyBattery = from(bucket: "garmin")
  |> range(start: -24h)
  |> filter(fn: (r) => r["_measurement"] == "body_battery")
  |> filter(fn: (r) => r["_field"] == "value")
  |> last()
  |> set(key: "metric", value: "body_battery")

// 2. Sleep Score - Last night (previous 24h)
sleepScore = from(bucket: "garmin")
  |> range(start: -24h)
  |> filter(fn: (r) => r["_measurement"] == "sleep_score")
  |> filter(fn: (r) => r["_field"] == "value")
  |> last()
  |> set(key: "metric", value: "sleep_score")

// 3. Training Load - Sum of last 48h
trainingLoad = from(bucket: "garmin")
  |> range(start: -48h)
  |> filter(fn: (r) => r["_measurement"] == "training_minutes")
  |> filter(fn: (r) => r["_field"] == "value")
  |> sum()
  |> set(key: "metric", value: "training_minutes")

// Union all results
union(tables: [bodyBattery, sleepScore, trainingLoad])
```

### Expected InfluxDB Schema

The query expects the following measurements in InfluxDB:

#### Measurement: `body_battery`
- **Field**: `value` (integer, 0-100)
- **Description**: Current body battery level
- **Update Frequency**: Every few minutes

#### Measurement: `sleep_score`
- **Field**: `value` (integer, 0-100)
- **Description**: Sleep quality score from previous night
- **Update Frequency**: Once per night

#### Measurement: `training_minutes`
- **Field**: `value` (integer, minutes)
- **Description**: Training duration in minutes
- **Update Frequency**: After each training session

### Query Explanation

1. **`from(bucket: "garmin")`**: Selects the Garmin data bucket
2. **`range(start: -Xh)`**: Defines the time window for each metric
3. **`filter(fn: (r) => ...)`**: Filters by measurement and field name
4. **`last()`**: Gets the most recent value (for body battery and sleep score)
5. **`sum()`**: Aggregates all training minutes in the 48h window
6. **`set(key: "metric", value: "...")`**: Tags each result with a metric identifier
7. **`union(tables: [...])`**: Combines all three queries into a single result set

### Error Handling

The implementation validates that all three metrics are present in the result. If any metric is missing, an [`ExternalServiceException`](../src/main/java/dev/stat/chat/service/ExternalServiceException.java) is thrown with details about the missing metrics.

### Testing

Unit tests use mocked InfluxDB clients. See [`DefaultHealthDataClientTest`](../src/test/java/dev/stat/chat/service/DefaultHealthDataClientTest.java) for test cases covering:
- Successful data retrieval
- InfluxDB connection failures
- Missing/incomplete data scenarios
