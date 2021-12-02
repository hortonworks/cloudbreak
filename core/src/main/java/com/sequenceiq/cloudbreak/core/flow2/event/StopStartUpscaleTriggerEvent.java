package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartUpscaleTriggerEvent extends StackEvent implements HostGroupPayload {

    private final String hostGroup;

    private final Integer adjustment;

    private final Set<Long> hostIds;

    private final boolean singlePrimaryGateway;

    private final boolean restartServices;

    private final ClusterManagerType clusterManagerType;

    public StopStartUpscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Set<Long> hostIds, boolean singlePrimaryGateway,
            boolean restartServices, ClusterManagerType clusterManagerType) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
        this.hostIds = hostIds;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.restartServices = restartServices;
        this.clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    @Override
    public String getHostGroupName() {
        return hostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public Set<Long> getHostIds() {
        return hostIds;
    }

    public boolean isSinglePrimaryGateway() {
        return singlePrimaryGateway;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleTriggerEvent{" +
                "hostGroup='" + hostGroup + '\'' +
                ", adjustment=" + adjustment +
                ", hostIds=" + hostIds +
                ", singlePrimaryGateway=" + singlePrimaryGateway +
                ", restartServices=" + restartServices +
                ", clusterManagerType=" + clusterManagerType +
                '}';
    }
}
