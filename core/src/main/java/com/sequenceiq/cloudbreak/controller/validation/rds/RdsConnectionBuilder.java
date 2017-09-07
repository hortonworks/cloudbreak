package com.sequenceiq.cloudbreak.controller.validation.rds;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Component
public class RdsConnectionBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConnectionBuilder.class);

    public Map<String, String> buildRdsConnection(String connectionURL, String connectionUserName, String connectionPassword, String clusterName,
            Set<String> targets) {
        Map<String, String> map = new HashMap<>();

        Properties connectionProps = new Properties();
        connectionProps.put("user", connectionUserName);
        connectionProps.put("password", connectionPassword);
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
        try (Statement statement = conn.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + clusterName + service);
        } catch (PSQLException ex) {
            if ("42P04".equals(ex.getSQLState())) {
                LOGGER.warn("The expected database already exist");
            } else {
                throw new BadRequestException("Failed to create database in RDS: " + ex.getMessage(), ex);
            }
        } catch (SQLException e) {
            throw new BadRequestException("Failed to connect to RDS: " + e.getMessage(), e);
        }
    }
}
