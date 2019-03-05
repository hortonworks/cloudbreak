package com.sequenceiq.cloudbreak.controller.validation.rds;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;

@Component
public class RdsConnectionBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConnectionBuilder.class);

    private static final String CREATE_SQL = "CREATE DATABASE ?";

    public Map<String, String> buildRdsConnection(String connectionURL, String connectionUserName, String connectionPassword, String clusterName,
            Iterable<String> targets) {
        Map<String, String> map = new HashMap<>();

        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", connectionUserName);
        connectionProps.setProperty("password", connectionPassword);
        try (Connection conn = DriverManager.getConnection(connectionURL, connectionProps)) {
            for (String target : targets) {
                createDb(conn, clusterName, target);
                map.put(target, clusterName + target);
            }
        } catch (SQLException e) {
            throw new BadRequestException("Failed to connect to RDS: " + e.getMessage(), e);
        }
        return map;
    }

    private void createDb(Connection conn, String clusterName, String service) {
        try (PreparedStatement preparedStatement = conn.prepareStatement(CREATE_SQL)) {
            preparedStatement.setString(1, clusterName + service);
            preparedStatement.executeUpdate(CREATE_SQL);
        } catch (PSQLException ex) {
            if ("42P04".equals(ex.getSQLState())) {
                LOGGER.info("The expected database already exist");
            } else {
                throw new BadRequestException("Failed to create database in RDS: " + ex.getMessage(), ex);
            }
        } catch (SQLException e) {
            throw new BadRequestException("Failed to connect to RDS: " + e.getMessage(), e);
        }
    }
}
