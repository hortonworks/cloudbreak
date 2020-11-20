package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.common.api.type.LoadBalancerType;

public class CloudLoadBalancer {

    private final LoadBalancerType type;

    private final Map<Integer, Set<Group>> portToTargetGroupMapping;

    public CloudLoadBalancer(LoadBalancerType type) {
        this.type = type;
        portToTargetGroupMapping = new HashMap<>();
    }

    public void addPortToTargetGroupMapping(Integer port, Set<Group> targetGroups) {
        if (portToTargetGroupMapping.containsKey(port)) {
            portToTargetGroupMapping.get(port).addAll(targetGroups);
        } else {
            portToTargetGroupMapping.put(port, targetGroups);
        }
    }

    public Map<Integer, Set<Group>> getPortToTargetGroupMapping() {
        return portToTargetGroupMapping;
    }

    public LoadBalancerType getType() {
        return type;
    }
}
