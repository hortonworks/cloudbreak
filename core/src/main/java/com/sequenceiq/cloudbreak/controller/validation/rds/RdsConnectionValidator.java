package com.sequenceiq.cloudbreak.controller.validation.rds;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Component
public class RdsConnectionValidator {

    public void validateRdsConnection(RDSConfigJson rdsConfigJson) {
        Properties connectionProps = new Properties();
        connectionProps.put("user", rdsConfigJson.getConnectionUserName());
        connectionProps.put("password", rdsConfigJson.getConnectionPassword());
        try {
            Connection conn = DriverManager.getConnection(rdsConfigJson.getConnectionURL(), connectionProps);
            conn.close();
        } catch (SQLException e) {
            throw new BadRequestException("Failed to connect to RDS: " + e.getMessage(), e);
        }
    }
}
