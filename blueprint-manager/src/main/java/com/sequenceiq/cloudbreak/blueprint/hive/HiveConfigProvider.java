package com.sequenceiq.cloudbreak.blueprint.hive;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.RdsType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

public class HiveConfigProvider implements BlueprintComponentConfigProvider {


    @Override
    public List<BlueprintConfigurationEntry> rdsConfigs(BlueprintPreparationObject source, String blueprintText) {
        Optional<RDSConfig> rds = source.getRdsConfigs().stream().filter(r -> r.getType().equals(RdsType.HIVE)).findFirst();
        String configFile = "druid-common";
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(configFile, "javax.jdo.option.ConnectionURL", rds.get().getConnectionURL()));
        configs.add(new BlueprintConfigurationEntry(configFile, "javax.jdo.option.ConnectionDriverName", rds.get().getDatabaseType().getDbDriver()));
        configs.add(new BlueprintConfigurationEntry(configFile, "javax.jdo.option.ConnectionUserName", rds.get().getConnectionUserName()));
        configs.add(new BlueprintConfigurationEntry(configFile, "javax.jdo.option.ConnectionPassword", rds.get().getConnectionPassword()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hive_database", rds.get().getDatabaseType().getAmbariDbOption()));
        configs.add(new BlueprintConfigurationEntry(configFile, "hive_database_type", rds.get().getDatabaseType().getDbName()));
        return configs;
    }

    @Override
    public boolean rdsConfigShouldApply(BlueprintPreparationObject source, String blueprintText) {
        Optional<RDSConfig> druidRds = source.getRdsConfigs().stream().filter(r -> r.getType().equals(RdsType.HIVE)).findFirst();
        return druidRds.isPresent();
    }

}
