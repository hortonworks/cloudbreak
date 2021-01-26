package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfigFreeipa;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToValueMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToVariableNameMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_DATABASE_PORT;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_DATABASE_USER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_ENABLE_LDAP_AUTH;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_LDAP_BASEDN;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_LDAP_URI;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_SERVICE_CONFIG_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.JDBC_URL_OVERRIDE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.METASTORE_CANARY_HEALTH_ENABLED;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_COMPACTOR_INITIATOR_ON;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class HiveMetastoreConfigProviderTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    @Mock
    private CmTemplateProcessor templateProcessor;

    @InjectMocks
    private HiveMetastoreConfigProvider underTest;

    @Test
    void getServiceTypeTest() {
        assertThat(underTest.getServiceType()).isEqualTo(HiveRoles.HIVE);
    }

    @Test
    void getRoleTypesTest() {
        assertThat(underTest.getRoleTypes()).containsOnly(HiveRoles.HIVEMETASTORE);
    }

    @Test
    void dbTypeTest() {
        assertThat(underTest.dbType()).isEqualTo(DatabaseType.HIVE);
    }

    @Test
    void getRoleConfigsTest() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(HiveRoles.HIVEMETASTORE, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(METASTORE_CANARY_HEALTH_ENABLED, Boolean.FALSE.toString())
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    static Object[][] cmVersionMinimalDataProvider() {
        return new Object[][]{
                // testCaseName cmVersion
                {"CM 7.2.0", CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0},
                {"CM 7.2.1", CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_1},
                {"CM 7.2.2", CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cmVersionMinimalDataProvider")
    void getServiceConfigsTestDbOnlyAndNoSsl(String testCaseName, Versioned cmVersion) {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(null)))
                .withProductDetails(generateCmRepo(cmVersion), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    private void verifyDbOnlyMinimalResult(List<ApiClusterTemplateConfig> result) {
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    void getServiceConfigsTestDbOnlyWithSsl() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(RdsSslMode.ENABLED)))
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlySslResult(result);
    }

    private void verifyDbOnlySslResult(List<ApiClusterTemplateConfig> result) {
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme"),
                entry(JDBC_URL_OVERRIDE, "jdbc:postgresql://10.1.1.1:5432/hive?sslmode=verify-full&sslrootcert=" + SSL_CERTS_FILE_PATH)
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    static Object[][] cmVersionNoSslSupportDataProvider() {
        return new Object[][]{
                // testCaseName cmVersion
                {"CM 7.2.0", CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0},
                {"CM 7.2.1", CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_1},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cmVersionNoSslSupportDataProvider")
    void getServiceConfigsTestDbOnlyWithSslButWrongCmVersion(String testCaseName, Versioned cmVersion) {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(RdsSslMode.ENABLED)))
                .withProductDetails(generateCmRepo(cmVersion), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    @Test
    void getServiceConfigsTestDbOnlyWithSslAndTemplateWithHarmlessHmsServiceConfigs() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(RdsSslMode.ENABLED)))
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();
        initHmsServiceConfigs(List.of(config("foo", "bar")));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlySslResult(result);
    }

    private void initHmsServiceConfigs(List<ApiClusterTemplateConfig> serviceConfigs) {
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setServiceConfigs(serviceConfigs);
        when(templateProcessor.getServiceByType(HiveRoles.HIVE)).thenReturn(Optional.of(service));
    }

    @Test
    void getServiceConfigsTestDbOnlyWithSslAndTemplateWithHarmlessHmsServiceConfigsAndDummyCustomHmsDbKeys() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(RdsSslMode.ENABLED)))
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();
        initHmsServiceConfigs(List.of(config("foo", "bar"), config(HIVE_METASTORE_DATABASE_HOST, null)));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlySslResult(result);
    }

    @Test
    void getServiceConfigsTestDbOnlyWithSslAndTemplateWithHarmlessHmsServiceConfigsAndCustomHmsDbOverride() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(RdsSslMode.ENABLED)))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();
        initHmsServiceConfigs(List.of(config("foo", "bar"), config(HIVE_METASTORE_DATABASE_HOST, "customhms.mydomain.com")));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    @Test
    void getServiceConfigsTestDbAndKerberos() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(null)))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withKerberosConfig(kerberosConfigFreeipa())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme"),
                entry(HIVE_SERVICE_CONFIG_SAFETY_VALVE, "<property><name>hive.hook.proto.file.per.event</name><value>true</value></property>")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    void getServiceConfigsTestDatalakeAndNoLdap() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(null)))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    @Test
    void getServiceConfigsTestDatalakeWithLdap() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(null)))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withStackType(StackType.DATALAKE)
                .withLdapConfig(ldapConfig())
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme"),
                entry(HIVE_METASTORE_ENABLE_LDAP_AUTH, Boolean.TRUE.toString()),
                entry(HIVE_METASTORE_LDAP_URI, "ldap://localhost:389"),
                entry(HIVE_METASTORE_LDAP_BASEDN, "cn=users,dc=example,dc=org")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test void getServiceConfigsTestDatahubCm710() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(null)))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .withStackType(StackType.WORKLOAD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    @Test void getServiceConfigsTestDatahubCm711() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsConfigs(Set.of(createRdsConfig(null)))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_1), null)
                .withStackType(StackType.WORKLOAD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap)
            .containsOnly(entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme"),
                entry(HIVE_COMPACTOR_INITIATOR_ON, "false"));
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    private RDSConfig createRdsConfig(RdsSslMode sslMode) {
        RDSConfig rdsConfig = rdsConfig(DatabaseType.HIVE);
        rdsConfig.setSslMode(sslMode);
        return rdsConfig;
    }

    private ClouderaManagerRepo generateCmRepo(Versioned version) {
        return new ClouderaManagerRepo()
                .withBaseUrl("baseurl")
                .withGpgKeyUrl("gpgurl")
                .withPredefined(true)
                .withVersion(version.getVersion());
    }

}