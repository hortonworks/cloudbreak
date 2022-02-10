package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartDownscaleTriggerEvent extends StackEvent implements HostGroupPayload {

    private final String hostGroup;

    private final Set<Long> hostIds;

    private final ClusterManagerType clusterManagerType;

    public StopStartDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> hostIds,
            ClusterManagerType clusterManagerType) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.hostIds = hostIds;
        this.clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    @Override
    public String getHostGroupName() {
        return hostGroup;
    }

    public Set<Long> getHostIds() {
        return hostIds;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleTriggerEvent{" +
                "hostGroup='" + hostGroup + '\'' +
                ", hostIds=" + hostIds +
                ", clusterManagerType=" + clusterManagerType +
                '}';
    }
}
