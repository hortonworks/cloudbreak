package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DatabaseCommonTest {
    private static final String ORACLE_URL = "jdbc:oracle:@test.eu-west-1.rds.amazonaws.com:1521/hivedb";

    private static final String MARIADB_URL = "jdbc:mariadb://test.eu-west-1.rds.amazonaws.com:3306/hivedb";

    private static final String MYSQL_URL = "jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306/hivedb?useSSL=true&requireSSL=false";

    private static final String MYSQL_URL_WITHOUT_PORT = "jdbc:mysql://test.eu-west-1.rds.amazonaws.com/hivedb?useSSL=true&requireSSL=false";

    private static final String POSTGRES_URL = "jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/hivedb";

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
}
