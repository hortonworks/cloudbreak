package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;

@Service
public class PostgresConfigService {

    public static final String POSTGRES_COMMON = "postgres-common";

    public static final String POSTGRESQL_SERVER = "postgresql-server";

    @Value("${cb.recovery.database.reuse}")
    private List<String> databasesReusedDuringRecovery;

    @Inject
    private RdsConfigProviderFactory rdsConfigProviderFactory;

    @Inject
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Inject
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Inject
    private EmbeddedDatabaseConfigProvider embeddedDatabaseConfigProvider;

    public void decorateServicePillarWithPostgresIfNeeded(Map<String, SaltPillarProperties> servicePillar, Stack stack, Cluster cluster) {
        Map<String, Object> postgresConfig = initPostgresConfig(stack, cluster);

        Set<String> rootCerts = dbCertificateProvider.getRelatedSslCerts(stack, cluster);
        if (CollectionUtils.isNotEmpty(rootCerts)) {
            Map<String, String> rootSslCertsMap = Map.of("ssl_certs", String.join("\n", rootCerts),
                    "ssl_certs_file_path", dbCertificateProvider.getSslCertsFilePath());
            servicePillar.put(POSTGRES_COMMON, new SaltPillarProperties("/postgresql/root-certs.sls",
                    singletonMap("postgres_root_certs", rootSslCertsMap)));
        }

        if (!postgresConfig.isEmpty()) {
            servicePillar.put(POSTGRESQL_SERVER, new SaltPillarProperties("/postgresql/postgre.sls", singletonMap("postgres", postgresConfig)));
        }
    }

    public Set<RDSConfig> createRdsConfigIfNeeded(Stack stack, Cluster cluster) {
        return rdsConfigProviderFactory.getAllSupportedRdsConfigProviders().stream().map(provider ->
                provider.createPostgresRdsConfigIfNeeded(stack, cluster)).reduce((first, second) -> second).orElse(Collections.emptySet());
    }

    private Map<String, Object> initPostgresConfig(Stack stack, Cluster cluster) {
        Map<String, Object> postgresConfig = new HashMap<>();
        if (dbServerConfigurer.isRemoteDatabaseNeeded(cluster)) {
            postgresConfig.put("configure_remote_db", "true");
        } else {
            postgresConfig.putAll(embeddedDatabaseConfigProvider.collectEmbeddedDatabaseConfigs(stack));
        }
        if (CollectionUtils.isNotEmpty(databasesReusedDuringRecovery)) {
            postgresConfig.put("recovery_reused_databases", databasesReusedDuringRecovery);
        }
        rdsConfigProviderFactory.getAllSupportedRdsConfigProviders().forEach(provider ->
                postgresConfig.putAll(provider.createServicePillarConfigMapIfNeeded(stack, cluster)));
        return postgresConfig;
    }
}
