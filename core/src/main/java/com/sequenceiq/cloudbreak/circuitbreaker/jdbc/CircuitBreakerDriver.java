package com.sequenceiq.cloudbreak.circuitbreaker.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import io.opentracing.contrib.jdbc.TracingDriver;

public class CircuitBreakerDriver implements Driver {

    private static final Driver INSTANCE = new CircuitBreakerDriver();

    private static final Driver TRACING_DRIVER = new TracingDriver();

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not register CircuitBreakerDriver with DriverManager", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return new CircuitBreakerConnection(TRACING_DRIVER.connect(url, info));
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return TRACING_DRIVER.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return TRACING_DRIVER.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return TRACING_DRIVER.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return TRACING_DRIVER.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return TRACING_DRIVER.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return TRACING_DRIVER.getParentLogger();
    }
}
