package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterScaleEvent extends StackEvent implements HostGroupPayload {
    private final String hostGroupName;
    private final Integer adjustment;

    public StartClusterScaleEvent(Long stackId, String hostGroupName, Integer adjustment) {
        this(null, stackId, hostGroupName, adjustment);
    }

    public StartClusterScaleEvent(String selector, Long stackId, String hostGroupName, Integer adjustment) {
        super(selector, stackId);
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }
}
