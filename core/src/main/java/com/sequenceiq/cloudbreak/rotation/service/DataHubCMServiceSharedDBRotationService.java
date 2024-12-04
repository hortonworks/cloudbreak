package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy.FALLBACK_TO_ROLLCONFIG;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation.CREATION;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation.DELETION;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DataHubCMServiceSharedDBRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataHubCMServiceSharedDBRotationService.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private SharedDBRotationUtils sharedDBRotationUtils;

    public void rotateSharedServiceDbSecretOnDataHub(StackDto stack) {
        SdxBasicView sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stack.getEnvironmentCrn()).orElseThrow();
        if (TargetPlatform.CDL.equals(sdxBasicView.platform())) {
            throw new SecretRotationException("Shared service DB user/password rotation for Data Hub connected to a CDL is not supported yet!");
        }
        StackDto datalakeStack = stackService.getByCrn(sdxBasicView.crn());
        if (!datalakeStack.isAvailable()) {
            throw new SecretRotationException("Data Lake is not available, which is a requirement for shared service DB user/password rotation!");
        }
        String jdbcConnectionUrl = sharedDBRotationUtils.getJdbcConnectionUrl(sdxBasicView.dbServerCrn());
        Set<RDSConfig> rdsConfigsByUrl = rdsConfigService.findAllByConnectionUrlAndTypeWithClusters(jdbcConnectionUrl);
        Optional<RDSConfig> currentOwnHmsRdsConfig = rdsConfigsByUrl.stream()
                .filter(rdsConfig -> rdsConfig.getClusters().size() == 1)
                .filter(rdsConfig -> Objects.equals(rdsConfig.getClusters().iterator().next().getId(), stack.getCluster().getId()))
                .findFirst();
        String newDbUser = sharedDBRotationUtils.getNewDatabaseUserName(stack);
        if (currentOwnHmsRdsConfig.isEmpty()) {
            LOGGER.info("Creating separated HMS database user/password for Data Hub based on one used by Data Lake.");
            Long datalakeClusterId = stackService.getByCrn(sdxBasicView.crn()).getCluster().getId();
            RDSConfig datalakeHmsRdsConfig = rdsConfigsByUrl.stream()
                    .filter(rdsConfig -> rdsConfig.getClusters().stream().map(Cluster::getId).anyMatch(id -> Objects.equals(id, datalakeClusterId)))
                    .findFirst()
                    .orElseThrow();
            executeDbUserOperation(datalakeStack, newDbUser, CREATION);
            RDSConfig pooledHmsRdsConfig = replaceCurrentRdsConfigWithPooledRdsConfig(stack, datalakeHmsRdsConfig);
            updateConfigInCM(pooledHmsRdsConfig, stack);
        } else {
            LOGGER.info("Replacing separated HMS database user/password of Data Hub.");
            executeDbUserOperation(datalakeStack, newDbUser, CREATION);
            RDSConfig pooledHmsRdsConfig = replaceCurrentRdsConfigWithPooledRdsConfig(stack, currentOwnHmsRdsConfig.get());
            updateConfigInCM(pooledHmsRdsConfig, stack);
            executeDbUserOperation(datalakeStack, currentOwnHmsRdsConfig.get().getConnectionUserName(), DELETION);
        }
    }

    private void executeDbUserOperation(StackDto datalakeStack, String dbUser, ExternalDatabaseUserOperation operation) {
        FlowIdentifier flowIdentifier = stackOperationService.manageDatabaseUser(datalakeStack.getResourceCrn(), dbUser, HIVE.name(), operation.name());
        sharedDBRotationUtils.pollFlow(flowIdentifier);
    }

    private RDSConfig replaceCurrentRdsConfigWithPooledRdsConfig(StackDto stackDto, RDSConfig currentHmsRdsConfig) {
        Set<RDSConfig> rdsConfigsByUrl = rdsConfigService.findAllByConnectionUrlAndTypeWithClusters(currentHmsRdsConfig.getConnectionURL());
        RDSConfig pooledHmsRdsConfig = rdsConfigsByUrl.stream().filter(rdsConfig -> rdsConfig.getClusters().isEmpty()).findFirst().orElseThrow();
        Cluster cluster = clusterService.getByIdWithLists(stackDto.getCluster().getId());
        Set<RDSConfig> clusterRdsConfigs = new HashSet<>(cluster.getRdsConfigs());
        clusterRdsConfigs.add(pooledHmsRdsConfig);
        clusterRdsConfigs.remove(currentHmsRdsConfig);
        cluster.setRdsConfigs(clusterRdsConfigs);
        clusterService.save(cluster);
        return pooledHmsRdsConfig;
    }

    private void updateConfigInCM(RDSConfig rdsConfig, StackDto stackDto) {
        try {
            clusterApiConnectors.getConnector(stackDto)
                    .clusterModificationService()
                    .updateConfig(sharedDBRotationUtils.getConfigTableForRotationInCM(rdsConfig), FALLBACK_TO_ROLLCONFIG);
        } catch (Exception e) {
            LOGGER.error("Failed to replace configuration in CM: ", e);
            throw new RuntimeException(e);
        }
    }
}
