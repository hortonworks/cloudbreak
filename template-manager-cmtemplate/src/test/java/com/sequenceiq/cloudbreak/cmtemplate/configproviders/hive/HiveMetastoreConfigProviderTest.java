package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.TestUtil.kerberosConfigFreeipa;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.TestUtil.rdsConfigWithoutCluster;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToValueMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigTestUtil.getConfigNameToVariableNameMap;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_COMPACTOR_INITIATOR_ON;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_ENABLE_LDAP_AUTH;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_LDAP_BASEDN;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_METASTORE_LDAP_URI;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.HIVE_SERVICE_CONFIG_SAFETY_VALVE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.JDBC_URL_OVERRIDE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider.METASTORE_CANARY_HEALTH_ENABLED;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PORT;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;

@ExtendWith(MockitoExtension.class)
class HiveMetastoreConfigProviderTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    @Mock
    private CmTemplateProcessor templateProcessor;

    @InjectMocks
    private HiveMetastoreConfigProvider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "gcpExternalDatabaseSslVerificationMode", "verify-ca");
    }

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

        List<ApiClusterTemplateConfig> result = underTest.getRoleConfigs(HiveRoles.HIVEMETASTORE, templateProcessor, tpo);

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
                .withRdsViews(Set.of(createRdsConfig(null))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", false))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withProductDetails(generateCmRepo(cmVersion), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    private void verifyDbOnlyMinimalResultWorkloadStack(List<ApiClusterTemplateConfig> result) {
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme"),
                entry(HIVE_SERVICE_CONFIG_SAFETY_VALVE, "")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    private void verifyDbOnlyMinimalResult(List<ApiClusterTemplateConfig> result) {
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        assertThat(configNameToValueMap).containsOnly(
                entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme"),
                entry("hive_service_config_safety_valve", "<property><name>hive.metastore.try.direct.sql.ddl</name><value>true</value></property>" +
                        "<property><name>hive.metastore.try.direct.sql</name><value>true</value></property>")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @ParameterizedTest(name = "cloudPlatform={0}")
    @ValueSource(strings = {"AWS", "GCP"})
    void getServiceConfigsTestDbOnlyWithSsl(String cloudPlatform) {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(RdsSslMode.ENABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, SSL_CERTS_FILE_PATH, cloudPlatform, true))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        String sslMode = "GCP".equals(cloudPlatform) ? "verify-ca" : "verify-full";

        verifyDbOnlySslResult(result, sslMode, StackType.DATALAKE);
    }

    static Object[][] sslClusterDataProvider() {
        return new Object[][]{
                // cloudPlatform, externalDb
                {"AWS", true},
                {"AWS", false},
                {"GCP", true},
                {"GCP", false}
        };
    }

    @ParameterizedTest()
    @MethodSource("sslClusterDataProvider")
    void getServiceConfigsTestDbOnlyWithSslForDatalake(String cloudPlatform, boolean externalDb) {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(RdsSslMode.ENABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, SSL_CERTS_FILE_PATH, cloudPlatform, externalDb))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withDataLakeView(new DatalakeView(false, "crn", externalDb))
                .withCloudPlatform(CloudPlatform.valueOf(cloudPlatform))
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        String sslMode = "GCP".equals(cloudPlatform) &&  externalDb ? "verify-ca" : "verify-full";

        verifyDbOnlySslResult(result, sslMode, StackType.DATALAKE);
    }

    static Object[][] sslClusterDataProviderForDatahub() {
        return new Object[][]{
                // cloudPlatform, externalDbDatalake, externalDbDatahub
                {"AWS", true, false},
                {"AWS", false, false},
                {"GCP", true, false},
                {"GCP", false, false},
                {"AWS", true, true},
                {"AWS", false, true},
                {"GCP", true, true},
                {"GCP", false, true}
        };
    }

    @ParameterizedTest()
    @MethodSource("sslClusterDataProviderForDatahub")
    void getServiceConfigsTestDbOnlyWithSslForDatahub(String cloudPlatform, boolean externalDbDatalake, boolean externalDbDatahub) {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(RdsSslMode.ENABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, SSL_CERTS_FILE_PATH, cloudPlatform, externalDbDatahub))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.WORKLOAD)
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withDataLakeView(new DatalakeView(false, "crn", externalDbDatalake))
                .withCloudPlatform(CloudPlatform.valueOf(cloudPlatform))
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        String sslMode = "GCP".equals(cloudPlatform) &&  externalDbDatalake ? "verify-ca" : "verify-full";

        verifyDbOnlySslResult(result, sslMode, StackType.WORKLOAD);
    }

    private void verifyDbOnlySslResult(List<ApiClusterTemplateConfig> result, String sslMode, StackType stackType) {
        Map<String, String> configNameToValueMap = getConfigNameToValueMap(result);
        String hiveServiceConfigSafetyValve = "";
        if (stackType.equals(StackType.DATALAKE)) {
            hiveServiceConfigSafetyValve = "<property><name>hive.metastore.try.direct.sql.ddl</name><value>true</value></property>" +
                    "<property><name>hive.metastore.try.direct.sql</name><value>true</value></property>";
        }
        assertThat(configNameToValueMap).contains(
                entry(HIVE_METASTORE_DATABASE_HOST, "10.1.1.1"),
                entry(HIVE_METASTORE_DATABASE_NAME, "hive"),
                entry(HIVE_METASTORE_DATABASE_PASSWORD, "iamsoosecure"),
                entry(HIVE_METASTORE_DATABASE_PORT, "5432"),
                entry(HIVE_METASTORE_DATABASE_TYPE, "postgresql"),
                entry(HIVE_METASTORE_DATABASE_USER, "heyitsme"),
                entry(JDBC_URL_OVERRIDE, "jdbc:postgresql://10.1.1.1:5432/hive?sslmode=" + sslMode + "&sslrootcert=" + SSL_CERTS_FILE_PATH),
                entry("hive_service_config_safety_valve", hiveServiceConfigSafetyValve)
        );
        if (stackType.equals(StackType.WORKLOAD)) {
            assertThat(configNameToValueMap).contains(entry("hive_compactor_initiator_on", "false"));
        }
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
                .withRdsViews(Set.of(createRdsConfig(RdsSslMode.ENABLED))
                    .stream()
                    .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                    .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withProductDetails(generateCmRepo(cmVersion), null)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    @ParameterizedTest(name = "cloudPlatform={0}")
    @ValueSource(strings = {"AWS", "GCP"})
    void getServiceConfigsTestDbOnlyWithSslAndTemplateWithHarmlessHmsServiceConfigs(String cloudPlatform) {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(RdsSslMode.ENABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, SSL_CERTS_FILE_PATH, cloudPlatform, true))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withCloudPlatform(CloudPlatform.valueOf(cloudPlatform))
                .build();
        initHmsServiceConfigs(List.of(config("foo", "bar")));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        String sslMode = "GCP".equals(cloudPlatform) ? "verify-ca" : "verify-full";

        verifyDbOnlySslResult(result, sslMode, StackType.DATALAKE);
    }

    private void initHmsServiceConfigs(List<ApiClusterTemplateConfig> serviceConfigs) {
        ApiClusterTemplateService service = new ApiClusterTemplateService();
        service.setServiceConfigs(serviceConfigs);
        when(templateProcessor.getServiceByType(HiveRoles.HIVE)).thenReturn(Optional.of(service));
    }

    @ParameterizedTest(name = "cloudPlatform={0}")
    @ValueSource(strings = {"AWS", "GCP"})
    void getServiceConfigsTestDbOnlyWithSslAndTemplateWithHarmlessHmsServiceConfigsAndDummyCustomHmsDbKeys(String cloudPlatform) {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(RdsSslMode.ENABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, SSL_CERTS_FILE_PATH, cloudPlatform, true))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withRdsSslCertificateFilePath(SSL_CERTS_FILE_PATH)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();
        initHmsServiceConfigs(List.of(config("foo", "bar"), config(HIVE_METASTORE_DATABASE_HOST, null)));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        String sslMode = "GCP".equals(cloudPlatform) ? "verify-ca" : "verify-full";

        verifyDbOnlySslResult(result, sslMode, StackType.DATALAKE);
    }

    @Test
    void getServiceConfigsTestDbOnlyWithSslAndTemplateWithHarmlessHmsServiceConfigsAndCustomHmsDbOverride() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(RdsSslMode.ENABLED))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, SSL_CERTS_FILE_PATH, true))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .build();
        initHmsServiceConfigs(List.of(config("foo", "bar"), config(HIVE_METASTORE_DATABASE_HOST, "customhms.mydomain.com")));

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    @Test
    void getServiceConfigsTestDbAndKerberos() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(null))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
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
                entry(HIVE_SERVICE_CONFIG_SAFETY_VALVE, "<property><name>hive.hook.proto.file.per.event</name><value>true</value></property>" +
                        "<property><name>hive.metastore.try.direct.sql.ddl</name><value>true</value></property>" +
                        "<property><name>hive.metastore.try.direct.sql</name><value>true</value></property>")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    void getServiceConfigsTestDatalakeAndNoLdap() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(null))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withStackType(StackType.DATALAKE)
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withStackType(StackType.DATALAKE)
                .build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResult(result);
    }

    @Test
    void getServiceConfigsTestDatalakeWithLdap() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(null))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
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
                entry(HIVE_METASTORE_LDAP_BASEDN, "cn=users,dc=example,dc=org"),
                entry("hive_service_config_safety_valve", "<property><name>hive.metastore.try.direct.sql.ddl</name><value>true</value></property>" +
                        "<property><name>hive.metastore.try.direct.sql</name><value>true</value></property>")
        );
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test void getServiceConfigsTestDatahubCm710() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(null))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0), null)
                .withStackType(StackType.WORKLOAD).build();

        List<ApiClusterTemplateConfig> result = underTest.getServiceConfigs(templateProcessor, tpo);

        verifyDbOnlyMinimalResultWorkloadStack(result);
    }

    @Test void getServiceConfigsTestDatahubCm711() {
        TemplatePreparationObject tpo = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(null))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
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
                entry(HIVE_COMPACTOR_INITIATOR_ON, "false"),
                entry(HIVE_SERVICE_CONFIG_SAFETY_VALVE, ""));
        Map<String, String> configNameToVariableNameMap = getConfigNameToVariableNameMap(result);
        assertThat(configNameToVariableNameMap).isEmpty();
    }

    @Test
    void getServiceConfigHiveSafetyValve() {
        TemplatePreparationObject source = new TemplatePreparationObject.Builder()
                .withRdsViews(Set.of(createRdsConfig(null))
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withProductDetails(generateCmRepo(CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2), null)
                .withStackType(StackType.DATALAKE)
                .withLdapConfig(ldapConfig())
                .build();

        Map<String, String> result = getConfigNameToValueMap(underTest.getServiceConfigs(templateProcessor, source));
        assertThat(result.get("hive_service_config_safety_valve")).isEqualTo("<property><name>hive.metastore.try.direct.sql.ddl</name><value>true" +
                "</value></property><property><name>hive.metastore.try.direct.sql</name><value>true</value></property>");
    }

    private RdsConfigWithoutCluster createRdsConfig(RdsSslMode sslMode) {
        RdsConfigWithoutCluster rdsConfig = rdsConfigWithoutCluster(DatabaseType.HIVE, RdsSslMode.DISABLED);
        when(rdsConfig.getSslMode()).thenReturn(sslMode);
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