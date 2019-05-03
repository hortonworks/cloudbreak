package com.sequenceiq.freeipa.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.postgresql.Driver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

public class DatabaseUtil {
    public static final String DEFAULT_SCHEMA_NAME = "public";

    private DatabaseUtil() {
    }

    public static void createSchemaIfNeeded(String dbType, String dbAddress, String dbName, String dbUser, String dbPassword, String dbSchema)
            throws SQLException {
        if (!DEFAULT_SCHEMA_NAME.equals(dbSchema)) {
            SimpleDriverDataSource ds = new SimpleDriverDataSource();
            ds.setDriverClass(Driver.class);
            ds.setUrl(String.format("jdbc:%s://%s/%s", dbType, dbAddress, dbName));
            try (Connection conn = ds.getConnection(dbUser, dbPassword); Statement statement = conn.createStatement()) {
                    statement.execute("CREATE SCHEMA IF NOT EXISTS " + dbSchema);
            }
        }
    }
}
