package com.sequenceiq.cloudbreak.blueprint.druid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.RdsType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@Component
public class DruidSupersetConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DruidSupersetConfigProvider.class);

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public List<BlueprintConfigurationEntry> getConfigurationEntries(BlueprintPreparationObject source, String blueprintText) {
        LOGGER.info("Druid Superset exists in Blueprint");
        String configFile = "druid-superset-env";
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_password", source.getStack().getCluster().getPassword()));
        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_firstname", source.getStack().getCluster().getUserName()));
        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_lastname", source.getStack().getCluster().getUserName()));
        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_email", source.getIdentityUser().getUsername()));
        return configs;
    }

    @Override
    public List<BlueprintConfigurationEntry> rdsConfigs(BlueprintPreparationObject source, String blueprintText) {
        Optional<RDSConfig> druidRds = source.getRdsConfigs().stream().filter(r -> r.getType().equals(RdsType.DRUID)).findFirst();
        String configFile = "druid-common";
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(configFile, "druid.metadata.storage.type", parseDatabaseTypeFromJdbcUrl(druidRds.get().getConnectionURL())));
        configs.add(new BlueprintConfigurationEntry(configFile, "druid.metadata.storage.connector.connectURI", druidRds.get().getConnectionURL()));
        configs.add(new BlueprintConfigurationEntry(configFile, "druid.metadata.storage.connector.user", druidRds.get().getConnectionUserName()));
        configs.add(new BlueprintConfigurationEntry(configFile, "druid.metadata.storage.connector.password", druidRds.get().getConnectionPassword()));
        return configs;
    }

    @Override
    public boolean rdsConfigShouldApply(BlueprintPreparationObject source, String blueprintText) {
        Optional<RDSConfig> druidRds = source.getRdsConfigs().stream().filter(r -> r.getType().equals(RdsType.DRUID)).findFirst();
        return druidRds.isPresent();
    }

    @Override
    public Set<String> components() {
        return Sets.newHashSet("DRUID_SUPERSET");
    }

    private String parseDatabaseTypeFromJdbcUrl(String jdbcUrl) {
        return jdbcUrl.split(":")[1];
    }

}
