package com.sequenceiq.cloudbreak.controller.validation.rds;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Component
public class RdsConnectionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConnectionValidator.class);

    public void validateRdsConnection(String connectionURL, String connectionUserName, String connectionPassword) {
        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", connectionUserName);
        connectionProps.setProperty("password", connectionPassword);
        try (Connection conn = DriverManager.getConnection(connectionURL, connectionProps)) {
            LOGGER.debug("RDS is available");
        } catch (SQLException e) {
            throw new BadRequestException("Failed to connect to RDS: " + e.getMessage(), e);
        }
    }
}
