package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class RDSConfigProvider {

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

    private String parseDatabaseTypeFromJdbcUrl(String jdbcUrl) {
        return jdbcUrl.split(":")[1];
    }



}

