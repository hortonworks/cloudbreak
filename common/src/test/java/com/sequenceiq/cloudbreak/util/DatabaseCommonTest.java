package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

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

    @Test
    public void testMariadbParsing() {
        assertThat(DatabaseCommon.getDatabaseType(MARIADB_URL).isEmpty()).isTrue();
    }

    @Test
    public void testMysqlParsing() {
        assertThat(DatabaseCommon.getDatabaseType(MYSQL_URL).get()).isEqualTo("mysql");
        assertHostParsing(MYSQL_URL, "test.eu-west-1.rds.amazonaws.com:3306", "hivedb");
    }

    @Test
    public void testOracleParsing() {
        assertHostParsing(ORACLE_URL, "test.eu-west-1.rds.amazonaws.com:1521", "hivedb");
        assertThat(DatabaseCommon.getDatabaseType(ORACLE_URL).get()).isEqualTo("oracle");
    }

    @Test
    public void testPostgresParsing() {
        assertHostParsing(POSTGRES_URL, "test.eu-west-1.rds.amazonaws.com:5432", "hivedb");
        assertThat(DatabaseCommon.getDatabaseType(POSTGRES_URL).get()).isEqualTo("postgresql");
    }

    @Test
    public void testMysqlParsingWithoutPort() {
        assertThat(DatabaseCommon.getHostPortAndDatabaseName(MYSQL_URL_WITHOUT_PORT).isEmpty()).isTrue();
    }

    private void assertHostParsing(String url, String hostAndPort, String databaseName) {
        HostAndPortAndDatabaseName mysqlInfo = DatabaseCommon.getHostPortAndDatabaseName(url).get();

        assertThat(mysqlInfo.getHostAndPort()).isEqualTo(hostAndPort);
        assertThat(mysqlInfo.getDatabaseName()).isEqualTo(databaseName);
    }

    @Test
    public void testPostgresConnectionURLWithoutDatabase() {
        String url = DatabaseCommon.getConnectionURL("postgresql", "test.eu-west-1.rds.amazonaws.com", 5432, Optional.empty());
        assertThat(url).isEqualTo(POSTGRES_URL_WITHOUT_DATABASE);
    }

    @Test
    public void testPostgresConnectionURLWithDatabase() {
        String url = DatabaseCommon.getConnectionURL("postgresql", "test.eu-west-1.rds.amazonaws.com", 5432, Optional.of("hivedb"));
        assertThat(url).isEqualTo(POSTGRES_URL);
    }

    @Test
    public void testMySQLConnectionURLWithoutDatabase() {
        String url = DatabaseCommon.getConnectionURL("mysql", "test.eu-west-1.rds.amazonaws.com", 3306, Optional.empty());
        assertThat(url).isEqualTo(MYSQL_URL_WITHOUT_DATABASE_AND_SSL);
    }

    @Test
    public void testMySQLConnectionURLWithDatabase() {
        String url = DatabaseCommon.getConnectionURL("mysql", "test.eu-west-1.rds.amazonaws.com", 3306, Optional.of("hivedb"));
        assertThat(url).isEqualTo(MYSQL_URL_WITHOUT_SSL);
    }

    @Test
    public void testOracleConnectionURLWithoutDatabase() {
        String url = DatabaseCommon.getConnectionURL("oracle", "test.eu-west-1.rds.amazonaws.com", 1521, Optional.empty());
        assertThat(url).isEqualTo(ORACLE_THIN_URL_WITHOUT_DATABASE);
    }

    @Test
    public void testOracleConnectionURLWithDatabase() {
        String url = DatabaseCommon.getConnectionURL("oracle", "test.eu-west-1.rds.amazonaws.com", 1521, Optional.of("hivedb"));
        assertThat(url).isEqualTo(ORACLE_THIN_URL);
    }
}
