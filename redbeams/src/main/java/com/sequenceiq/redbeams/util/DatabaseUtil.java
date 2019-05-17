package com.sequenceiq.redbeams.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.postgresql.Driver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

public class DatabaseUtil {
    public static final String DEFAULT_SCHEMA_NAME = "public";

    private DatabaseUtil() {
    }

    public static void createSchemaIfNeeded(String jdbcConnectionString, String dbUser, String dbPassword, String dbSchema)
            throws SQLException {
        if (!DEFAULT_SCHEMA_NAME.equals(dbSchema)) {
            SimpleDriverDataSource ds = new SimpleDriverDataSource();
            ds.setDriverClass(Driver.class);
            ds.setUrl(jdbcConnectionString);
            try (Connection conn = ds.getConnection(dbUser, dbPassword); Statement statement = conn.createStatement()) {
                    statement.execute("CREATE SCHEMA IF NOT EXISTS " + dbSchema);
            }
        }
    }
}
