package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class AmbariDatabaseToRdsConfigMigrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDatabaseToRdsConfigMigrationService.class);

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private AmbariDatabaseMapper ambariDatabaseMapper;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterService clusterService;

    public void migrateAmbariDatabaseClusterComponentsToRdsConfig() {
        Set<ClusterComponent> clusterComponents = clusterComponentConfigProvider.findByComponentType(ComponentType.AMBARI_DATABASE_DETAILS);
        LOGGER.info("Mapping of AmbariDatabaseClusterComponents to RdsConfig. {} components to map", clusterComponents.size());
        clusterComponents.forEach(this::migrateClusterComponent);
    }

    private void migrateClusterComponent(ClusterComponent component) {
        Cluster cluster = component.getCluster();
        LOGGER.debug("Mapping component with id: {} from cluster name: [{}] id: [{}]", component.getId(), cluster.getName(), cluster.getId());
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
        clusterComponentConfigProvider.store(component);
        LOGGER.debug("Component migration finished with id [{}]", component.getId());
    }

    private void addRdsConfigToCluster(Cluster cluster, RDSConfig rdsConfig) {
        if (cluster.getRdsConfigs() == null) {
            cluster.setRdsConfigs(new HashSet<>());
        }
        if (cluster.getRdsConfigs().stream().noneMatch(rdsConf -> RdsType.AMBARI.name().equalsIgnoreCase(rdsConf.getType()))) {
            cluster.getRdsConfigs().add(rdsConfig);
            clusterService.save(cluster);
        } else {
            cluster.getRdsConfigs().stream().findFirst().ifPresent(rdsConf ->
                    LOGGER.warn("RdsConfig with AMBARI type already exists for cluster [{}] RdsConfig id: [{}]", cluster.getId(), rdsConf.getId()));
        }
    }

    private RDSConfig createRdsConfig(ClusterComponent component, Cluster cluster) throws IOException {
        LOGGER.debug("Creating RdsConfig for component id: [{}]", component.getId());
        AmbariDatabaseDetailsJson ambariDatabaseDetailsJson = ambariDatabaseMapper.mapAmbariDatabaseToAmbariDatabaseDetailJson(
                component.getAttributes().get(AmbariDatabase.class));
        RDSConfig rdsConfig = ambariDatabaseMapper.mapAmbariDatabaseDetailsJsonToRdsConfig(ambariDatabaseDetailsJson, cluster, null);
        if (DatabaseVendor.EMBEDDED == rdsConfig.getDatabaseEngine()) {
            rdsConfig.setStatus(ResourceStatus.DEFAULT);
        }

        return rdsConfigService.create(rdsConfig, cluster.getStack().getWorkspace(), cluster.getStack().getCreator());
    }

}
