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
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class RDSConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigProvider.class);

    @Inject
    private RdsConfigService rdsConfigService;

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

    public void createHivePostgresRdsConfig(Stack stack, Cluster cluster, Set<RDSConfig> rdsConfigs, String dbUserName, String dbPort, String dbName) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(stack.getName() + stack.getId());
        rdsConfig.setConnectionUserName(dbUserName);
        rdsConfig.setConnectionPassword(PasswordUtil.generatePassword());
        String primaryGatewayIp = stack.getPrimaryGatewayInstance().getPrivateIp();
        rdsConfig.setConnectionURL(
                "jdbc:postgresql://" + primaryGatewayIp + ":" + dbPort + "/" + dbName
        );
        rdsConfig.setDatabaseType(RDSDatabase.POSTGRES);
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setOwner(stack.getOwner());
        rdsConfig.setAccount(stack.getAccount());
        rdsConfig.setClusters(Collections.singleton(cluster));
        rdsConfig = rdsConfigService.create(rdsConfig);

        if (rdsConfigs == null) {
            rdsConfigs = new HashSet<>();
        }
        rdsConfigs.add(rdsConfig);
        cluster.setRdsConfigs(rdsConfigs);
        clusterRepository.save(cluster);
    }

    private String parseDatabaseTypeFromJdbcUrl(String jdbcUrl) {
        return jdbcUrl.split(":")[1];
    }



}

