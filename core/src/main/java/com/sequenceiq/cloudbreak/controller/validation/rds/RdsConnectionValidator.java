package com.sequenceiq.cloudbreak.controller.validation.rds;

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
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class RdsConnectionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConnectionValidator.class);

    public void validateRdsConnection(RDSConfig rdsConfig) {
        if (StringUtils.isEmpty(rdsConfig.getConnectorJarUrl())) {
            if (rdsConfig.getDatabaseEngine() == DatabaseVendor.POSTGRES) {
                validateRdsConnection(new org.postgresql.Driver(), rdsConfig);
                return;
            } else {
                throw new BadRequestException("Only the PostgreSQL is validable without jdbc connector jar.");
            }
        }

        try {
            URL[] urls = {new URL("jar:" + rdsConfig.getConnectorJarUrl() + "!/")};
            try (URLClassLoader cl = URLClassLoader.newInstance(urls)) {
                Driver d = (Driver) cl.loadClass(rdsConfig.getDatabaseEngine().connectionDriver()).getConstructor().newInstance();
                validateRdsConnection(d, rdsConfig);
            } catch (ClassNotFoundException e) {
                String msg = String.format("Invalid connector JAR. Could not find the specified class (%s) or the JAR does not exist.",
                        rdsConfig.getDatabaseEngine().connectionDriver());
                LOGGER.info("{}, {}", msg, e.getMessage(), e);
                throw new BadRequestException(msg, e);
            } catch (InstantiationException | IllegalAccessException e) {
                String msg = String.format("Could not instantiate the specified class (%s).", rdsConfig.getDatabaseEngine().connectionDriver());
                LOGGER.info("{}, {}", msg, e.getMessage(), e);
                throw new BadRequestException(msg, e);
            } catch (Exception e) {
                LOGGER.info(e.getMessage(), e);
                throw new BadRequestException(e.getMessage(), e);
            }
        } catch (MalformedURLException e) {
            throw new BadRequestException("Malformed URL: " + e.getMessage(), e);
        }
    }

    private Properties connectionProperties(RDSConfig rdsConfig) {
        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", rdsConfig.getConnectionUserName());
        connectionProps.setProperty("password", rdsConfig.getConnectionPassword());
        return connectionProps;
    }

    private void validateRdsConnection(Driver driver, RDSConfig rdsConfig) {
        Properties connectionProps = connectionProperties(rdsConfig);
        try (Connection conn = driver.connect(rdsConfig.getConnectionURL(), connectionProps)) {
            LOGGER.debug("RDS is available");
        } catch (SQLException e) {
            throw new BadRequestException("Failed to connect to RDS: " + e.getMessage(), e);
        }
    }
}
