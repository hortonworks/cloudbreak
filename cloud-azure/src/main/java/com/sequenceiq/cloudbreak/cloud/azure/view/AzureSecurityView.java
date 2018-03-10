package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;

public class AzureSecurityView {

    private final Map<String, List<AzurePortView>> ports = new HashMap<>();

    private final Map<String, String> securityGroupIds = new HashMap<>();

    public AzureSecurityView(Iterable<Group> groups) {
        for (Group group : groups) {
            List<AzurePortView> groupPorts = new ArrayList<>(group.getSecurity().getRules().size());
            for (SecurityRule securityRule : group.getSecurity().getRules()) {
                for (PortDefinition port : securityRule.getPorts()) {
                    if (port.isRange()) {
                        String portRange = String.format("%s-%s", port.getFrom(), port.getTo());
                        groupPorts.add(new AzurePortView(securityRule.getCidr(), portRange, securityRule.getProtocol()));
                    } else {
                        groupPorts.add(new AzurePortView(securityRule.getCidr(), port.getFrom(), securityRule.getProtocol()));
                    }
                }
            }
            ports.put(group.getName(), groupPorts);
            if (StringUtils.isNotBlank(group.getSecurity().getCloudSecurityId())) {
                securityGroupIds.put(group.getName(), group.getSecurity().getCloudSecurityId());
            }
        }
    }

    public Map<String, List<AzurePortView>> getPorts() {
        return ports;
    }

    public Map<String, String> getSecurityGroupIds() {
        return securityGroupIds;
    }
}