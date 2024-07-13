package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.upgrade.rds.UpgradeRdsBackupRestoreStateParamsProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

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
    private EmbeddedDatabaseConfigProvider embeddedDatabaseConfigProvider;

    @Mock
    private DatabaseSslService databaseSslService;

    @Mock
    private UpgradeRdsBackupRestoreStateParamsProvider upgradeRdsBackupRestoreStateParamsProvider;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private ExternalDatabaseConfig externalDatabaseConfig;

    @InjectMocks
    private PostgresConfigService underTest;

    @Mock
    private StackDto stack;

    @BeforeEach
    void setUp() {
        Cluster cluster = new Cluster();
        when(stack.getCluster()).thenReturn(cluster);
        lenient().when(externalDatabaseConfig.getGcpExternalDatabaseSslVerificationMode()).thenReturn("verify-ca");
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
        verify(upgradeRdsBackupRestoreStateParamsProvider, times(1)).createParamsForRdsBackupRestore(stack, "");
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
        verify(upgradeRdsBackupRestoreStateParamsProvider, times(1)).createParamsForRdsBackupRestore(stack, "");
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
                entry("ssl_verification_mode", sslVerificationMode));

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
                entry("ssl_verification_mode", sslVerificationMode));

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
                entry("ssl_verification_mode", sslVerificationMode));

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
                entry("ssl_verification_mode", sslVerificationMode));

        verify(databaseSslService, never()).getDbSslDetailsForCreationAndUpdateInCluster(any(StackDto.class));
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