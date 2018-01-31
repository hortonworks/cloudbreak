package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RDSDatabase;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class RDSConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigProvider.class);

    @Inject
    private HiveConfigProvider hiveConfigProvider;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    public List<BlueprintConfigurationEntry> getConfigs(Set<RDSConfig> rdsConfigs) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        for (RDSConfig rds : rdsConfigs) {
            switch (rds.getType()) {
                case HIVE:
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionURL", rds.getConnectionURL()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionDriverName", rds.getDatabaseType().getDbDriver()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionUserName", rds.getConnectionUserName()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionPassword", rds.getConnectionPassword()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-env", "hive_database", rds.getDatabaseType().getAmbariDbOption()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-env", "hive_database_type", rds.getDatabaseType().getDbName()));
                    break;
                case RANGER:
                    break;
                case DRUID:
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.type",
                            parseDatabaseTypeFromJdbcUrl(rds.getConnectionURL())));
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.connectURI", rds.getConnectionURL()));
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.user", rds.getConnectionUserName()));
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.password", rds.getConnectionPassword()));
                    break;
                default:
                    break;
            }
        }
        return bpConfigs;
    }

    public Set<RDSConfig> createPostgresRdsConfigIfNeeded(Stack stack, Cluster cluster, Blueprint blueprint) {
        Set<RDSConfig> rdsConfigs = rdsConfigRepository.findByClusterId(stack.getOwner(), stack.getAccount(), cluster.getId());
        if (hiveConfigProvider.isRdsConfigNeedForHiveMetastore(blueprint)) {
            LOGGER.info("Creating postgres RDSConfig");
            RDSConfig rdsConfig = new RDSConfig();
            rdsConfig.setName(stack.getName() + stack.getId());
            rdsConfig.setConnectionUserName(hiveConfigProvider.getHiveDbUser());
            rdsConfig.setConnectionPassword(hiveConfigProvider.getHiveDbPassword());
            rdsConfig.setConnectionURL(
                    "jdbc:postgresql://" + hiveConfigProvider.getHiveDbHost() + ":" + hiveConfigProvider.getHiveDbPort() + "/" + hiveConfigProvider.getHiveDb()
            );
            rdsConfig.setDatabaseType(RDSDatabase.POSTGRES);
            rdsConfig.setStatus(ResourceStatus.DEFAULT);
            rdsConfig.setOwner(stack.getOwner());
            rdsConfig.setAccount(stack.getAccount());
            rdsConfig.setClusters(Collections.singleton(cluster));
            rdsConfig = rdsConfigService.create(rdsConfig);

            if (rdsConfigs == null) {
                rdsConfigs = new HashSet<>();
                cluster.setRdsConfigs(rdsConfigs);
            }
            rdsConfigs.add(rdsConfig);
            cluster.setRdsConfigs(rdsConfigs);
            clusterRepository.save(cluster);
        }
        return rdsConfigs;
    }

    private String parseDatabaseTypeFromJdbcUrl(String jdbcUrl) {
        return jdbcUrl.split(":")[1];
    }



}

