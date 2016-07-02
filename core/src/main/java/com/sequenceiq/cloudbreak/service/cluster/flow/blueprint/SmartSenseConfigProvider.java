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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@Component
public class SmartSenseConfigProvider {
    private static final String SMART_SENSE_SERVER_CONFIG_FILE = "hst-server-conf";
    private static final String HST_SERVER_COMPONENT = "HST_SERVER";
    private static final String HST_AGENT_COMPONENT = "HST_AGENT";

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
        Map<String, Object> params = credential.getAttributes().getMap();
        String smartSenseId = String.valueOf(params.get(SMART_SENSE_ID));
        if (configureSmartSense && StringUtils.isNoneEmpty(smartSenseId)) {
            Set<HostGroup> hostGroups = hostGroupService.getByCluster(stack.getCluster().getId());
            Set<String> hostGroupNames = hostGroups.stream().map(getHostGroupNameMapper()).collect(Collectors.toSet());
            blueprintText = addSmartSenseServerToBp(blueprintText, hostGroups, hostGroupNames);
            blueprintText = blueprintProcessor.addComponentToHostgroups(HST_AGENT_COMPONENT, hostGroupNames, blueprintText);
            configs.addAll(getSmartSenseServerConfigs());
            configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.smartsense.id", smartSenseId));
            configs.addAll(getSmartSenseGatewayConfigs(stack));
            blueprintText = blueprintProcessor.addConfigEntries(blueprintText, configs, true);
        }
        return blueprintText;
    }

    private String addSmartSenseServerToBp(String blueprintText, Set<HostGroup> hostGroups, Set<String> hostGroupNames) {
        if (!blueprintProcessor.componentExistsInBlueprint(HST_SERVER_COMPONENT, blueprintText)) {
            String aHostGroupName = hostGroupNames.stream().findFirst().get();
            Optional<String> hostGroupWithOneNode = hostGroups.stream()
                    .filter(hostGroup -> hostGroup.getHostMetadata().size() == 1)
                    .map(getHostGroupNameMapper())
                    .findFirst();
            if (hostGroupWithOneNode.isPresent()) {
                aHostGroupName = hostGroupWithOneNode.get();
            }
            blueprintText = blueprintProcessor.addComponentToHostgroups(HST_SERVER_COMPONENT, Collections.singletonList(aHostGroupName), blueprintText);
        }
        return blueprintText;
    }

    private Function<HostGroup, String> getHostGroupNameMapper() {
        return HostGroup::getName;
    }

    private Collection<? extends BlueprintConfigurationEntry> getSmartSenseServerConfigs() {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.account.name", "Hortonworks Data Platform AWS Marketplace"));
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.notification.email", "aws-marketplace@hortonworks.com"));
        return configs;
    }

    private Collection<? extends BlueprintConfigurationEntry> getSmartSenseGatewayConfigs(Stack stack) {
        List<BlueprintConfigurationEntry> configs = new ArrayList<>();
        String privateIp = stack.getGatewayInstanceGroup().getInstanceMetaData().stream().findFirst().get().getPrivateIp();
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "gateway.host", privateIp));
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "gateway.enabled", "true"));
        return configs;
    }
}
