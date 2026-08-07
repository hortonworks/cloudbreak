package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.encryptionprofile.EncryptionProfileService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.upgrade.rds.UpgradeExternalRdsStateParamsProvider;
import com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class PostgresConfigServiceTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String POSTGRES_COMMON = "postgres-common";

    private static final String POSTGRESQL_SERVER = "postgresql-server";

    private static final String POSTGRES_VERSION = "postgres_version";

    private static final String DBVERSION = "dbversion";

    private static final Long CLUSTER_ID = 123L;

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    @Mock
    private RdsConfigProviderFactory rdsConfigProviderFactory;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private DatabaseSslService databaseSslService;

    @Mock
    private UpgradeExternalRdsStateParamsProvider upgradeExternalRdsStateParamsProvider;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private ExternalDatabaseConfig externalDatabaseConfig;

    @Mock
    private EncryptionProfileService encryptionProfileService;

    @Mock
    private EncryptionProfileProvider encryptionProfileProvider;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EnvironmentConfigProvider environmentConfigProvider;

    @Mock
    private EmbeddedDatabaseConfigProvider embeddedDatabaseConfigProvider;

    @InjectMocks
    private PostgresConfigService underTest;

    @Mock
    private StackDto stack;

    @BeforeEach
    void setUp() {
        Cluster cluster = new Cluster();
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(externalDatabaseConfig.getGcpExternalDatabaseSslVerificationMode()).thenReturn("verify-ca");
        lenient().when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslDisabledFromDatabaseSslService() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        Cluster cluster = new Cluster();
        cluster.setDbSslEnabled(false);
        cluster.setDbSslRootCertBundle(null);
        when(stack.getCluster()).thenReturn(cluster);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(new HashSet<>(), false));

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        assertThat(servicePillar).isEmpty();
        verify(upgradeExternalRdsStateParamsProvider, times(1)).createParamsForRdsBackupRestore(stack, "");
        verify(databaseSslService, never()).isDbSslEnabledByClusterView(any(StackView.class), any(ClusterView.class));
    }

    static Object[][] sslDisabledFromClusterDataProvider() {
        return new Object[][]{
                // dbSslEnabled, dbSslRootCertBundle
                {false, ""},
                {false, " "},
        };
    }

    @ParameterizedTest()
    @MethodSource("sslDisabledFromClusterDataProvider")
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslDisabledFromCluster(boolean dbSslEnabled, String dbSslRootCertBundle) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        StackView stackView = new Stack();
        when(stack.getStack()).thenReturn(stackView);
        Cluster cluster = new Cluster();
        cluster.setDbSslEnabled(dbSslEnabled);
        cluster.setDbSslRootCertBundle(dbSslRootCertBundle);
        when(stack.getCluster()).thenReturn(cluster);
        when(databaseSslService.isDbSslEnabledByClusterView(stackView, cluster)).thenReturn(dbSslEnabled);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        assertThat(servicePillar).isEmpty();
        verify(upgradeExternalRdsStateParamsProvider, times(1)).createParamsForRdsBackupRestore(stack, "");
        verify(databaseSslService, never()).getDbSslDetailsForCreationAndUpdateInCluster(any(StackDto.class));
    }

    @Test
    void decorateServicePillarWithPostgresWhenReusedDatabaseListWasEmpty() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ReflectionTestUtils.setField(underTest, "databasesReusedDuringRecovery", List.of());
        Cluster cluster = new Cluster();
        cluster.setDbSslEnabled(false);
        when(stack.getCluster()).thenReturn(cluster);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(new HashSet<>(), false));

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        assertThat(servicePillar).isEmpty();
        verify(databaseSslService, never()).isDbSslEnabledByClusterView(any(StackView.class), any(ClusterView.class));
    }

    @Test
    void decorateServicePillarWithPostgresWhenReusedDatabaseListIsNotEmpty() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ReflectionTestUtils.setField(underTest, "databasesReusedDuringRecovery", List.of("HIVE"));
        when(stack.getExternalDatabaseEngineVersion()).thenReturn(DBVERSION);
        Cluster cluster = new Cluster();
        cluster.setDbSslEnabled(false);
        when(stack.getCluster()).thenReturn(cluster);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(new HashSet<>(), false));

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        assertThat(servicePillar).isNotEmpty();
        SaltPillarProperties saltPillarProperties = servicePillar.get(POSTGRESQL_SERVER);
        assertThat(saltPillarProperties).isNotNull();
        assertThat(saltPillarProperties.getPath()).isEqualTo("/postgresql/postgre.sls");

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).hasSize(1);

        Map<String, Object> reusedDatabases = (Map<String, Object>) properties.get("postgres");
        assertThat(reusedDatabases).isNotNull();
        assertThat(reusedDatabases).containsOnly(entry("recovery_reused_databases", List.of("HIVE")), entry(POSTGRES_VERSION, DBVERSION));

        verify(databaseSslService, never()).isDbSslEnabledByClusterView(any(StackView.class), any(ClusterView.class));
    }

    static Object[][] sslAndCloudPlatformDataProvider() {
        return new Object[][]{
                // sslEnabledForStack, cloudProvider, externalDbServerCrn
                {false, "GCP", null},
                {false, "GCP", DB_SERVER_CRN},
                {true, "GCP", null},
                {true, "GCP", DB_SERVER_CRN},
                {false, "AWS", null},
                {false, "AWS", DB_SERVER_CRN},
                {true, "AWS", null},
                {true, "AWS", DB_SERVER_CRN}
        };
    }

    @ParameterizedTest(name = "sslEnabledForStack={0}, cloudProvider={1}, externalDbServerCrn={2}")
    @MethodSource("sslAndCloudPlatformDataProvider")
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslEnabled(boolean sslEnabledForStack, String cloudProvider, String externalDbServerCrn) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        rootCerts.add("cert2");
        Cluster cluster = new Cluster();
        cluster.setDbSslRootCertBundle(null);
        cluster.setId(CLUSTER_ID);
        cluster.setDatabaseServerCrn(externalDbServerCrn);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getExternalDatabaseEngineVersion()).thenReturn(DBVERSION);
        when(stack.getCloudPlatform()).thenReturn(cloudProvider);
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(rootCerts, sslEnabledForStack));
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        SaltPillarProperties saltPillarProperties = servicePillar.get(POSTGRES_COMMON);
        assertThat(saltPillarProperties).isNotNull();
        assertThat(saltPillarProperties.getPath()).isEqualTo("/postgresql/root-certs.sls");

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).hasSize(1);

        Map<String, Object> rootSslCertsMap = (Map<String, Object>) properties.get("postgres_root_certs");
        String sslVerificationMode = "GCP".equals(cloudProvider) && StringUtils.isNotEmpty(externalDbServerCrn) ? "verify-ca" : "verify-full";
        assertThat(rootSslCertsMap).isNotNull();
        assertThat(rootSslCertsMap).containsOnly(
                entry("ssl_certs", "cert1\ncert2"),
                entry("ssl_certs_file_path", SSL_CERTS_FILE_PATH),
                entry("ssl_restart_required", "false"),
                entry("ssl_for_cm_db_natively_supported", "false"),
                entry("ssl_enabled", String.valueOf(sslEnabledForStack)),
                entry("ssl_verification_mode", sslVerificationMode),
                entry("tls_advanced_control", "false"),
                entry("tls_min_version", ""),
                entry("tls_max_version", ""),
                entry("tls12_ciphers", ""),
                entry("tls13_ciphers", ""));

        verify(databaseSslService, never()).isDbSslEnabledByClusterView(any(StackView.class), any(ClusterView.class));
    }

    @ParameterizedTest(name = "sslEnabledForStack={0}, cloudProvider={1}")
    @MethodSource("sslAndCloudPlatformDataProvider")
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslEnabledAndRestartRequired(boolean sslEnabledForStack, String cloudProvider) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Stack stackView = new Stack();
        Cluster cluster = new Cluster();
        cluster.setDbSslEnabled(sslEnabledForStack);
        cluster.setDbSslRootCertBundle("cert1");
        cluster.setDatabaseServerCrn("crn");
        cluster.setId(CLUSTER_ID);
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        when(stack.getStack()).thenReturn(stackView);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn(cloudProvider);
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.isDbSslEnabledByClusterView(stackView, cluster)).thenReturn(sslEnabledForStack);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        SaltPillarProperties saltPillarProperties = servicePillar.get(POSTGRES_COMMON);
        assertThat(saltPillarProperties).isNotNull();
        assertThat(saltPillarProperties.getPath()).isEqualTo("/postgresql/root-certs.sls");

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).hasSize(1);

        Map<String, Object> rootSslCertsMap = (Map<String, Object>) properties.get("postgres_root_certs");
        String sslVerificationMode = "GCP".equals(cloudProvider) ? "verify-ca" : "verify-full";
        assertThat(rootSslCertsMap).isNotNull();
        assertThat(rootSslCertsMap).containsOnly(
                entry("ssl_certs", "cert1"),
                entry("ssl_certs_file_path", SSL_CERTS_FILE_PATH),
                entry("ssl_restart_required", "true"),
                entry("ssl_for_cm_db_natively_supported", "false"),
                entry("ssl_enabled", String.valueOf(sslEnabledForStack)),
                entry("ssl_verification_mode", sslVerificationMode),
                entry("tls_advanced_control", "false"),
                entry("tls_min_version", ""),
                entry("tls_max_version", ""),
                entry("tls12_ciphers", ""),
                entry("tls13_ciphers", ""));

        verify(databaseSslService, never()).getDbSslDetailsForCreationAndUpdateInCluster(any(StackDto.class));
    }

    static Object[][] sslForCmDbNativeSupportDataProvider() {
        return new Object[][]{
                // cmRepoDetailsAvailable, cmVersion, sslForCmDbNativelySupportedExpected, cloudPlatform
                {false, null, false, "AWS"},
                {true, null, false, "AWS"},
                {true, "", false, "AWS"},
                {true, " ", false, "AWS"},
                {true, "7.6.2", false, "AWS"},
                {true, "7.9.0", false, "AWS"},
                {true, "7.9.1", false, "AWS"},
                {true, "7.9.2", true, "AWS"},
                {true, "7.9.3", true, "AWS"},
                {true, "7.10.0", true, "AWS"},
                {false, null, false, "GCP"},
                {true, null, false, "GCP"},
                {true, "", false, "GCP"},
                {true, " ", false, "GCP"},
                {true, "7.6.2", false, "GCP"},
                {true, "7.9.0", false, "GCP"},
                {true, "7.9.1", false, "GCP"},
                {true, "7.9.2", true, "GCP"},
                {true, "7.9.3", true, "GCP"},
                {true, "7.10.0", true, "GCP"}
        };
    }

    @ParameterizedTest(name = "cmRepoDetailsAvailable={0}, cmVersion={1}, cloudProvider={3}")
    @MethodSource("sslForCmDbNativeSupportDataProvider")
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslEnabledAndCmDbNativeSupport(boolean cmRepoDetailsAvailable, String cmVersion,
            boolean sslForCmDbNativelySupportedExpected, String cloudProvider) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        rootCerts.add("cert2");
        Cluster cluster = new Cluster();
        cluster.setDbSslRootCertBundle(null);
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        cluster.setId(CLUSTER_ID);
        when(stack.getExternalDatabaseEngineVersion()).thenReturn(DBVERSION);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn(cloudProvider);
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(rootCerts, true));
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(cmRepoDetailsAvailable ? generateCmRepo(() -> cmVersion) : null);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        SaltPillarProperties saltPillarProperties = servicePillar.get(POSTGRES_COMMON);
        assertThat(saltPillarProperties).isNotNull();
        assertThat(saltPillarProperties.getPath()).isEqualTo("/postgresql/root-certs.sls");

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).hasSize(1);

        Map<String, Object> rootSslCertsMap = (Map<String, Object>) properties.get("postgres_root_certs");
        assertThat(rootSslCertsMap).isNotNull();
        String sslVerificationMode = "GCP".equals(cloudProvider) ? "verify-ca" : "verify-full";
        assertThat(rootSslCertsMap).containsOnly(
                entry("ssl_certs", "cert1\ncert2"),
                entry("ssl_certs_file_path", SSL_CERTS_FILE_PATH),
                entry("ssl_restart_required", "false"),
                entry("ssl_for_cm_db_natively_supported", String.valueOf(sslForCmDbNativelySupportedExpected)),
                entry("ssl_enabled", "true"),
                entry("ssl_verification_mode", sslVerificationMode),
                entry("tls_advanced_control", "false"),
                entry("tls_min_version", ""),
                entry("tls_max_version", ""),
                entry("tls12_ciphers", ""),
                entry("tls13_ciphers", ""));

        verify(databaseSslService, never()).isDbSslEnabledByClusterView(any(StackView.class), any(ClusterView.class));
    }

    private ClouderaManagerRepo generateCmRepo(Versioned version) {
        return new ClouderaManagerRepo()
                .withBaseUrl("baseurl")
                .withGpgKeyUrl("gpgurl")
                .withPredefined(true)
                .withVersion(version.getVersion());
    }

    @ParameterizedTest(name = "cmRepoDetailsAvailable={0}, cmVersion={1}, cloudProvider={3}")
    @MethodSource("sslForCmDbNativeSupportDataProvider")
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslEnabledAndCmDbNativeSupportAndRestartRequired(boolean cmRepoDetailsAvailable, String cmVersion,
            boolean sslForCmDbNativelySupportedExpected, String cloudProvider) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Stack stackView = new Stack();
        Cluster cluster = new Cluster();
        cluster.setDbSslEnabled(true);
        cluster.setDbSslRootCertBundle("cert1");
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        cluster.setId(CLUSTER_ID);
        when(stack.getStack()).thenReturn(stackView);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn(cloudProvider);
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.isDbSslEnabledByClusterView(stackView, cluster)).thenReturn(true);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(cmRepoDetailsAvailable ? generateCmRepo(() -> cmVersion) : null);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        SaltPillarProperties saltPillarProperties = servicePillar.get(POSTGRES_COMMON);
        assertThat(saltPillarProperties).isNotNull();
        assertThat(saltPillarProperties.getPath()).isEqualTo("/postgresql/root-certs.sls");

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).hasSize(1);

        Map<String, Object> rootSslCertsMap = (Map<String, Object>) properties.get("postgres_root_certs");
        assertThat(rootSslCertsMap).isNotNull();
        String sslVerificationMode = "GCP".equals(cloudProvider) ? "verify-ca" : "verify-full";
        assertThat(rootSslCertsMap).containsOnly(
                entry("ssl_certs", "cert1"),
                entry("ssl_certs_file_path", SSL_CERTS_FILE_PATH),
                entry("ssl_restart_required", "true"),
                entry("ssl_for_cm_db_natively_supported", String.valueOf(sslForCmDbNativelySupportedExpected)),
                entry("ssl_enabled", "true"),
                entry("ssl_verification_mode", sslVerificationMode),
                entry("tls_advanced_control", "false"),
                entry("tls_min_version", ""),
                entry("tls_max_version", ""),
                entry("tls12_ciphers", ""),
                entry("tls13_ciphers", ""));

        verify(databaseSslService, never()).getDbSslDetailsForCreationAndUpdateInCluster(any(StackDto.class));
    }

    static Object[][] encryptionProfileDataProvider() {
        return new Object[][]{
                {false, "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:test", false,
                        Set.of("TLSv1.2", "TLSv1.3"), "ECDHE-A", "TLS_AES_128_GCM_SHA256", "false", "", "", "", ""},
                {true,  "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:test", false,
                        Set.of("TLSv1.2", "TLSv1.3"),
                        "ECDHE-A:ECDHE-B", "TLS_AES_128_GCM_SHA256", "true", "TLSv1.2", "TLSv1.3", "ECDHE-A:ECDHE-B", "TLS_AES_128_GCM_SHA256"},
                {true,  "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:tls13", false,
                        Set.of("TLSv1.3"), "", "TLS_AES_256_GCM_SHA384", "true", "TLSv1.3", "TLSv1.3", "", "TLS_AES_256_GCM_SHA384"},
                {true,  "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:legacy", true,
                        Set.of("TLSv1.2"), "ECDHE-LEGACY", "", "false", "", "", "", ""},
        };
    }

    @ParameterizedTest(name = "entitlement={0}, encryptionProfile={1}, legacy={2}, tlsVersions={3}")
    @MethodSource("encryptionProfileDataProvider")
    void decorateServicePillarWithPostgresIfNeededPopulatesTlsCipherFields(boolean entitlementEnabled, String encryptionProfileCrn, boolean legacy,
            Set<String> tlsVersions, String openSsl12, String iana13, String expectedTlsAdv, String expectedMin, String expectedMax,
            String expectedT12, String expectedT13) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        Cluster cluster = new Cluster();
        cluster.setDbSslRootCertBundle(null);
        cluster.setId(CLUSTER_ID);
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        cluster.setEncryptionProfileCrn(encryptionProfileCrn);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getAccountId()).thenReturn("acct");
        when(stack.getExternalDatabaseEngineVersion()).thenReturn("17");
        lenient().when(stack.getEnvironmentCrn()).thenReturn("env-crn");
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(rootCerts, true));
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);
        when(entitlementService.isConfigureEncryptionProfileEnabled("acct")).thenReturn(entitlementEnabled);
        if (entitlementEnabled) {
            EncryptionProfileResponse profile = new EncryptionProfileResponse();
            profile.setName(legacy ? "cdp_default_fips_v1" : "custom");
            profile.setTlsVersions(tlsVersions);
            Map<String, List<String>> cipherSuites = new HashMap<>();
            profile.setCipherSuites(cipherSuites);
            when(encryptionProfileService.getEncryptionProfile(stack, null)).thenReturn(profile);
            if (!legacy) {
                when(encryptionProfileProvider.getOpenSslCipherSuites(cipherSuites, CipherSuitesLimitType.DEFAULT, false)).thenReturn(openSsl12);
                when(encryptionProfileProvider.getTls13CipherSuites(cipherSuites)).thenReturn(iana13);
            }
        }

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        Map<String, Object> rootSslCertsMap = (Map<String, Object>) servicePillar.get(POSTGRES_COMMON).getProperties().get("postgres_root_certs");
        assertThat(rootSslCertsMap).containsOnly(
                entry("ssl_certs", "cert1"),
                entry("ssl_certs_file_path", SSL_CERTS_FILE_PATH),
                entry("ssl_restart_required", "false"),
                entry("ssl_for_cm_db_natively_supported", "false"),
                entry("ssl_enabled", "true"),
                entry("ssl_verification_mode", "verify-full"),
                entry("tls_advanced_control", expectedTlsAdv),
                entry("tls_min_version", expectedMin),
                entry("tls_max_version", expectedMax),
                entry("tls12_ciphers", expectedT12),
                entry("tls13_ciphers", expectedT13));
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededPropagatesWhenEncryptionProfileServiceFails() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        Cluster cluster = new Cluster();
        cluster.setDbSslRootCertBundle(null);
        cluster.setId(CLUSTER_ID);
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        cluster.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:default:encryptionProfile:test");
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getAccountId()).thenReturn("acct");
        when(stack.getExternalDatabaseEngineVersion()).thenReturn("17");
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(rootCerts, true));
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);
        when(entitlementService.isConfigureEncryptionProfileEnabled("acct")).thenReturn(true);
        when(encryptionProfileService.getEncryptionProfile(stack, null)).thenThrow(new RuntimeException("boom"));

        assertThat(catchThrowable(() -> underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
        assertThat(servicePillar).doesNotContainKey(POSTGRES_COMMON);
    }

    @ParameterizedTest(name = "postgres version = {0}")
    @ValueSource(strings = {"10", "11", "14"})
    @NullSource
    void decorateServicePillarWithPostgresIfNeededSkipsEnrichmentOnNonPostgres17(String dbEngineVersion) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        Cluster cluster = new Cluster();
        cluster.setDbSslRootCertBundle(null);
        cluster.setId(CLUSTER_ID);
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        cluster.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:default:encryptionProfile:test");
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getExternalDatabaseEngineVersion()).thenReturn(dbEngineVersion);
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(rootCerts, true));
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        Map<String, Object> rootSslCertsMap = (Map<String, Object>) servicePillar.get(POSTGRES_COMMON).getProperties().get("postgres_root_certs");

        assertThat(rootSslCertsMap).contains(
                entry("tls_advanced_control", "false"),
                entry("tls_min_version", ""),
                entry("tls_max_version", ""),
                entry("tls12_ciphers", ""),
                entry("tls13_ciphers", ""));
        verify(encryptionProfileService, never()).getEncryptionProfile(any(), any());
        verify(entitlementService, never()).isConfigureEncryptionProfileEnabled(any());
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededPassesCallerSuppliedEnvironmentResponseToEncryptionProfileService() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        Cluster cluster = new Cluster();
        cluster.setDbSslRootCertBundle(null);
        cluster.setId(CLUSTER_ID);
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        cluster.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:default:encryptionProfile:test");
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getAccountId()).thenReturn("acct");
        when(stack.getExternalDatabaseEngineVersion()).thenReturn("17");
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(rootCerts, true));
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);
        when(entitlementService.isConfigureEncryptionProfileEnabled("acct")).thenReturn(true);
        DetailedEnvironmentResponse caller = new DetailedEnvironmentResponse();
        EncryptionProfileResponse profile = new EncryptionProfileResponse();
        profile.setName("custom");
        profile.setTlsVersions(Set.of("TLSv1.2", "TLSv1.3"));
        profile.setCipherSuites(new HashMap<>());
        when(encryptionProfileService.getEncryptionProfile(stack, caller)).thenReturn(profile);
        when(encryptionProfileProvider.getOpenSslCipherSuites(any(), eq(CipherSuitesLimitType.DEFAULT), eq(false))).thenReturn("ECDHE-A");
        when(encryptionProfileProvider.getTls13CipherSuites(any())).thenReturn("TLS_AES_128_GCM_SHA256");

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, caller);

        verify(encryptionProfileService, times(1)).getEncryptionProfile(stack, caller);
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededSkipsEnrichmentOnLegacyEncryptionProfile() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        Cluster cluster = new Cluster();
        cluster.setDbSslRootCertBundle(null);
        cluster.setId(CLUSTER_ID);
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        cluster.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:default:encryptionProfile:legacy");
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getAccountId()).thenReturn("acct");
        when(stack.getExternalDatabaseEngineVersion()).thenReturn("17");
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stack)).thenReturn(new DatabaseSslDetails(rootCerts, true));
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);
        when(entitlementService.isConfigureEncryptionProfileEnabled("acct")).thenReturn(true);
        EncryptionProfileResponse legacyProfile = new EncryptionProfileResponse();
        legacyProfile.setName("cdp_default_fips_v1");
        when(encryptionProfileService.getEncryptionProfile(stack, null)).thenReturn(legacyProfile);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        Map<String, Object> rootSslCertsMap = (Map<String, Object>) servicePillar.get(POSTGRES_COMMON).getProperties().get("postgres_root_certs");

        assertThat(rootSslCertsMap).contains(
                entry("tls_advanced_control", "false"),
                entry("tls_min_version", ""),
                entry("tls_max_version", ""),
                entry("tls12_ciphers", ""),
                entry("tls13_ciphers", ""));
        verifyNoInteractions(encryptionProfileProvider);
    }

    @Test
    void testCreateRdsConfigIfNeededDelegatesAllProvidersAndReturnsLastResult() {
        AbstractRdsConfigProvider provider1 = mock(AbstractRdsConfigProvider.class);
        AbstractRdsConfigProvider provider2 = mock(AbstractRdsConfigProvider.class);
        RdsConfigWithoutCluster rdsConfig1 = mock(RdsConfigWithoutCluster.class);
        RdsConfigWithoutCluster rdsConfig2 = mock(RdsConfigWithoutCluster.class);
        RdsConfigWithoutCluster rdsConfig3 = mock(RdsConfigWithoutCluster.class);

        when(rdsConfigProviderFactory.getAllSupportedRdsConfigProviders())
                .thenReturn(new java.util.LinkedHashSet<>(List.of(provider1, provider2)));
        when(provider1.createPostgresRdsConfigIfNeeded(stack)).thenReturn(Set.of(rdsConfig1));
        when(provider2.createPostgresRdsConfigIfNeeded(stack)).thenReturn(Set.of(rdsConfig2, rdsConfig3));

        Set<RdsConfigWithoutCluster> result = underTest.createRdsConfigIfNeeded(stack);

        assertThat(result).size().isEqualTo(2);
        assertThat(result).containsExactlyInAnyOrder(rdsConfig2, rdsConfig3);
        verify(provider1).createPostgresRdsConfigIfNeeded(stack);
        verify(provider2).createPostgresRdsConfigIfNeeded(stack);
    }

    @Test
    void testCreateRdsConfigIfNeededReturnsEmptyWhenNoProviders() {
        when(rdsConfigProviderFactory.getAllSupportedRdsConfigProviders())
                .thenReturn(java.util.Collections.emptySet());

        Set<RdsConfigWithoutCluster> result = underTest.createRdsConfigIfNeeded(stack);

        assertThat(result).isEmpty();
    }

    @Test
    void testUploadServicePillarsForPostgres() throws CloudbreakOrchestratorFailedException {
        ExitCriteriaModel exitCriteriaModel = mock(ExitCriteriaModel.class);
        OrchestratorStateParams orchestratorStateParams = mock(OrchestratorStateParams.class);
        StackView stackView = new Stack();
        when(stack.getStack()).thenReturn(stackView);
        Cluster cluster = new Cluster();
        cluster.setDbSslEnabled(false);
        cluster.setDbSslRootCertBundle("");
        when(stack.getCluster()).thenReturn(cluster);
        when(databaseSslService.isDbSslEnabledByClusterView(stackView, cluster)).thenReturn(false);


        underTest.uploadServicePillarsForPostgres(stack, exitCriteriaModel, orchestratorStateParams);

        verify(hostOrchestrator, times(1)).saveCustomPillars(any(), eq(exitCriteriaModel), eq(orchestratorStateParams));
    }
}