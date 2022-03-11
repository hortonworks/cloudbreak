package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private DatabaseCommon databaseCommon;

    @Before
    public void setUp() {
        databaseCommon = new DatabaseCommon();
    }

    @Test
    public void testMariadbParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(MARIADB_URL);
        assertThat(fields.getVendorDriverId()).isEqualTo("mariadb");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(3306);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:3306");
        assertThat(fields.getDatabase()).isEqualTo(Optional.of("hivedb"));
    }

    @Test
    public void testMysqlParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(MYSQL_URL);
        assertThat(fields.getVendorDriverId()).isEqualTo("mysql");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(3306);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:3306");
        assertThat(fields.getDatabase()).isEqualTo(Optional.of("hivedb"));
    }

    @Test
    public void testMysqlNoDatabaseParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(MYSQL_URL_WITHOUT_DATABASE_AND_SSL);
        assertThat(fields.getVendorDriverId()).isEqualTo("mysql");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(3306);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:3306");
        assertThat(fields.getDatabase().isPresent()).isFalse();
    }

    @Test
    public void testOracleParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(ORACLE_URL);
        assertThat(fields.getVendorDriverId()).isEqualTo("oracle");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(1521);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:1521");
        assertThat(fields.getDatabase()).isEqualTo(Optional.of("hivedb"));
    }

    @Test
    public void testOracleThinParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(ORACLE_THIN_URL);
        assertThat(fields.getVendorDriverId()).isEqualTo("oracle");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(1521);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:1521");
        assertThat(fields.getDatabase()).isEqualTo(Optional.of("hivedb"));
    }

    @Test
    public void testOracleThinNoDatabaseParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(ORACLE_THIN_URL_WITHOUT_DATABASE);
        assertThat(fields.getVendorDriverId()).isEqualTo("oracle");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(1521);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:1521");
        assertThat(fields.getDatabase().isPresent()).isFalse();
    }

    @Test
    public void testPostgresParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(POSTGRES_URL);
        assertThat(fields.getVendorDriverId()).isEqualTo("postgresql");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(5432);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:5432");
        assertThat(fields.getDatabase()).isEqualTo(Optional.of("hivedb"));
    }

    @Test
    public void testPostgresNoDatabaseParsing() {
        thrown.expect(IllegalArgumentException.class);

        databaseCommon.parseJdbcConnectionUrl(POSTGRES_URL_WITHOUT_DATABASE);
    }

    @Test
    public void testPostgresHyphenatedDatabaseParsing() {
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(POSTGRES_URL_WITH_HYPHENATED_DATABASE_NAME);
        assertThat(fields.getVendorDriverId()).isEqualTo("postgresql");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(5432);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:5432");
        assertThat(fields.getDatabase()).isEqualTo(Optional.of("hive-db-"));
    }

    @Test
    public void testPostgresConnectionUrlWithoutDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("postgresql", "test.eu-west-1.rds.amazonaws.com", 5432, Optional.empty());
        assertThat(url).isEqualTo(POSTGRES_URL_WITH_POSTGRES_DATABASE);
    }

    @Test
    public void testPostgresConnectionUrlWithDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("postgresql", "test.eu-west-1.rds.amazonaws.com", 5432, Optional.of("hivedb"));
        assertThat(url).isEqualTo(POSTGRES_URL);
    }

    @Test
    public void testMySQLConnectionUrlWithoutDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("mysql", "test.eu-west-1.rds.amazonaws.com", 3306, Optional.empty());
        assertThat(url).isEqualTo(MYSQL_URL_WITHOUT_DATABASE_AND_SSL);
    }

    @Test
    public void testMySQLConnectionUrlWithDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("mysql", "test.eu-west-1.rds.amazonaws.com", 3306, Optional.of("hivedb"));
        assertThat(url).isEqualTo(MYSQL_URL_WITHOUT_SSL);
    }

    @Test
    public void testOracleConnectionUrlWithoutDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("oracle", "test.eu-west-1.rds.amazonaws.com", 1521, Optional.empty());
        assertThat(url).isEqualTo(ORACLE_THIN_URL_WITHOUT_DATABASE);
    }

    @Test
    public void testOracleConnectionUrlWithDatabase() {
        String url = databaseCommon.getJdbcConnectionUrl("oracle", "test.eu-west-1.rds.amazonaws.com", 1521, Optional.of("hivedb"));
        assertThat(url).isEqualTo(ORACLE_THIN_URL);
    }

    @Test
    public void testPortCheck() {
        thrown.expect(IllegalArgumentException.class);
        new JdbcConnectionUrlFields("postgresql", "test.eu-west-1.rds.amazonaws.com", -1, Optional.empty());
    }

    @Test
    public void testExecuteUpdatesNoTransaction() throws SQLException {
        Connection conn = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate(any(String.class))).thenReturn(1);
        List<String> sqlStrings = List.of("sql1", "sql2");

        List<Integer> rowCounts = databaseCommon.executeUpdates(conn, sqlStrings, false);

        assertThat(rowCounts).isEqualTo(List.of(1, 1));
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

        assertThat(rowCounts).isEqualTo(List.of(1, 1));
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
        thrown.expect(SQLException.class);
        Connection conn = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate("sql1")).thenThrow(new SQLException("fail"));
        List<String> sqlStrings = List.of("sql1", "sql2");

        try {
            databaseCommon.executeUpdates(conn, sqlStrings, false);
        } finally {
            verify(statement).executeUpdate("sql1");
            verify(statement, never()).executeUpdate("sql2");
            verify(conn, never()).rollback();
        }
    }

    @Test
    public void testExecuteUpdatesFailTransaction() throws SQLException {
        thrown.expect(SQLException.class);
        Connection conn = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeUpdate("sql1")).thenThrow(new SQLException("fail"));
        List<String> sqlStrings = List.of("sql1", "sql2");

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
    }

    @Test
    public void testDbMajorVersionRegexp() {
        Pattern dbVersionPattern = Pattern.compile(POSTGRES_VERSION_REGEX);
        assertFalse("empty string", dbVersionPattern.matcher("").matches());
        assertFalse("not a version", dbVersionPattern.matcher("null").matches());
        assertTrue("valid version: 9.6", dbVersionPattern.matcher("9.6").matches());
        assertTrue("valid version: 10", dbVersionPattern.matcher("10").matches());
        assertTrue("valid version: 11", dbVersionPattern.matcher("11").matches());
        assertTrue("valid version: 12", dbVersionPattern.matcher("12").matches());
        assertTrue("valid version: 13", dbVersionPattern.matcher("13").matches());
        assertTrue("valid version: 14", dbVersionPattern.matcher("14").matches());
        assertFalse("not a valid version", dbVersionPattern.matcher("141").matches());
        assertFalse("valid version twice", dbVersionPattern.matcher("1111").matches());
    }
}
