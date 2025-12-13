package com.sequenceiq.cloudbreak.template.views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.views.provider.RdsViewProvider;

public class RdsViewProviderTest {

    private static final String ASSERT_ERROR_MSG = "The generated connection URL(connectionHost field) is not valid!";

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String SSL_OPTIONS_SUFFIX = "sslmode=verify-full&sslrootcert=" + SSL_CERTS_FILE_PATH;

    private RdsViewProvider underTest = new RdsViewProvider();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "gcpExternalDatabaseSslVerificationMode", "verify-ca");
        ReflectionTestUtils.setField(underTest, "rootCertsPath", "/default-path");
    }

    @Test
    public void testCreateRdsViewWhenConnectionUrlContainsSubprotocolAndSubname() {
        String connectionUrl = "jdbc:postgresql:subname://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getHost()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com");
        assertThat(underTest.getPort()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("5432");
        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger");
        assertThat(underTest.getSubprotocol()).isEqualTo("postgresql:subname");
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsProperConnectionUrl() {
        String connectionUrl = "jdbc:postgresql://some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

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

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getConnectionUserName()).isEqualTo("admin@some-rds");
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsConnectionUrlWithoutJDBCUrlPrefix() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getHost()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com");
        assertThat(underTest.getPort()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("5432");
        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger");
    }

    @Test
    public void testCreateRdsViewWhenRDSConfigContainsDoublePortInUrl() {
        String connectionUrl = "some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("some-rds.1d3nt1f13r.eu-west-1.rds.amazonaws.com:5432:5432/ranger");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenRangerAndPostgreSQL() {
        String connectionUrl = "jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, DatabaseType.RANGER);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenHiveAndPostgreSQL() {
        String connectionUrl = "jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.POSTGRES, DatabaseType.HIVE);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:postgresql://ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:5432/ranger");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenRangerAndOracle() {
        String connectionUrl = "jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12, DatabaseType.RANGER);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521/orcl");
    }

    @Test
    public void testCreateRdsViewConnectionStringWhenHiveAndOracle() {
        String connectionUrl = "jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12, DatabaseType.HIVE);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getConnectionString()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:oracle:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl");
    }

    @Test
    public void testCreateRdsViewDatabaseNameWhenOracle() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getDatabaseName()).withFailMessage(ASSERT_ERROR_MSG).isEqualTo("orcl");
    }

    @Test
    public void testCreateRdsViewWhenOracle() {
        String connectionUrl = "jdbc:oracle:thin:@ranger.cmseikcocinw.us-east-1.rds.amazonaws.com:1521:orcl";
        RDSConfig rdsConfig = createRdsConfig(connectionUrl, DatabaseVendor.ORACLE12);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

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

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

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

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getHostWithPortWithJdbc()).withFailMessage(ASSERT_ERROR_MSG)
                .isEqualTo("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306");
    }

    @Test
    public void testCreateRdsViewWhenMalformedUrl() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://foo.com", DatabaseVendor.MYSQL);

        assertThatCode(() -> underTest.getRdsView(rdsConfig, "AWS", true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testCreateRdsViewHostWithPortWithJdbcWhenQueryParameters() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger?foo=bar", DatabaseVendor.MYSQL);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getHostWithPortWithJdbc()).isEqualTo("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306");
    }

    @Test
    public void testCreateRdsViewUseSslWhenDisabled() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);
        rdsConfig.setSslMode(RdsSslMode.DISABLED);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.isUseSsl()).isFalse();
    }

    @Test
    public void testCreateRdsViewUseSslWhenEnabled() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);
        rdsConfig.setSslMode(RdsSslMode.ENABLED);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.isUseSsl()).isTrue();
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenNoFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "AWS", true);

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo("/default-path");
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenNullFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, null, "AWS", true);

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo("/default-path");
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenEmptyFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, "", "AWS", true);

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo("/default-path");
    }

    @Test
    public void testCreateRdsViewSslCertificateFilePathWhenValidFilePath() {
        RDSConfig rdsConfig = createRdsConfig("jdbc:mysql://ranger-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ranger", DatabaseVendor.MYSQL);

        RdsView underTest = this.underTest.getRdsView(rdsConfig, SSL_CERTS_FILE_PATH, "AWS", true);

        assertThat(underTest.getSslCertificateFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
    }

    static Object[][] embeddedDbAndCloudPlatformDataProvider() {
        return new Object[][]{
                // externalDb, cloudPlatform
                {false, "GCP"},
                {true, "GCP"},
                {false, "AWS"},
                {true, "AWS"}
        };
    }

    @ParameterizedTest(name = "externalDb={0}, cloudPlatform={1}")
    @MethodSource("embeddedDbAndCloudPlatformDataProvider")
    public void testCreateRdsViewWithRdsViewWithoutCluster(boolean externalDb, String cloudPlatform) {
        String sslMode = "GCP".equals(cloudPlatform) && externalDb ? "verify-ca" : "verify-full";
        RdsConfigWithoutCluster rdsView = mock(RdsConfigWithoutCluster.class);
        when(rdsView.isArchived()).thenReturn(true);
        when(rdsView.getConnectionDriver()).thenReturn("driver");
        when(rdsView.getConnectionURL()).thenReturn("jdbc:mysql://ozzie-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ozzie");
        when(rdsView.getConnectionPassword()).thenReturn("pass");
        when(rdsView.getConnectionUserName()).thenReturn("username");
        when(rdsView.getConnectorJarUrl()).thenReturn("jarurl");
        when(rdsView.getCreationDate()).thenReturn(1L);
        when(rdsView.getDatabaseEngine()).thenReturn(DatabaseVendor.MYSQL);
        when(rdsView.getDeletionTimestamp()).thenReturn(2L);
        when(rdsView.getDescription()).thenReturn("desc");
        when(rdsView.getId()).thenReturn(-1L);
        when(rdsView.getName()).thenReturn("name");
        when(rdsView.getSslMode()).thenReturn(RdsSslMode.ENABLED);
        when(rdsView.getType()).thenReturn("ozzie");

        RdsView underTest = this.underTest.getRdsView(rdsView, "ssl-path", cloudPlatform, externalDb);
        assertThat(underTest.getClusterManagerVendor()).isEqualTo("mysql");
        assertThat(underTest.getConnectionString())
                .isEqualTo("jdbc:mysql://ozzie-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ozzie?sslmode=" + sslMode
                        + "&sslrootcert=ssl-path");
        assertThat(underTest.getConnectionUserName()).isEqualTo("username");
        assertThat(underTest.getHostWithPortWithJdbc()).isEqualTo("jdbc:mysql://ozzie-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306");
        assertThat(underTest.getHost()).isEqualTo("ozzie-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com");
        assertThat(underTest.getConnectionDriver()).isEqualTo("driver");
        assertThat(underTest.getConnectionPassword()).isEqualTo("pass");
        assertThat(underTest.getConnectionURL())
                .isEqualTo("jdbc:mysql://ozzie-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ozzie?sslmode=" + sslMode
                                                + "&sslrootcert=ssl-path");
        assertThat(underTest.getDatabaseEngine()).isEqualTo("mysql");
        assertThat(underTest.getDatabaseType()).isEqualTo("mysql");
        assertThat(underTest.getDatabaseVendor()).isEqualTo(DatabaseVendor.MYSQL);
        assertThat(underTest.getDatabaseName()).isEqualTo("ozzie");
        assertThat(underTest.getFancyName()).isEqualTo("MySQL / MariaDB");
        assertThat(underTest.getLowerCaseDatabaseEngine()).isEqualTo("mysql");
        assertThat(underTest.getName()).isEqualTo("ozzie");
        assertThat(underTest.getPassword()).isEqualTo("pass");
        assertThat(underTest.getPort()).isEqualTo("3306");
        assertThat(underTest.getSslCertificateFilePath()).isEqualTo("ssl-path");
        assertThat(underTest.getSubprotocol()).isEqualTo("mysql");
        assertThat(underTest.getUserName()).isEqualTo("username");
        assertThat(underTest.getVendor()).isEqualTo("mysql");
        assertThat(underTest.getWithoutJDBCPrefix())
                .isEqualTo("ozzie-mysql.cmseikcocinw.us-east-1.rds.amazonaws.com:3306/ozzie?sslmode=" + sslMode
                        + "&sslrootcert=ssl-path");
        assertThat(underTest.isUseSsl()).isEqualTo(true);
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

        RdsView underTest = this.underTest.getRdsView(rdsConfig, SSL_CERTS_FILE_PATH, "AWS", true);

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
