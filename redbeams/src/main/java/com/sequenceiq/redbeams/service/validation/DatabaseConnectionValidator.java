package com.sequenceiq.redbeams.service.validation;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;

@Component
public class DatabaseConnectionValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnectionValidator.class);

    @Inject
    private DriverFunctions driverFunctions;

    @Override
    public boolean supports(Class<?> clazz) {
        return DatabaseConfig.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DatabaseConfig database = (DatabaseConfig) target;
        String connectionUrl = database.getConnectionURL();

        driverFunctions.execWithDatabaseDriver(database, driver -> {
            try (Connection conn = driver.connect(database)) {
                LOGGER.debug("Connection successful to {}", connectionUrl);
            } catch (SQLException e) {
                String message = "Failed to connect to " + connectionUrl;
                LOGGER.info(message, e);
                errors.reject("connectionfailed", message);
            }
        });
    }

}
