package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartDownscaleTriggerEvent extends StackEvent implements HostGroupPayload {

    private final String hostGroup;

    private final Set<Long> hostIds;

    private final boolean singlePrimaryGateway;

    private final ClusterManagerType clusterManagerType;

    public StopStartDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> hostIds, boolean singlePrimaryGateway,
            ClusterManagerType clusterManagerType) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.hostIds = hostIds;
        this.singlePrimaryGateway = singlePrimaryGateway;
        this.clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    @Override
    public String getHostGroupName() {
        return hostGroup;
    }

    public Set<Long> getHostIds() {
        return hostIds;
    }

    public boolean isSinglePrimaryGateway() {
        return singlePrimaryGateway;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleTriggerEvent{" +
                "hostGroup='" + hostGroup + '\'' +
                ", hostIds=" + hostIds +
                ", singlePrimaryGateway=" + singlePrimaryGateway +
                ", clusterManagerType=" + clusterManagerType +
                '}';
    }
}
