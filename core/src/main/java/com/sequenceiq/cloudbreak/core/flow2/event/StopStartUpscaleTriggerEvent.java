package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartUpscaleTriggerEvent extends StackEvent implements HostGroupPayload {

    private final String hostGroup;

    private final Integer adjustment;

    private final ClusterManagerType clusterManagerType;

    public StopStartUpscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, ClusterManagerType clusterManagerType) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
        this.clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    @Override
    public String getHostGroupName() {
        return hostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleTriggerEvent{" +
                "hostGroup='" + hostGroup + '\'' +
                ", adjustment=" + adjustment +
                ", clusterManagerType=" + clusterManagerType +
                '}';
    }
}
