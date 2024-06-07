package com.sequenceiq.cloudbreak.database;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.postgresql.Driver;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import com.sequenceiq.cloudbreak.common.database.BatchProperties;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseUtil {

    public static final String DEFAULT_SCHEMA_NAME = "public";

    private static final int CONNECTION_MAX_LIFETIME_IN_MINUTES = 13;

    private DatabaseUtil() {
    }

    public static void createSchemaIfNeeded(String dbType, String dbAddress, String dbName, String dbUser, String dbPassword, String dbSchema)
            throws SQLException {
        if (!DEFAULT_SCHEMA_NAME.equals(dbSchema)) {
            SimpleDriverDataSource ds = new SimpleDriverDataSource();
            ds.setDriverClass(Driver.class);
            ds.setUrl(String.format("jdbc:%s://%s/%s", dbType, dbAddress, dbName));
            try (Connection conn = ds.getConnection(dbUser, dbPassword); Statement statement = conn.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + dbSchema);
            }
        }
    }

    public static HikariDataSource getDataSource(String poolName, DatabaseProperties databaseProperties, String databaseAddress, NodeConfig nodeConfig)
            throws SQLException {
        return getDataSource(poolName, databaseProperties, databaseAddress, nodeConfig, Optional.empty());
    }

    public static HikariDataSource getDataSource(String poolName, DatabaseProperties databaseProperties, String databaseAddress, NodeConfig nodeConfig,
            Optional<Integer> poolSizeOverride) throws SQLException {
        DatabaseUtil.createSchemaIfNeeded("postgresql", databaseAddress, databaseProperties.getDatabase(), databaseProperties.getUser(),
                databaseProperties.getPassword(), databaseProperties.getSchemaName());
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        if (databaseProperties.isSsl()) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
        }
        if (nodeConfig.isNodeIdSpecified()) {
            config.addDataSourceProperty("ApplicationName", nodeConfig.getId());
        }
        String jdbcUrl = String.format("jdbc:postgresql://%s/%s?currentSchema=%s", databaseAddress, databaseProperties.getDatabase(),
                databaseProperties.getSchemaName());
        config.setJdbcUrl(jdbcUrl);
        if (poolSizeOverride.isPresent()) {
            config.setMaximumPoolSize(poolSizeOverride.get());
        } else {
            config.setMaximumPoolSize(databaseProperties.getPoolSize());
        }
        config.setMinimumIdle(databaseProperties.getMinimumIdle());
        config.setConnectionTimeout(SECONDS.toMillis(databaseProperties.getConnectionTimeout()));
        long customIdleTimeout = MINUTES.toMillis(databaseProperties.getIdleTimeout());
        config.setIdleTimeout(customIdleTimeout);
        config.setUsername(databaseProperties.getUser());

        HikariDataSource hikariDataSource;
        if (databaseProperties.rdsIamRoleBasedAuthentication()) {
            long connectionMaxLifeTime = MINUTES.toMillis(CONNECTION_MAX_LIFETIME_IN_MINUTES);
            long idleTimeout = Math.min(customIdleTimeout, connectionMaxLifeTime);
            config.setMaxLifetime(connectionMaxLifeTime);
            config.setIdleTimeout(idleTimeout);
            hikariDataSource = new RdsIamAuthBasedHikariDataSource(config);
        } else {
            config.setPassword(databaseProperties.getPassword());
            hikariDataSource = new HikariDataSource(config);
        }

        return hikariDataSource;
    }

    public static BatchProperties createBatchProperties(Environment environment) {
        return new BatchProperties(environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", Integer.class),
                environment.getProperty("spring.jpa.properties.hibernate.order_inserts", Boolean.class),
                environment.getProperty("spring.jpa.properties.hibernate.order_updates", Boolean.class),
                environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_versioned_data", Boolean.class));
    }

}
