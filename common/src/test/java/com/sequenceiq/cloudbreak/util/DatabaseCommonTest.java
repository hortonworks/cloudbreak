package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon.JdbcConnectionUrlFields;

public class DatabaseCommonTest {
    private static final String ORACLE_URL = "jdbc:oracle:@test.eu-west-1.rds.amazonaws.com:1521/hivedb";

    private static final String ORACLE_THIN_URL_WITHOUT_DATABASE = "jdbc:oracle:thin:@test.eu-west-1.rds.amazonaws.com:1521";

    private static final String ORACLE_THIN_URL = ORACLE_THIN_URL_WITHOUT_DATABASE + ":hivedb";

    private static final String MARIADB_URL = "jdbc:mariadb://test.eu-west-1.rds.amazonaws.com:3306/hivedb";

    private static final String MYSQL_URL_WITHOUT_DATABASE_AND_SSL = "jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306";

    private static final String MYSQL_URL_WITHOUT_SSL = MYSQL_URL_WITHOUT_DATABASE_AND_SSL + "/hivedb";

    private static final String MYSQL_URL = MYSQL_URL_WITHOUT_SSL + "?useSSL=true&requireSSL=false";

    private static final String MYSQL_URL_WITHOUT_PORT = "jdbc:mysql://test.eu-west-1.rds.amazonaws.com/hivedb?useSSL=true&requireSSL=false";

    private static final String POSTGRES_URL_WITHOUT_DATABASE = "jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/";

    private static final String POSTGRES_URL_WITH_POSTGRES_DATABASE = POSTGRES_URL_WITHOUT_DATABASE + "postgres";

    private static final String POSTGRES_URL = POSTGRES_URL_WITHOUT_DATABASE + "hivedb";

    private static final String POSTGRES_URL_WITH_HYPHENATED_DATABASE_NAME = POSTGRES_URL_WITHOUT_DATABASE + "hive-db-";

    private DatabaseCommon databaseCommon;

    @BeforeEach
    public void setUp() {
        databaseCommon = new DatabaseCommon();
    }

    @Test
    public void testMariadbParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(MARIADB_URL);
        assertEquals(fields.getVendorDriverId(), "mariadb");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 3306);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:3306");
        assertEquals(fields.getDatabase(), Optional.of("hivedb"));
    }

    @Test
    public void testMysqlParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(MYSQL_URL);
        assertEquals(fields.getVendorDriverId(), "mysql");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 3306);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:3306");
        assertEquals(fields.getDatabase(), Optional.of("hivedb"));
    }

    @Test
    public void testMysqlNoDatabaseParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(MYSQL_URL_WITHOUT_DATABASE_AND_SSL);
        assertEquals(fields.getVendorDriverId(), "mysql");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 3306);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:3306");
        assertFalse(fields.getDatabase().isPresent());
    }

    @Test
    public void testOracleParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(ORACLE_URL);
        assertEquals(fields.getVendorDriverId(), "oracle");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 1521);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:1521");
        assertEquals(fields.getDatabase(), Optional.of("hivedb"));
    }

    @Test
    public void testOracleThinParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(ORACLE_THIN_URL);
        assertEquals(fields.getVendorDriverId(), "oracle");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 1521);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:1521");
        assertEquals(fields.getDatabase(), Optional.of("hivedb"));
    }

    @Test
    public void testOracleThinNoDatabaseParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(ORACLE_THIN_URL_WITHOUT_DATABASE);
        assertEquals(fields.getVendorDriverId(), "oracle");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 1521);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:1521");
        assertFalse(fields.getDatabase().isPresent());
    }

    @Test
    public void testPostgresParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(POSTGRES_URL);
        assertEquals(fields.getVendorDriverId(), "postgresql");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 5432);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:5432");
        assertEquals(fields.getDatabase(), Optional.of("hivedb"));
    }

    @Test
    public void testPostgresNoDatabaseParsing() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            databaseCommon.parseJdbcConnectionUrl(POSTGRES_URL_WITHOUT_DATABASE);
        });

        assertEquals("PostgreSQL connection URLs require a database", exception.getMessage());
    }

    @Test
    public void testPostgresHyphenatedDatabaseParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(POSTGRES_URL_WITH_HYPHENATED_DATABASE_NAME);
        assertEquals(fields.getVendorDriverId(), "postgresql");
        assertEquals(fields.getHost(), "test.eu-west-1.rds.amazonaws.com");
        assertEquals(fields.getPort(), 5432);
        assertEquals(fields.getHostAndPort(), "test.eu-west-1.rds.amazonaws.com:5432");
        assertEquals(fields.getDatabase(), Optional.of("hive-db-"));
    }

    @Test
    public void testPostgresConnectionUrlWithoutDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("postgresql", "test.eu-west-1.rds.amazonaws.com", 5432, Optional.empty());
        assertEquals(url, POSTGRES_URL_WITH_POSTGRES_DATABASE);
    }

    @Test
    public void testPostgresConnectionUrlWithDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("postgresql", "test.eu-west-1.rds.amazonaws.com", 5432, Optional.of("hivedb"));
        assertEquals(url, POSTGRES_URL);
    }

    @Test
    public void testMySQLConnectionUrlWithoutDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("mysql", "test.eu-west-1.rds.amazonaws.com", 3306, Optional.empty());
        assertEquals(url, MYSQL_URL_WITHOUT_DATABASE_AND_SSL);
    }

    @Test
    public void testMySQLConnectionUrlWithDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("mysql", "test.eu-west-1.rds.amazonaws.com", 3306, Optional.of("hivedb"));
        assertEquals(url, MYSQL_URL_WITHOUT_SSL);
    }

    @Test
    public void testOracleConnectionUrlWithoutDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("oracle", "test.eu-west-1.rds.amazonaws.com", 1521, Optional.empty());
        assertEquals(url, ORACLE_THIN_URL_WITHOUT_DATABASE);
    }

    @Test
    public void testOracleConnectionUrlWithDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("oracle", "test.eu-west-1.rds.amazonaws.com", 1521, Optional.of("hivedb"));
        assertEquals(url, ORACLE_THIN_URL);
    }

    @Test
    public void testPortCheck() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new JdbcConnectionUrlFields("postgresql", "test.eu-west-1.rds.amazonaws.com", -1, Optional.empty());
        });

        assertEquals("invalid port -1", exception.getMessage());
    }

    @Test
    public void testExecuteUpdatesNoTransaction() throws SQLException {
        Connection conn = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate(any(String.class))).thenReturn(1);
        List<String> sqlStrings = List.of("sql1", "sql2");

        List<Integer> rowCounts = databaseCommon.executeUpdates(conn, sqlStrings, false);

        assertEquals(rowCounts, List.of(1, 1));
        verify(statement).executeUpdate("sql1");
        verify(statement).executeUpdate("sql2");
        verify(conn, never()).setAutoCommit(false);
        verify(conn, never()).commit();
    }

    @Test
    public void testExecuteUpdatesTransaction() throws SQLException {
        Connection conn = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate(any(String.class))).thenReturn(1);
        List<String> sqlStrings = List.of("sql1", "sql2");

        List<Integer> rowCounts = databaseCommon.executeUpdates(conn, sqlStrings, true);

        assertEquals(rowCounts, List.of(1, 1));
        verify(statement).executeUpdate("sql1");
        verify(statement).executeUpdate("sql2");

        InOrder inOrder = inOrder(conn);
        inOrder.verify(conn).setAutoCommit(false);
        inOrder.verify(conn).commit();
        inOrder.verify(conn, never()).rollback();
        inOrder.verify(conn).setAutoCommit(true);
    }

    @Test
    public void testExecuteUpdatesFailNoTransaction() throws SQLException {
        Connection conn = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate("sql1")).thenThrow(new SQLException("fail"));
        List<String> sqlStrings = List.of("sql1", "sql2");


        SQLException exception = assertThrows(SQLException.class, () -> {
            try {
                databaseCommon.executeUpdates(conn, sqlStrings, false);
            } finally {
                verify(statement).executeUpdate("sql1");
                verify(statement, never()).executeUpdate("sql2");
                verify(conn, never()).rollback();
            }
        });

        assertEquals("fail", exception.getMessage());
    }

    @Test
    public void testExecuteUpdatesFailTransaction() throws SQLException {
        Connection conn = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate("sql1")).thenThrow(new SQLException("fail"));
        List<String> sqlStrings = List.of("sql1", "sql2");

        SQLException exception = assertThrows(SQLException.class, () -> {
            try {
                databaseCommon.executeUpdates(conn, sqlStrings, true);
            } finally {
                verify(statement).executeUpdate("sql1");
                verify(statement, never()).executeUpdate("sql2");

                InOrder inOrder = inOrder(conn);
                inOrder.verify(conn).setAutoCommit(false);
                inOrder.verify(conn).rollback();
                inOrder.verify(conn, never()).commit();
                inOrder.verify(conn).setAutoCommit(true);
            }
        });

        assertEquals("fail", exception.getMessage());
    }

    @Test
    public void testDbMajorVersionRegexp() {
        Pattern dbVersionPattern = Pattern.compile(POSTGRES_VERSION_REGEX);
        assertFalse(dbVersionPattern.matcher("").matches(), "empty string");
        assertFalse(dbVersionPattern.matcher("null").matches(), "not a version");
        assertTrue(dbVersionPattern.matcher("9.6").matches(), "valid version: 9.6");
        assertTrue(dbVersionPattern.matcher("10").matches(), "valid version: 10");
        assertTrue(dbVersionPattern.matcher("11").matches(), "valid version: 11");
        assertTrue(dbVersionPattern.matcher("12").matches(), "valid version: 12");
        assertTrue(dbVersionPattern.matcher("13").matches(), "valid version: 13");
        assertTrue(dbVersionPattern.matcher("14").matches(), "valid version: 14");
        assertTrue(dbVersionPattern.matcher("17").matches(), "valid version: 17");
        assertFalse(dbVersionPattern.matcher("141").matches(), "not a valid version");
        assertFalse(dbVersionPattern.matcher("1111").matches(), "valid version twice");
    }
}
