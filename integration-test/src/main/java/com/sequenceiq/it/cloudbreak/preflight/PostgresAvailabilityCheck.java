package com.sequenceiq.it.cloudbreak.preflight;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.config.database.DatabaseProperties;

@Component
public class PostgresAvailabilityCheck implements PreflightCheck {

    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    @Inject
    private DatabaseProperties databaseProperties;

    @Override
    public PreflightCheckResult check() {
        try (Connection connection = DriverManager.getConnection(
                databaseProperties.getConnectionUrl(), databaseProperties.getUsername(), databaseProperties.getPassword())) {
            if (connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                return PreflightCheckResult.pass(getName(), "connected to " + databaseProperties.getConnectionUrl());
            }
            return PreflightCheckResult.fail(getName(), "connection to " + databaseProperties.getConnectionUrl() + " is not valid");
        } catch (SQLException e) {
            return PreflightCheckResult.fail(getName(), "could not connect to " + databaseProperties.getConnectionUrl() + ": " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "postgres-availability";
    }
}
