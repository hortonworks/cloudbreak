package com.sequenceiq.redbeams.service.drivers;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Optional;
import java.util.Properties;
//CHECKSTYLE:OFF
import java.util.logging.Logger;
//CHECKSTYLE:ON

/**
 * This is a convenience class that wraps a SQL driver and adds some connection calls for using a databaseConfig
 * or databaseServerConfig object to connect.
 */
public class DriverWithConnectivity implements Driver {

    private final Driver delegate;

    public DriverWithConnectivity(Driver delegate) {
        this.delegate = requireNonNull(delegate, "delegate is null");
    }

    public Connection connect(DatabaseConfig databaseConfig) throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", databaseConfig.getConnectionUserName().getRaw());
        connectionProps.setProperty("password", databaseConfig.getConnectionPassword().getRaw());

        return connect(databaseConfig.getConnectionURL(), connectionProps);
    }

    public Connection connect(DatabaseServerConfig databaseServerConfig) throws SQLException {
        String connectionUrl = new DatabaseCommon().getJdbcConnectionUrl(
                databaseServerConfig.getDatabaseVendor().jdbcUrlDriverId(), databaseServerConfig.getHost(),
                databaseServerConfig.getPort(), Optional.empty());

        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", databaseServerConfig.getConnectionUserName());
        connectionProps.setProperty("password", databaseServerConfig.getConnectionPassword());
        connectionProps.setProperty("sslmode", "allow");

        return connect(connectionUrl, connectionProps);
    }

    @Override
    public Connection connect(String s, Properties properties) throws SQLException {
        return delegate.connect(s, properties);
    }

    @Override
    public boolean acceptsURL(String s) throws SQLException {
        return delegate.acceptsURL(s);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String s, Properties properties) throws SQLException {
        return delegate.getPropertyInfo(s, properties);
    }

    @Override
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return delegate.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @VisibleForTesting
    Driver getDelegate() {
        return delegate;
    }
}
