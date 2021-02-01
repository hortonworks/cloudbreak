package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.common.api.type.LoadBalancerType;

public class CloudLoadBalancer {

    private final LoadBalancerType type;

    private final Map<TargetGroupPortPair, Set<Group>> portToTargetGroupMapping;

    public CloudLoadBalancer(LoadBalancerType type) {
        this.type = type;
        portToTargetGroupMapping = new HashMap<>();
    }

    public void addPortToTargetGroupMapping(TargetGroupPortPair portPair, Set<Group> targetGroups) {
        if (portToTargetGroupMapping.containsKey(portPair)) {
            portToTargetGroupMapping.get(portPair).addAll(targetGroups);
        } else {
            portToTargetGroupMapping.put(portPair, targetGroups);
        }
    }

    public Map<TargetGroupPortPair, Set<Group>> getPortToTargetGroupMapping() {
        return portToTargetGroupMapping;
    }

    public LoadBalancerType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "CloudLoadBalancer{" +
            "type=" + type +
            ", portToTargetGroupMapping=" + portToTargetGroupMapping +
            '}';
    }
}
