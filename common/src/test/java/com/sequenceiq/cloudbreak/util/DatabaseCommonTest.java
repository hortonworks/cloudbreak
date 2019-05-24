package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.sequenceiq.cloudbreak.util.DatabaseCommon.JdbcConnectionUrlFields;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
        JdbcConnectionUrlFields fields = databaseCommon.parseJdbcConnectionUrl(POSTGRES_URL_WITHOUT_DATABASE);
        assertThat(fields.getVendorDriverId()).isEqualTo("postgresql");
        assertThat(fields.getHost()).isEqualTo("test.eu-west-1.rds.amazonaws.com");
        assertThat(fields.getPort()).isEqualTo(5432);
        assertThat(fields.getHostAndPort()).isEqualTo("test.eu-west-1.rds.amazonaws.com:5432");
        assertThat(fields.getDatabase().isPresent()).isFalse();
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
        assertThat(url).isEqualTo(POSTGRES_URL_WITHOUT_DATABASE);
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
}
