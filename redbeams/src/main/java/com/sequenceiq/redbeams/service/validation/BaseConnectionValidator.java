package com.sequenceiq.redbeams.service.validation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;

public class BaseConnectionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseConnectionValidator.class);

    protected void validate(String connectorJarUrl, DatabaseVendor databaseVendor, String connectionUrl, String connectionUserName,
                            String connectionPassword, Errors errors) {
        if (StringUtils.isEmpty(connectorJarUrl)) {
            if (databaseVendor == DatabaseVendor.POSTGRES) {
                validateWithDriver(new org.postgresql.Driver(), connectionUrl, connectionUserName, connectionPassword, errors);
            } else {
                errors.rejectValue("connectorJarUrl", "missingjarurl", "Only PostgreSQL can be validated without a connector JAR URL");
            }
            return;
        }

        try {
            URL[] urls = { new URL("jar:" + connectorJarUrl + "!/") };
            String driverClassName = databaseVendor.connectionDriver();
            try (URLClassLoader cl = URLClassLoader.newInstance(urls)) {
                Driver d = (Driver) cl.loadClass(driverClassName).getConstructor().newInstance();
                validateWithDriver(d, connectionUrl, connectionUserName, connectionPassword, errors);
            } catch (ClassNotFoundException e) {
                String message = "Could not locate driver class " + driverClassName + " in connector JAR URL " + connectorJarUrl;
                LOGGER.info(message, e);
                errors.rejectValue("connectorJarUrl", "classnotfound", message);
            } catch (IOException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                String message = "Could not load driver class " + driverClassName + " from connector JAR URL " + connectorJarUrl;
                LOGGER.info(message, e);
                errors.rejectValue("connectorJarUrl", "couldnotload", message);
            }
        } catch (MalformedURLException e) {
            String message = "Malformed connector JAR URL " + connectorJarUrl;
            LOGGER.info(message, e);
            errors.rejectValue("connectorJarUrl", "malformedurl", message);
        }
    }

    private void validateWithDriver(Driver driver, String connectionUrl, String connectionUserName, String connectionPassword,
                                    Errors errors) {
        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", connectionUserName);
        connectionProps.setProperty("password", connectionPassword);

        try (Connection conn = driver.connect(connectionUrl, connectionProps)) {
            LOGGER.debug("Connection successful to {}", connectionUrl);
        } catch (SQLException e) {
            String message = "Failed to connect to " + connectionUrl;
            LOGGER.info(message, e);
            errors.reject("connectionfailed", message);
        }
    }
}
