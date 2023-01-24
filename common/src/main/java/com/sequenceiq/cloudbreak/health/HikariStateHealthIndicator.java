package com.sequenceiq.cloudbreak.health;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ThreadUtil;
import com.zaxxer.hikari.HikariDataSource;

@Component
public class HikariStateHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HikariStateHealthIndicator.class);

    @Value("${hikari.pool.readiness.down-if-pool-is-full}")
    private boolean setReadinessProbeDownIfPoolIsFull;

    @Inject
    private List<DataSource> dataSources;

    @Inject
    private HikariDataSourcePoolMetadataExtractor hikariDataSourcePoolMetadataExtractor;

    @Override
    public Health health() {
        boolean poolIsFull = false;
        Map<String, Object> details = new HashMap<>();
        try {
            for (DataSource dataSource : dataSources) {
                if (dataSource instanceof HikariDataSource) {
                    HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                    String poolName = hikariDataSource.getPoolName();
                    DataSourcePoolMetadata poolMetadata = hikariDataSourcePoolMetadataExtractor.extract(hikariDataSource);
                    details.put(poolName, poolMetadata);
                    if (poolMetadata.getActive() >= poolMetadata.getMax()) {
                        LOGGER.warn("Application has run out of open db connections. name: {}, details: {}", poolName, poolMetadata);
                        poolIsFull = true;
                    }
                }
            }
            LOGGER.debug("HikariPool details: {}", details);
            threadDumpIfNeeded(poolIsFull);
        } catch (RuntimeException exception) {
            LOGGER.info("Failed while checking the health of hikari pool!", exception);
        }
        return buildHealthResponse(poolIsFull, details);
    }

    private Health buildHealthResponse(boolean poolIsFull, Map<String, Object> details) {
        Health health;
        if (poolIsFull && setReadinessProbeDownIfPoolIsFull) {
            health = Health.down().withDetails(details).build();
        } else {
            health = Health.up().withDetails(details).build();
        }
        return health;
    }

    private void threadDumpIfNeeded(boolean poolIsFull) {
        if (poolIsFull) {
            LOGGER.info("Application has run out of open db connections. Thead dump: {}", ThreadUtil.dump());
        }
    }
}