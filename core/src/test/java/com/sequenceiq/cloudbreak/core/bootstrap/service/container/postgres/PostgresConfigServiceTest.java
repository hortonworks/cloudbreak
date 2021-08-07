package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService.POSTGRESQL_SERVER;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService.POSTGRES_COMMON;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;

@ExtendWith(MockitoExtension.class)
class PostgresConfigServiceTest {

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    @Mock
    private RdsConfigProviderFactory rdsConfigProviderFactory;

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Mock
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Mock
    private EmbeddedDatabaseConfigProvider embeddedDatabaseConfigProvider;

    @InjectMocks
    private PostgresConfigService underTest;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        cluster = new Cluster();
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslDisabled() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

        assertThat(servicePillar).isEmpty();
    }

    @Test
    void decorateServicePillarWithPostgresWhenReusedDatabaseListIsEmpty() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ReflectionTestUtils.setField(underTest, "databasesReusedDuringRecovery", List.of());
        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

        assertThat(servicePillar).isEmpty();
    }

    @Test
    void decorateServicePillarWithPostgresWhenReusedDatabaseListIsNotEmpty() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ReflectionTestUtils.setField(underTest, "databasesReusedDuringRecovery", List.of("HIVE"));
        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

        assertThat(servicePillar).isNotEmpty();

        SaltPillarProperties saltPillarProperties = servicePillar.get(POSTGRESQL_SERVER);
        assertThat(saltPillarProperties).isNotNull();
        assertThat(saltPillarProperties.getPath()).isEqualTo("/postgresql/postgre.sls");

        Map<String, Object> properties = saltPillarProperties.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).hasSize(1);

        Map<String, List<String>> reusedDatabases = (Map<String, List<String>>) properties.get("postgres");
        assertThat(reusedDatabases).isNotNull();
        assertThat(reusedDatabases).containsOnly(entry("recovery_reused_databases", List.of("HIVE")));
    }

    @Test
    void decorateServicePillarWithPostgresIfNeededTestCertsWhenSslEnabled() {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        Set<String> rootCerts = new LinkedHashSet<>();
        rootCerts.add("cert1");
        rootCerts.add("cert2");
        when(dbCertificateProvider.getRelatedSslCerts(stack, cluster)).thenReturn(rootCerts);
        when(dbCertificateProvider.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);

        underTest.decorateServicePillarWithPostgresIfNeeded(servicePillar, stack, cluster);

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