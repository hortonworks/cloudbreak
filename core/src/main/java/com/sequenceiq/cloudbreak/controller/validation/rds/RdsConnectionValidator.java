package com.sequenceiq.cloudbreak.controller.validation.rds;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Component
public class RdsConnectionValidator {

    public void validateRdsConnection(String connectionURL, String connectionUserName, String connectionPassword) {
        Properties connectionProps = new Properties();
        connectionProps.put("user", connectionUserName);
        connectionProps.put("password", connectionPassword);
        try {
            Connection conn = DriverManager.getConnection(connectionURL, connectionProps);
            conn.close();
        } catch (SQLException e) {
            throw new BadRequestException("Failed to connect to RDS: " + e.getMessage(), e);
        }
    }
}
