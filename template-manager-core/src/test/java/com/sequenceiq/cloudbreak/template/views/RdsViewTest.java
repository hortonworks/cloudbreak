package com.sequenceiq.cloudbreak.template.views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;

public class RdsViewTest {

    private static final String ASSERT_ERROR_MSG = "The generated connection URL(connectionHost field) is not valid!";

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String SSL_OPTIONS_SUFFIX = "sslmode=verify-full&sslrootcert=" + SSL_CERTS_FILE_PATH;

    @Test
    public void testCreateRdsViewWhenConnectionUrlContainsSubprotocolAndSubname() {
        String connectionUrl = "jdbc:postgresql:subname://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getHost()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com");
        assertThat(underTest.getPort()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("5432");
        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger");
        assertThat(underTest.getSubprotocol()).isEqualTo("postgresql:subname");
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsProperConnectionUrl() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getHost()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com");
        assertThat(underTest.getPort()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("5432");
        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger");
        assertThat(underTest.getSubprotocol()).isEqualTo("postgresql");

        assertThat(underTest.getConnectionUserName()).isEqualTo("admin");
        assertThat(underTest.getConnectionPassword()).isEqualTo("adminpassword");
    }

    // Do pass the Azure-specific hostname suffix to CM. See CB-3791.
    @Test
    public void testCreateRdsViewWhenConnectionUserNameHasSuffix() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);
        rdsConfig.setConnectionUserName(rdsConfig.getConnectionUserName() + "@some-rds");

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getConnectionUserName()).isEqualTo("admin@some-rds");
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutJDBCUrlPrefix() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getHost()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com");
        assertThat(underTest.getPort()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("5432");
        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger");
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsDoublePortInUrl() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432/ranger");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenRangerAndPostgreSQL() {
        String connectionUrl = "jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, DatabaseType.RANGER);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenHiveAndPostgreSQL() {
        String connectionUrl = "jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, DatabaseType.HIVE);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenRangerAndOracle() {
        String connectionUrl = "jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12, DatabaseType.RANGER);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/orcl");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenHiveAndOracle() {
        String connectionUrl = "jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12, DatabaseType.HIVE);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl");
    }

    @Test
    public void testCreateRdsViewDatabaseNameWhenOracle() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("orcl");
    }

    @Test
    public void testCreateRdsViewWhenOracle() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getPort()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("1521");
        assertThat(underTest.getHost()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger.cmseikcocinw.us-east-1.rds.amazonaws.com");
        assertThat(underTest.getSubprotocol()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("oracle:thin");
        assertThat(underTest.getHostWithPortWithJdbc()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521");
    }

    @Test
    public void testCreateRdsViewWhenOracleWithService() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/XE";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getPort()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("1521");
        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("XE");
        assertThat(underTest.getHost()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger.cmseikcocinw.us-east-1.rds.amazonaws.com");
        assertThat(underTest.getSubprotocol()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("oracle:thin");
        assertThat(underTest.getHostWithPortWithJdbc()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521");
        assertThat(underTest.getWithoutJDBCPrefix()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/XE");
    }

    @Test
    public void testCreateRdsViewWhenCutDatabaseNameIfContainsMoreThanOne() {
        String connectionUrl = "jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.MYSQL);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getHostWithPortWithJdbc()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306");
    }

    @Test
    public void testCreateRdsViewWhenMalformedUrl() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://foo.com", DatabaseVendor.MYSQL);

        assertThatCode(() -> new RdsView(rdsConfig)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testCreateRdsViewHostWithPortWithJdbcWhenQueryParameters() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger?foo=bar", DatabaseVendor.MYSQL);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getHostWithPortWithJdbc()).isEqualTo("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306");
    }

    @Test
    public void testCreateRdsViewUseSslWhenDisabled() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);
        rdsConfig.setSslMode(RdsSslMode.DISABLED);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.isUseSsl()).isFalse();
    }

    @Test
    public void testCreateRdsViewUseSslWhenEnabled() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);
        rdsConfig.setSslMode(RdsSslMode.ENABLED);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.isUseSsl()).isTrue();
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenNoFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = new RdsView(rdsConfig);

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo("");
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenNullFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = new RdsView(rdsConfig, null);

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo("");
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenEmptyFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = new RdsView(rdsConfig, "");

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo("");
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenValidFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = new RdsView(rdsConfig, SSL_CERTS_FILE_PATH);

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
    }

    static Object[][] sslConnectionUrlDataProvider() {
        return new Object[][]{
                // testCaseName connectionUrl connectionUrlExpected
                {"No query parameters", "jdbc:mysql://foo.com:3306/hive", "jdbc:mysql://foo.com:3306/hive?" + SSL_OPTIONS_SUFFIX},
                {"Empty query parameters", "jdbc:mysql://foo.com:3306/hive?", "jdbc:mysql://foo.com:3306/hive?" + SSL_OPTIONS_SUFFIX},
                {"Query parameters ending in ampersand", "jdbc:mysql://foo.com:3306/hive?foo=bar&",
                        "jdbc:mysql://foo.com:3306/hive?foo=bar&" + SSL_OPTIONS_SUFFIX},
                {"Query parameters ending in regular char", "jdbc:mysql://foo.com:3306/hive?foo=bar",
                        "jdbc:mysql://foo.com:3306/hive?foo=bar&" + SSL_OPTIONS_SUFFIX},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslConnectionUrlDataProvider")
    public void testCreateRdsViewSslConnectionUrl(String testCaseName, String connectionUrl, String connectionUrlExpected) {
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.MYSQL);
        rdsConfig.setSslMode(RdsSslMode.ENABLED);

        RdsView underTest = new RdsView(rdsConfig, SSL_CERTS_FILE_PATH);

        assertThat(underTest.getConnectionURL()).isEqualTo(connectionUrlExpected);
    }

    private RDSConfig createRdsConfig(String connectionUrl) {
        return createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, DatabaseType.HIVE);
    }

    private RDSConfig createRdsConfig(String connectionUrl, DatabaseVendor databaseVendor) {
        return createRdsConfig(connectionUrl, databaseVendor, DatabaseType.HIVE);
    }

    private RDSConfig createRdsConfig(String connectionUrl, DatabaseVendor databaseVendor, DatabaseType databaseType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL(connectionUrl);
        rdsConfig.setConnectionPassword("adminpassword");
        rdsConfig.setConnectionUserName("admin");
        rdsConfig.setDatabaseEngine(databaseVendor);
        rdsConfig.setType(databaseType.name());
        return rdsConfig;
    }

}
