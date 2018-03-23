package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterComponentRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;

@Service
@Transactional
public class AmbariDatabaseToRdsConfigMigrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDatabaseToRdsConfigMigrationService.class);

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private AmbariDatabaseMapper ambariDatabaseMapper;

    @Inject
    private ClusterComponentRepository clusterComponentRepository;

    @Inject
    private ClusterRepository clusterRepository;

    public void migrateAmbariDatabaseClusterComponentsToRdsConfig() {
        Set<ClusterComponent> clusterComponents = clusterComponentRepository.findByComponentType(ComponentType.AMBARI_DATABASE_DETAILS);
        LOGGER.info("Mapping of AmbariDatabaseClusterComponents to RdsConfig. {} components to map", clusterComponents.size());
        clusterComponents.forEach(this::migrateClusterComponent);
    }

    private void migrateClusterComponent(ClusterComponent component) {
        Cluster cluster = component.getCluster();
        LOGGER.debug("Mapping component with id: {} from cluster name: [{}] id: [{}] and stack name: [{}] id: [{}]",
                component.getId(), cluster.getName(), cluster.getId(), cluster.getStack().getDisplayName(), cluster.getStack().getId());
        try {
            if (cluster.getStatus() != Status.DELETE_COMPLETED) {
                RDSConfig rdsConfig = createRdsConfig(component, cluster);
                addRdsConfigToCluster(cluster, rdsConfig);
            }
            markClusterComponentAsMigrated(component);
        } catch (IOException e) {
            LOGGER.error("Could not read component with id [{}]", component.getId(), e);
        }
    }

    private void markClusterComponentAsMigrated(ClusterComponent component) {
        component.setComponentType(ComponentType.AMBARI_DATABASE_DETAILS_MIGRATED);
        clusterComponentRepository.save(component);
        LOGGER.debug("Component migration finished with id [{}]", component.getId());
    }

    private void addRdsConfigToCluster(Cluster cluster, RDSConfig rdsConfig) {
        if (cluster.getRdsConfigs() == null) {
            cluster.setRdsConfigs(new HashSet<>());
        }
        if (cluster.getRdsConfigs().stream().noneMatch(rdsConf -> RdsType.AMBARI.name().equalsIgnoreCase(rdsConf.getType()))) {
            cluster.getRdsConfigs().add(rdsConfig);
            clusterRepository.save(cluster);
        } else {
            cluster.getRdsConfigs().stream().findFirst().ifPresent(rdsConf -> {
                LOGGER.warn("RdsConfig with AMBARI type already exists for cluster [{}] RdsConfig id: [{}]", cluster.getId(), rdsConf.getId());
            });
        }
    }

    private RDSConfig createRdsConfig(ClusterComponent component, Cluster cluster) throws IOException {
        LOGGER.debug("Creating RdsConfig for component id: [{}]", component.getId());
        AmbariDatabaseDetailsJson ambariDatabaseDetailsJson = ambariDatabaseMapper.mapAmbariDatabaseToAmbariDatabaseDetailJson(
                component.getAttributes().get(AmbariDatabase.class));
        Stack stack = cluster.getStack();
        RDSConfig rdsConfig = ambariDatabaseMapper.mapAmbariDatabaseDetailsJsonToRdsConfig(ambariDatabaseDetailsJson, stack);
        return rdsConfigService.create(rdsConfig);
    }

}
