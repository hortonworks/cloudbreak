package com.sequenceiq.redbeams.service.drivers;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        execWithDatabaseDriver(databaseConfig.getConnectorJarUrl(), databaseConfig.getDatabaseVendor(), consumer);
    }

    /**
     * Executes the given function with the associated driver.
     *
     * @param databaseServerConfig the database server config used to get the driver
     * @param consumer             the function to execute
     */
    public void execWithDatabaseDriver(DatabaseServerConfig databaseServerConfig,
        Consumer<DriverWithConnectivity> consumer) {
        execWithDatabaseDriver(databaseServerConfig.getConnectorJarUrl(), databaseServerConfig.getDatabaseVendor(),
                consumer);
    }

    /**
     * Executes the given function with the associated driver.
     *
     * @param connectorJarUrl the URL to the connector jar
     * @param databaseVendor  the database vendor
     * @param consumer        the function to execute
     * @throws DriverLoadingException if there was a problem loading the driver
     */
    public void execWithDatabaseDriver(String connectorJarUrl, DatabaseVendor databaseVendor,
        Consumer<DriverWithConnectivity> consumer) {
        if (StringUtils.isEmpty(connectorJarUrl)) {
            if (databaseVendor == DatabaseVendor.POSTGRES) {
                consumer.accept(new DriverWithConnectivity(new org.postgresql.Driver()));
            } else {
                throw new DriverLoadingException("connectorJarUrl", "missingjarurl",
                        "Only PostgreSQL can be validated without a connector JAR URL");
            }
        } else {
            try {
                URL[] urls = {new URL("jar:" + connectorJarUrl + "!/")};
                String driverClassName = databaseVendor.connectionDriver();
                try (URLClassLoader cl = URLClassLoader.newInstance(urls)) {
                    Driver driver = (Driver) cl.loadClass(driverClassName).getConstructor().newInstance();
                    consumer.accept(new DriverWithConnectivity(driver));
                } catch (ClassNotFoundException e) {
                    String message = "Could not locate driver class " + driverClassName + " in connector JAR URL " + connectorJarUrl;
                    LOGGER.info(message, e);
                    throw new DriverLoadingException("connectorJarUrl", "classnotfound", message);
                } catch (IOException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    String message = "Could not load driver class " + driverClassName + " from connector JAR URL " + connectorJarUrl;
                    LOGGER.info(message, e);
                    throw new DriverLoadingException("connectorJarUrl", "couldnotload", message);
                }
            } catch (MalformedURLException e) {
                String message = "Malformed connector JAR URL " + connectorJarUrl;
                LOGGER.info(message, e);
                throw new DriverLoadingException("connectorJarUrl", "malformedurl", message);
            }
        }
    }
}
