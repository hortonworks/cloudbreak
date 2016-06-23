package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint


import java.util.ArrayList

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.RDSConfig

@Component
class RDSConfigProvider {

    fun getConfigs(rdsConfig: RDSConfig): List<BlueprintConfigurationEntry> {
        val bpConfigs = ArrayList<BlueprintConfigurationEntry>()
        bpConfigs.add(BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionURL", rdsConfig.connectionURL))
        bpConfigs.add(BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionDriverName", rdsConfig.databaseType.dbDriver))
        bpConfigs.add(BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionUserName", rdsConfig.connectionUserName))
        bpConfigs.add(BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionPassword", rdsConfig.connectionPassword))
        bpConfigs.add(BlueprintConfigurationEntry("hive-env", "hive_database", rdsConfig.databaseType.ambariDbOption))
        bpConfigs.add(BlueprintConfigurationEntry("hive-env", "hive_database_type", rdsConfig.databaseType.dbName))
        return bpConfigs
    }

}

