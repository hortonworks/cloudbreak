package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.SMART_SENSE_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@Component
public class SmartSenseConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSenseConfigProvider.class);
    private static final String SMART_SENSE_SERVER_CONFIG_FILE = "hst-server-conf";
    private static final String HST_SERVER_COMPONENT = "HST_SERVER";
    private static final String HST_AGENT_COMPONENT = "HST_AGENT";
    private static final String NAMENODE_COMPONENT = "NAMENODE";

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private HostGroupService hostGroupService;

    public boolean smartSenseIsConfigurable(String blueprint) {
        return configureSmartSense && blueprintProcessor.componentExistsInBlueprint(HST_SERVER_COMPONENT, blueprint);
    }

    public String addToBlueprint(Stack stack, String blueprintText) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        Credential credential = stack.getCredential();
        if (credential != null) {
            Map<String, Object> params = credential.getAttributes().getMap();
            String smartSenseId = String.valueOf(params.get(SMART_SENSE_ID));
            if (configureSmartSense && StringUtils.isNoneEmpty(smartSenseId)) {
                Set<HostGroup> hostGroups = hostGroupService.getByCluster(stack.getCluster().getId());
                Set<String> hostGroupNames = hostGroups.stream().map(getHostGroupNameMapper()).collect(Collectors.toSet());
                blueprintText = addSmartSenseServerToBp(blueprintText, hostGroups, hostGroupNames);
                blueprintText = blueprintProcessor.addComponentToHostgroups(HST_AGENT_COMPONENT, hostGroupNames, blueprintText);
                configs.addAll(getSmartSenseServerConfigs());
                configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.smartsense.id", smartSenseId));
                blueprintText = blueprintProcessor.addConfigEntries(blueprintText, configs, true);
            }
        }
        return blueprintText;
    }

    private String addSmartSenseServerToBp(String blueprintText, Set<HostGroup> hostGroups, Set<String> hostGroupNames) {
        if (!blueprintProcessor.componentExistsInBlueprint(HST_SERVER_COMPONENT, blueprintText)) {
            String aHostGroupName = hostGroupNames.stream().findFirst().get();
            String finalBlueprintText = blueprintText;
            Optional<String> hostGroupNameOfNameNode = hostGroupNames
                    .stream()
                    .filter(hostGroupName -> blueprintProcessor.getComponentsInHostGroup(finalBlueprintText, hostGroupName).contains(NAMENODE_COMPONENT))
                    .findFirst();
            if (hostGroupNameOfNameNode.isPresent()) {
                aHostGroupName = hostGroupNameOfNameNode.get();
            }
            LOGGER.info("Adding '{}' component to '{}' hosgroup in the Blueprint.", HST_SERVER_COMPONENT, aHostGroupName);
            blueprintText = blueprintProcessor.addComponentToHostgroups(HST_SERVER_COMPONENT, Collections.singletonList(aHostGroupName), blueprintText);
        }
        return blueprintText;
    }

    private Function<HostGroup, String> getHostGroupNameMapper() {
        return HostGroup::getName;
    }

    private Collection<? extends BlueprintConfigurationEntry> getSmartSenseServerConfigs() {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.account.name", "Hortonworks_Cloud_HDP"));
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.notification.email", "aws-marketplace@hortonworks.com"));
        return configs;
    }
}
