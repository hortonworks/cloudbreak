package com.sequenceiq.cloudbreak.blueprint.rds;


import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class RDSConfigProvider implements BlueprintComponentConfigProvider {

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        for (RDSConfig rds : source.getRdsConfigs()) {
            switch (rds.getType()) {
                case HIVE:
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionURL",
                            rds.getConnectionURL()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionDriverName",
                            rds.getDatabaseType().getDbDriver()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionUserName",
                            rds.getConnectionUserName()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-site", "javax.jdo.option.ConnectionPassword",
                            rds.getConnectionPassword()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-env", "hive_database",
                            rds.getDatabaseType().getAmbariDbOption()));
                    bpConfigs.add(new BlueprintConfigurationEntry("hive-env", "hive_database_type",
                            rds.getDatabaseType().getDbName()));
                    break;
                case RANGER:
                    break;
                case DRUID:
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.type",
                            parseDatabaseTypeFromJdbcUrl(rds.getConnectionURL())));
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.connectURI",
                            rds.getConnectionURL()));
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.user",
                            rds.getConnectionUserName()));
                    bpConfigs.add(new BlueprintConfigurationEntry("druid-common", "druid.metadata.storage.connector.password",
                            rds.getConnectionPassword()));
                    break;
                default:
                    break;
            }
        }
        return blueprintProcessor.addConfigEntries(blueprintText, bpConfigs, true);
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return true;
    }

    private String parseDatabaseTypeFromJdbcUrl(String jdbcUrl) {
        return jdbcUrl.split(":")[1];
    }

}

