package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;

public class AzureSecurityView {

    private final Map<String, List<AzurePortView>> ports = new HashMap<>();

    public AzureSecurityView(List<Group> groups) {
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
        }
    }

    public Map<String, List<AzurePortView>> getPorts() {
        return ports;
    }

}