package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair.TargetGroupPortPairDeserializer;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair.TargetGroupPortPairSerializer;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class CloudLoadBalancer {

    private final LoadBalancerType type;

    @JsonSerialize(keyUsing = TargetGroupPortPairSerializer.class)
    @JsonDeserialize(keyUsing = TargetGroupPortPairDeserializer.class)
    private final Map<TargetGroupPortPair, Set<Group>> portToTargetGroupMapping;

    private final LoadBalancerSku sku;

    private final Boolean stickySession;

    public CloudLoadBalancer(LoadBalancerType type) {
        this(type, LoadBalancerSku.getDefault(), false);
    }

    @JsonCreator
    public CloudLoadBalancer(
            @JsonProperty("type") LoadBalancerType type,
            @JsonProperty("sku") LoadBalancerSku sku,
            @JsonProperty("stickySession") boolean stickySession) {
        this.type = type;
        this.sku = sku;
        portToTargetGroupMapping = new HashMap<>();
        this.stickySession = stickySession;
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

    public LoadBalancerSku getSku() {
        return sku;
    }

    public Boolean isStickySession() {
        return stickySession;
    }

    @Override
    public String toString() {
        return "CloudLoadBalancer{" +
                "type=" + type +
                ", portToTargetGroupMapping=" + portToTargetGroupMapping +
                ", stickySession=" + stickySession +
                ", sku=" + sku +
                '}';
    }

}
