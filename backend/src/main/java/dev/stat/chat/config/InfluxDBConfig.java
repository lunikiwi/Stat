package dev.stat.chat.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import okhttp3.OkHttpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * Configuration for InfluxDB client with connection pooling and resilience.
 * Reads connection parameters from application.properties.
 *
 * Configures OkHttp client with:
 * - Connection pooling (max 5 idle connections, 5 min keep-alive)
 * - Read timeout: 30s
 * - Connect timeout: 10s
 * - Write timeout: 30s
 */
@ApplicationScoped
public class InfluxDBConfig {

    private static final Logger LOG = Logger.getLogger(InfluxDBConfig.class);

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
        LOG.infof("Creating InfluxDB client for %s", influxUrl);

        // Configure OkHttp client with connection pooling and timeouts
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .retryOnConnectionFailure(true);  // Enable automatic retry on connection failures

        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(influxUrl)
                .authenticateToken(influxToken.toCharArray())
                .org(influxOrg)
                .bucket(influxBucket)
                .okHttpClient(okHttpBuilder)
                .build();

        return InfluxDBClientFactory.create(options);
    }

    public String getOrg() {
        return influxOrg;
    }

    public String getBucket() {
        return influxBucket;
    }
}
