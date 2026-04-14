package dev.stat.chat.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration for InfluxDB client.
 * Reads connection parameters from application.properties.
 */
@ApplicationScoped
public class InfluxDBConfig {

    @ConfigProperty(name = "stat.influxdb.url")
    String influxUrl;

    @ConfigProperty(name = "stat.influxdb.token")
    String influxToken;

    @ConfigProperty(name = "stat.influxdb.org")
    String influxOrg;

    @ConfigProperty(name = "stat.influxdb.bucket")
    String influxBucket;

    @Produces
    @ApplicationScoped
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(influxUrl, influxToken.toCharArray(), influxOrg, influxBucket);
    }

    public String getOrg() {
        return influxOrg;
    }

    public String getBucket() {
        return influxBucket;
    }
}
