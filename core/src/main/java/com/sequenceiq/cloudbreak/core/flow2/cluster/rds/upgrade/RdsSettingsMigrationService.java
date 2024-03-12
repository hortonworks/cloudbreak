package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;

@Service
public class RdsSettingsMigrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdsSettingsMigrationService.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_SALT_RETRY = 10;

    private static final int MAX_PILLAR_RETRY = 100;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private Set<AbstractRdsConfigProvider> rdsConfigProviders;

    @Inject
    private Set<AbstractRdsRoleConfigProvider> rdsRoleConfigProviders;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    public Set<RDSConfig> collectRdsConfigs(Long clusterId, Predicate<RDSConfig> rdsConfigPredicate) {
        return rdsConfigService.findByClusterId(clusterId)
                .stream()
                .filter(rdsConfigPredicate)
                .filter(rdsCfg -> rdsConfigProviders.stream().anyMatch(configProvider -> matchesRdsTypeWithDatabaseType(configProvider.getRdsType(), rdsCfg)))
                .collect(Collectors.toSet());
    }

    public Set<RDSConfig> updateRdsConfigs(StackDto stackDto, Set<RDSConfig> rdsConfigs) {
        LOGGER.debug("Updating rds config connections to the upgraded database in cloudbreak database.");
        boolean dataHubCluster = StackType.WORKLOAD.equals(stackDto.getStack().getType());
        Set<RDSConfig> updatableConfigs = rdsConfigs.stream()
                .filter(rdsConfig -> isConfigUpdateRequired(rdsConfig, dataHubCluster))
                .collect(Collectors.toSet());
        updatableConfigs.forEach(this::updateUserName);
        rdsConfigService.pureSaveAll(updatableConfigs);
        return updatableConfigs;
    }

    public Table<String, String, String> collectCMServiceConfigs(Set<RDSConfig> rdsConfigs) {
        Table<String, String, String> cmServiceConfigTable = HashBasedTable.create();
        rdsConfigs.forEach(rdsConfig -> {
            Optional<AbstractRdsRoleConfigProvider> rdsRoleConfigProvider = rdsRoleConfigProviders.stream()
                    .filter(configProvider -> configProvider.dbType().equals(DatabaseType.valueOf(rdsConfig.getType()))).findFirst();
            if (rdsRoleConfigProvider.isPresent()) {
                AbstractRdsRoleConfigProvider roleConfigProvider = rdsRoleConfigProvider.get();
                cmServiceConfigTable.put(roleConfigProvider.getServiceType(), roleConfigProvider.dbUserKey(), rdsConfig.getConnectionUserName());
            }
        });
        return cmServiceConfigTable;
    }

    public void updateCMServiceConfigs(StackDtoDelegate stackDto, Table<String, String, String> cmServiceConfigs) throws Exception {
        clusterApiConnectors.getConnector(stackDto).clusterModificationService().updateConfigWithoutRestart(cmServiceConfigs);
    }

    public void updateSaltPillars(StackDto stackDto, Long clusterId) throws CloudbreakOrchestratorFailedException {
        Map<String, SaltPillarProperties> pillarPropertiesSet = Maps.newHashMap();
        pillarPropertiesSet.put(ClusterHostServiceRunner.CM_DATABASE_PILLAR_KEY,
                clusterHostServiceRunner.getClouderaManagerDatabasePillarProperties(clusterId));
        pillarPropertiesSet.put(PostgresConfigService.POSTGRESQL_SERVER, postgresConfigService.getPostgreSQLServerPropertiesForRotation(stackDto));
        LOGGER.debug("Updating db related pillars for db server upgrade: {}", String.join(",", pillarPropertiesSet.keySet()));
        hostOrchestrator.saveCustomPillars(new SaltConfig(pillarPropertiesSet), exitCriteriaProvider.get(stackDto),
                saltStateParamsService.createStateParams(stackDto, null, true, MAX_PILLAR_RETRY, MAX_RETRY_ON_ERROR));
    }

    public void updateCMDatabaseConfiguration(StackDto stackDto) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Updating CM db.properties configuration file according to the db settings changes.");
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        hostOrchestrator.executeSaltState(primaryGatewayConfig, Set.of(primaryGatewayConfig.getHostname()), List.of("cloudera.manager.update-db-user"),
                exitCriteriaProvider.get(stackDto), Optional.of(MAX_SALT_RETRY), Optional.of(MAX_RETRY_ON_ERROR));
    }

    private boolean matchesRdsTypeWithDatabaseType(DatabaseType rdsType, RDSConfig rdsConfig) {
        return rdsType.name().equals(rdsConfig.getType());
    }

    /**
     * Configuration is required in the following cases:
     * - if the cluster is a datalake
     * - if the cluster is a datahub and the rdsconfig is for the datahub (not for the shared db with the datalake)
     * @param rdsCfg the RdsConfig for the database
     * @param dataHubCluster true if datahub, false if datalake
     * @return true if config update is required
     */
    private boolean isConfigUpdateRequired(RDSConfig rdsCfg, boolean dataHubCluster) {
        return !(dataHubCluster && rdsConfigService.getClustersUsingResource(rdsCfg).size() > 1);
    }

    private void updateUserName(RDSConfig rdsConfig) {
        String originalUserName = rdsConfig.getConnectionUserName();
        String newUserName = originalUserName.split("@")[0];
        LOGGER.debug("Updating rds config connection {}'s user name from {} to {}", rdsConfig.getName(),
                originalUserName, newUserName);
        rdsConfig.setConnectionUserName(newUserName);
    }
}
