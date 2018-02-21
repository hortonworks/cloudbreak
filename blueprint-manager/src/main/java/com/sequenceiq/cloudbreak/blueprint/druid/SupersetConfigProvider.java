package com.sequenceiq.cloudbreak.blueprint.druid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;

@Component
public class SupersetConfigProvider implements BlueprintComponentConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupersetConfigProvider.class);

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) {
        LOGGER.info("Superset exists in Blueprint");
        String configFile = "superset-env";
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();

        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_password", source.getStack().getCluster().getPassword()));
        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_firstname", source.getStack().getCluster().getUserName()));
        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_lastname", source.getStack().getCluster().getUserName()));
        configs.add(new BlueprintConfigurationEntry(configFile, "superset_admin_email", source.getIdentityUser().getUsername()));

        return blueprintProcessor.addConfigEntries(blueprintText, configs, false);
    }

    @Override
    public Set<String> components() {
        return Sets.newHashSet("SUPERSET");
    }
}
