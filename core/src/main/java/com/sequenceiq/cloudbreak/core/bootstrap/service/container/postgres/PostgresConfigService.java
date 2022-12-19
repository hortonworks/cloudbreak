package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider.RedbeamsDbSslDetails;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.upgrade.rds.UpgradeRdsBackupRestoreStateParamsProvider;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class PostgresConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDatabaseConfigProvider.class);

    private static final String POSTGRES_COMMON = "postgres-common";

    private static final String POSTGRESQL_SERVER = "postgresql-server";

    private static final String POSTGRES_VERSION = "postgres_version";

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

    @Inject
    private UpgradeRdsBackupRestoreStateParamsProvider upgradeRdsBackupRestoreStateParamsProvider;

    @Inject
    private ClusterService clusterService;

    public void decorateServicePillarWithPostgresIfNeeded(Map<String, SaltPillarProperties> servicePillar, StackDto stackDto) {
        Map<String, Object> postgresConfig = initPostgresConfig(stackDto);

        String rootCertsBundle = null;
        boolean sslEnabled = false;
        if (stackDto.getCluster().getDbSslEnabled()) {
            if (StringUtils.isBlank(stackDto.getCluster().getDbSslRootCertBundle())) {
                RedbeamsDbSslDetails sslDetails = dbCertificateProvider.getRelatedSslCerts(stackDto);
                rootCertsBundle = clusterService.updateDbSslCert(stackDto.getCluster().getId(), sslDetails);
                sslEnabled = sslDetails.isSslEnabledForStack();
            } else {
                rootCertsBundle = stackDto.getCluster().getDbSslRootCertBundle();
                sslEnabled = stackDto.getCluster().getDbSslEnabled();
            }
        }
        if (StringUtils.isNotEmpty(rootCertsBundle)) {
            Map<String, String> rootSslCertsMap = Map.of("ssl_certs", rootCertsBundle,
                    "ssl_certs_file_path", dbCertificateProvider.getSslCertsFilePath(),
                    "ssl_enabled", String.valueOf(sslEnabled));
            servicePillar.put(POSTGRES_COMMON, new SaltPillarProperties("/postgresql/root-certs.sls",
                    singletonMap("postgres_root_certs", rootSslCertsMap)));
        }

        if (!postgresConfig.isEmpty()) {
            servicePillar.put(POSTGRESQL_SERVER, new SaltPillarProperties("/postgresql/postgre.sls", singletonMap("postgres", postgresConfig)));
        }
        servicePillar.putAll(upgradeRdsBackupRestoreStateParamsProvider.createParamsForRdsBackupRestore(stackDto, ""));
    }

    public Set<RdsConfigWithoutCluster> createRdsConfigIfNeeded(Stack stack, Cluster cluster, DatabaseType databaseType) {
        return rdsConfigProviderFactory.getRdsConfigProviderForRdsType(databaseType)
                .createPostgresRdsConfigIfNeeded(stack, cluster);
    }

    public Set<RdsConfigWithoutCluster> createRdsConfigIfNeeded(StackDtoDelegate stackDto) {
        return rdsConfigProviderFactory.getAllSupportedRdsConfigProviders().stream()
                .map(provider -> provider.createPostgresRdsConfigIfNeeded(stackDto))
                .reduce((first, second) -> second)
                .orElse(Collections.emptySet());
    }

    private Map<String, Object> initPostgresConfig(StackDto stackDto) {
        Map<String, Object> postgresConfig = new HashMap<>();
        if (dbServerConfigurer.isRemoteDatabaseNeeded(stackDto.getCluster().getDatabaseServerCrn())) {
            postgresConfig.put("configure_remote_db", "true");
        } else {
            postgresConfig.putAll(embeddedDatabaseConfigProvider.collectEmbeddedDatabaseConfigs(stackDto));
        }
        StackView stack = stackDto.getStack();
        if (StringUtils.isNotBlank(stack.getExternalDatabaseEngineVersion())) {
            LOGGER.debug("Configuring embedded DB version to [{}]", stack.getExternalDatabaseEngineVersion());
            postgresConfig.put(POSTGRES_VERSION, stack.getExternalDatabaseEngineVersion());
        }
        if (CollectionUtils.isNotEmpty(databasesReusedDuringRecovery)) {
            postgresConfig.put("recovery_reused_databases", databasesReusedDuringRecovery);
        }
        rdsConfigProviderFactory.getAllSupportedRdsConfigProviders().forEach(provider ->
                postgresConfig.putAll(provider.createServicePillarConfigMapIfNeeded(stackDto)));
        return postgresConfig;
    }
}
