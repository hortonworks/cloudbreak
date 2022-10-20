package com.sequenceiq.redbeams.service.drivers;

import java.sql.Driver;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

/**
 * Caches and retrieves database drivers.
 */
@Service
public class DriverFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverFunctions.class);

    /**
     * Executes the given function with the associated driver.
     *
     * @param databaseConfig the database config used to get the driver
     * @param consumer       the function to execute
     * @throws DriverLoadingException if there was a problem loading the driver
     */
    public void execWithDatabaseDriver(DatabaseConfig databaseConfig, Consumer<DriverWithConnectivity> consumer) {
        execWithDatabaseDriver(databaseConfig.getDatabaseVendor(), consumer);
    }

    /**
     * Executes the given function with the associated driver.
     *
     * @param databaseServerConfig the database server config used to get the driver
     * @param consumer             the function to execute
     */
    public void execWithDatabaseDriver(DatabaseServerConfig databaseServerConfig,
        Consumer<DriverWithConnectivity> consumer) {
        execWithDatabaseDriver(databaseServerConfig.getDatabaseVendor(), consumer);
    }

    /**
     * Executes the given function with the associated driver.
     *
     * @param databaseVendor  the database vendor
     * @param consumer        the function to execute
     * @throws DriverLoadingException if there was a problem loading the driver
     */
    private void execWithDatabaseDriver(DatabaseVendor databaseVendor, Consumer<DriverWithConnectivity> consumer) {
        Driver driver;
        switch (databaseVendor) {
            case POSTGRES:
                LOGGER.debug("Using internal driver for " + databaseVendor);
                driver = new org.postgresql.Driver();
                break;
            default:
                throw new UnsupportedOperationException("Database access not supported for " + databaseVendor);
        }
        consumer.accept(new DriverWithConnectivity(driver));
    }
}
