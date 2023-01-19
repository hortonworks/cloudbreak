package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

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
import com.sequenceiq.cloudbreak.dto.DatabaseSslDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.service.upgrade.rds.UpgradeRdsBackupRestoreStateParamsProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class PostgresConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresConfigService.class);

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
    private EmbeddedDatabaseConfigProvider embeddedDatabaseConfigProvider;

    @Inject
    private DatabaseSslService databaseSslService;

    @Inject
    private UpgradeRdsBackupRestoreStateParamsProvider upgradeRdsBackupRestoreStateParamsProvider;

    public void decorateServicePillarWithPostgresIfNeeded(Map<String, SaltPillarProperties> servicePillar, StackDto stackDto) {
        Map<String, Object> postgresConfig = initPostgresConfig(stackDto);
        SSLSaltConfig sslSaltConfig;
        if (stackDto.getCluster().getDbSslRootCertBundle() == null) {
            sslSaltConfig = getSslSaltConfigWhenRootCertNull(stackDto);
        } else {
            sslSaltConfig = getSslSaltConfigWhenRootCertAlreadyInitialized(stackDto);
        }
        if (StringUtils.isNotBlank(sslSaltConfig.getRootCertsBundle())) {
            generateDatabaseSSLConfiguration(servicePillar, sslSaltConfig);
        }

        if (!postgresConfig.isEmpty()) {
            servicePillar.put(POSTGRESQL_SERVER, new SaltPillarProperties("/postgresql/postgre.sls", singletonMap("postgres", postgresConfig)));
        }
        servicePillar.putAll(upgradeRdsBackupRestoreStateParamsProvider.createParamsForRdsBackupRestore(stackDto, ""));
    }

    private SSLSaltConfig getSslSaltConfigWhenRootCertAlreadyInitialized(StackDto stackDto) {
        SSLSaltConfig sslSaltConfig = new SSLSaltConfig();
        ClusterView cluster = stackDto.getCluster();
        String dbSslRootCertBundle = cluster.getDbSslRootCertBundle();
        boolean dbSslEnabled = databaseSslService.isDbSslEnabledByClusterView(stackDto.getStack(), cluster);
        LOGGER.info("Cluster.dbSslRootCertBundle is not null. Using SslDetails from Cluster: dbSslRootCertBundle='{}', dbSslEnabled={}", dbSslRootCertBundle,
                dbSslEnabled);
        sslSaltConfig.setRootCertsBundle(dbSslRootCertBundle);
        sslSaltConfig.setSslEnabled(dbSslEnabled);
        sslSaltConfig.setRestartRequired(true);
        return sslSaltConfig;
    }

    private SSLSaltConfig getSslSaltConfigWhenRootCertNull(StackDto stackDto) {
        SSLSaltConfig sslSaltConfig = new SSLSaltConfig();
        LOGGER.info("Cluster.dbSslRootCertBundle is null. Fetching SslDetails from DatabaseSslService");
        DatabaseSslDetails sslDetails = databaseSslService.getDbSslDetailsForCreationAndUpdateInCluster(stackDto);
        LOGGER.info("Fetched SslDetails: {}", sslDetails);
        sslSaltConfig.setRootCertsBundle(sslDetails.getSslCertBundle());
        sslSaltConfig.setSslEnabled(sslDetails.isSslEnabledForStack());
        sslSaltConfig.setRestartRequired(false);
        return sslSaltConfig;
    }

    private void generateDatabaseSSLConfiguration(Map<String, SaltPillarProperties> servicePillar, SSLSaltConfig sslSaltConfig) {
        Map<String, Object> rootSslCertsMap = new HashMap<>(sslSaltConfig.toMap());
        rootSslCertsMap.put("ssl_certs_file_path", databaseSslService.getSslCertsFilePath());
        servicePillar.put(POSTGRES_COMMON, new SaltPillarProperties("/postgresql/root-certs.sls",
                singletonMap("postgres_root_certs", rootSslCertsMap)));
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
        if (dbServerConfigurer.isRemoteDatabaseRequested(stackDto.getCluster().getDatabaseServerCrn())) {
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

    static class SSLSaltConfig {
        private String rootCertsBundle = "";

        private boolean sslEnabled;

        private boolean restartRequired;

        public String getRootCertsBundle() {
            return rootCertsBundle;
        }

        public void setRootCertsBundle(String rootCertsBundle) {
            this.rootCertsBundle = requireNonNull(rootCertsBundle);
        }

        public boolean isSslEnabled() {
            return sslEnabled;
        }

        public void setSslEnabled(boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
        }

        public boolean isRestartRequired() {
            return restartRequired;
        }

        public void setRestartRequired(boolean restartRequired) {
            this.restartRequired = restartRequired;
        }

        public Map<String, Object> toMap() {
            // Note: The Salt logic expects "ssl_enabled" and "ssl_restart_required" both be represented as strings, not primitive booleans.
            return Map.of(
                    "ssl_certs", rootCertsBundle,
                    "ssl_restart_required", String.valueOf(restartRequired),
                    "ssl_enabled", String.valueOf(sslEnabled)
            );
        }
    }

}
