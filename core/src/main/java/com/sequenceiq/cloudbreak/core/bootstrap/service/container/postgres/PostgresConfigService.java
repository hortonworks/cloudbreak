package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
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

@Service
public class PostgresConfigService {

    public static final String POSTGRESQL_SERVER = "postgresql-server";

    public static final String POSTGRES_ROTATION = "postgres-rotation";

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresConfigService.class);

    private static final String POSTGRES_COMMON = "postgres-common";

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

    @Inject
    private ClusterComponentConfigProvider clusterComponentProvider;

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
            servicePillar.put("postgres-rotation", getPillarPropertiesForRotation(stackDto));
        }
        servicePillar.putAll(upgradeRdsBackupRestoreStateParamsProvider.createParamsForRdsBackupRestore(stackDto, ""));
    }

    public SaltPillarProperties getPillarPropertiesForRotation(StackDto stackDto) {
        Map<String, Object> postgresConfig = new HashMap<>();
        rdsConfigProviderFactory.getAllSupportedRdsConfigProviders().forEach(provider ->
                postgresConfig.putAll(provider.createServicePillarConfigMapForRotation(stackDto)));
        return new SaltPillarProperties("/postgresql/rotation.sls", singletonMap("postgres-rotation", postgresConfig));
    }

    public SaltPillarProperties getPostgreSQLServerPropertiesForRotation(StackDto stackDto) {
        Map<String, Object> postgresConfig = initPostgresConfig(stackDto);
        return new SaltPillarProperties("/postgresql/postgre.sls", singletonMap("postgres", postgresConfig));
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
        sslSaltConfig.setSslForCmDbNativelySupported(isSslForCmDbNativelySupported(cluster));
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
        sslSaltConfig.setSslForCmDbNativelySupported(isSslForCmDbNativelySupported(stackDto.getCluster()));
        sslSaltConfig.setRestartRequired(false);
        return sslSaltConfig;
    }

    private boolean isSslForCmDbNativelySupported(ClusterView clusterView) {
        return isSslForCmDbNativelySupportedForCmVersion(getCmVersion(clusterView));
    }

    private String getCmVersion(ClusterView clusterView) {
        String cmVersion = null;
        Optional<ClouderaManagerRepo> cmRepoDetailsOpt = Optional.ofNullable(clusterComponentProvider.getClouderaManagerRepoDetails(clusterView.getId()));
        if (cmRepoDetailsOpt.isPresent()) {
            cmVersion = cmRepoDetailsOpt.get().getVersion();
            LOGGER.info("ClouderaManagerRepoDetails is available for stack, found CM version '{}'", cmVersion);
        } else {
            LOGGER.warn("ClouderaManagerRepoDetails is unavailable for stack, thus CM version cannot be determined.");
        }
        return cmVersion;
    }

    private boolean isSslForCmDbNativelySupportedForCmVersion(String cmVersion) {
        if (StringUtils.isBlank(cmVersion)) {
            LOGGER.info("CM version is NOT specified, thus CM DB native SSL support is NOT available");
            return false;
        }
        boolean available = CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(cmVersion, CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_9_2);
        LOGGER.info("CM DB native SSL support {} available for CM version {}", available ? "is" : "is NOT", cmVersion);
        return available;
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
        if (StringUtils.isNotBlank(stackDto.getExternalDatabaseEngineVersion())) {
            LOGGER.debug("Configuring embedded DB version to [{}]", stackDto.getExternalDatabaseEngineVersion());
            postgresConfig.put(POSTGRES_VERSION, stackDto.getExternalDatabaseEngineVersion());
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

        private boolean sslForCmDbNativelySupported;

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

        public boolean isSslForCmDbNativelySupported() {
            return sslForCmDbNativelySupported;
        }

        public void setSslForCmDbNativelySupported(boolean sslForCmDbNativelySupported) {
            this.sslForCmDbNativelySupported = sslForCmDbNativelySupported;
        }

        public Map<String, Object> toMap() {
            // Note: The Salt logic expects "ssl_enabled", "ssl_restart_required" and "ssl_for_cm_db_natively_supported" be all represented as strings,
            // not primitive booleans.
            return Map.of(
                    "ssl_certs", rootCertsBundle,
                    "ssl_restart_required", String.valueOf(restartRequired),
                    "ssl_enabled", String.valueOf(sslEnabled),
                    "ssl_for_cm_db_natively_supported", String.valueOf(sslForCmDbNativelySupported)
            );
        }
    }

}
