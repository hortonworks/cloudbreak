package com.sequenceiq.redbeams.service.validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;

@Component
public class DatabaseServerConnectionValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerConnectionValidator.class);

    @Inject
    private DriverFunctions driverFunctions;

    @Inject
    private DatabaseCommon databaseCommon;

    @Override
    public boolean supports(Class<?> clazz) {
        return DatabaseServerConfig.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DatabaseServerConfig server = (DatabaseServerConfig) target;
        String connectionUrl = databaseCommon.getJdbcConnectionUrl(server.getDatabaseVendor().jdbcUrlDriverId(),
            server.getHost(), server.getPort(), Optional.empty());

        driverFunctions.execWithDatabaseDriver(server, driver -> {
            try (Connection conn = driver.connect(server)) {
                LOGGER.debug("Connection successful to {}", connectionUrl);
            } catch (SQLException e) {
                String message = "Failed to connect to " + connectionUrl;
                LOGGER.info(message, e);
                errors.reject("connectionfailed", message);
            }
        });
    }

}
