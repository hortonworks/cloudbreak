package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.upgrade.rds.UpgradeRdsBackupRestoreStateParamsProvider;

@ExtendWith(MockitoExtension.class)
class PostgresConfigServiceTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String POSTGRES_COMMON = "postgres-common";

    private static final String POSTGRESQL_SERVER = "postgresql-server";

    private static final String POSTGRES_VERSION = "postgres_version";

    private static final String DBVERSION = "dbversion";

    @Mock
    private RdsConfigProviderFactory rdsConfigProviderFactory;

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Mock
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Mock
    private EmbeddedDatabaseConfigProvider embeddedDatabaseConfigProvider;

    @Mock
    private UpgradeRdsBackupRestoreStateParamsProvider upgradeRdsBackupRestoreStateParamsProvider;

    @InjectMocks
    private PostgresConfigService underTest;

    @Mock
    private StackDto stack;

    @BeforeEach
    void setUp() {
        Cluster cluster = new Cluster();
        when(stack.getCluster()).thenReturn(cluster);
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslDisabled() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        when(stack.getStack()).thenReturn(new Stack());

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        assertThat(servicePillar).isEmpty();
        verify(upgradeRdsBackupRestoreStateParamsProvider, times(1)).createParamsForRdsBackupRestore(stack, "");
    }

    @Test
    void decorateServicePillarWithPostgresWhenReusedDatabaseListIsEmpty() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ReflectionTestUtils.setField(underTest, "databasesReusedDuringRecovery", List.of());
        when(stack.getStack()).thenReturn(new Stack());

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        assertThat(servicePillar).isEmpty();
    }

    @Test
    void decorateServicePillarWithPostgresWhenReusedDatabaseListIsNotEmpty() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ReflectionTestUtils.setField(underTest, "databasesReusedDuringRecovery", List.of("HIVE"));
        Stack stackView = new Stack();
        stackView.setExternalDatabaseEngineVersion(DBVERSION);
        when(stack.getStack()).thenReturn(stackView);

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
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslEnabled() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        rootCerts.add("cert2");
        when(dbCertificateProvider.getRelatedSslCerts(stack)).thenReturn(rootCerts);
        when(dbCertificateProvider.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        Stack stackView = new Stack();
        stackView.setExternalDatabaseEngineVersion(DBVERSION);
        when(stack.getStack()).thenReturn(stackView);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack);

        SaltPillarProperties saltPillarProperties = servicePillar.get(POSTGRES_COMMON);
        assertThat(saltPillarProperties).isNotNull();
        assertThat(saltPillarProperties.getPath()).isEqualTo("/postgresql/root-certs.sls");

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).hasSize(1);

        Map<String, String> rootSslCertsMap = (Map<String, String>) properties.get("postgres_root_certs");
        assertThat(rootSslCertsMap).isNotNull();
        assertThat(rootSslCertsMap).containsOnly(entry("ssl_certs", "cert1\ncert2"), entry("ssl_certs_file_path", SSL_CERTS_FILE_PATH));
    }

}