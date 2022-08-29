package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartUpscaleTriggerEvent extends StackEvent {

    private final String hostGroup;

    private final Integer adjustment;

    private final ClusterManagerType clusterManagerType;

    private final boolean failureRecoveryEnabled;

    @JsonCreator
    public StopStartUpscaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroup") String hostGroup,
            @JsonProperty("adjustment") Integer adjustment,
            @JsonProperty("clusterManagerType") ClusterManagerType clusterManagerType,
            @JsonProperty("failureRecoveryEnabled") boolean failureRecoveryEnabled) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
        this.clusterManagerType = clusterManagerType;
        this.failureRecoveryEnabled = failureRecoveryEnabled;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    public boolean isFailureRecoveryEnabled() {
        return failureRecoveryEnabled;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleTriggerEvent{" +
                "hostGroup='" + hostGroup + '\'' +
                ", adjustment=" + adjustment +
                ", clusterManagerType=" + clusterManagerType +
                ", failureRecoveryEnabled=" + failureRecoveryEnabled +
                "} " + super.toString();
    }
}
