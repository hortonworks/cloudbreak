package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Component
public class DruidSupersetConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DruidSupersetConfigProvider.class);

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private UserDetailsService userDetailsService;

    public String addToBlueprint(Stack stack, String blueprintText) {
        if (blueprintProcessor.componentExistsInBlueprint("DRUID_SUPERSET", blueprintText)) {
            LOGGER.info("Druid Superset exists in Blueprint");
            List<BlueprintConfigurationEntry> configs = getConfigs(stack.getCluster());
            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, configs, false);
        }
        return blueprintText;
    }

    private List<BlueprintConfigurationEntry> getConfigs(Cluster cluster) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        String cbUser;
        cbUser = userDetailsService.getDetails(cluster.getOwner(), UserFilterField.USERID)
                .getUsername();

        configs.add(new BlueprintConfigurationEntry("druid-superset-env", "superset_admin_password",
                cluster.getPassword()
        ));
        configs.add(new BlueprintConfigurationEntry("druid-superset-env", "superset_admin_firstname",
                cluster.getUserName()
        ));
        configs.add(new BlueprintConfigurationEntry("druid-superset-env", "superset_admin_lastname",
                cluster.getUserName()
        ));
        configs.add(new BlueprintConfigurationEntry("druid-superset-env", "superset_admin_email",
                cbUser
        ));

        return configs;
    }
}
