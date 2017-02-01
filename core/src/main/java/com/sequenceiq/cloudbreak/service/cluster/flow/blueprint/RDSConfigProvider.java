package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;


import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.common.type.RdsType;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class RDSConfigProvider {

    public List<BlueprintConfigurationEntry> getConfigs(RDSConfig rdsConfig) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        if (rdsConfig.getType().equals(RdsType.DRUID)) {
            bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.type", "postgresql"));
            bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.connectURI", rdsConfig.getConnectionURL()));
            bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.user", rdsConfig.getConnectionUserName()));
            bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.password", rdsConfig.getConnectionPassword()));
            return bpConfigs;
        }
        bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionURL", rdsConfig.getConnectionURL()));
        bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionDriverName", rdsConfig.getDatabaseType().getDbDriver()));
        bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionUserName", rdsConfig.getConnectionUserName()));
        bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionPassword", rdsConfig.getConnectionPassword()));
        bpConfigs.add(new BlueprintConfigurationEntry("hive-env", "hive_database", rdsConfig.getDatabaseType().getAmbariDbOption()));
        bpConfigs.add(new BlueprintConfigurationEntry("hive-env", "hive_database_type", rdsConfig.getDatabaseType().getDbName()));
        return bpConfigs;
    }

}

