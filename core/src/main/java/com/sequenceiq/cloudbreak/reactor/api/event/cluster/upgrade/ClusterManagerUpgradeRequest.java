package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerUpgradeRequest extends StackEvent {
    private boolean runtimeServicesStartNeeded;

    public ClusterManagerUpgradeRequest(Long stackId, boolean runtimeServicesStartNeeded) {
        super(stackId);
        this.runtimeServicesStartNeeded = runtimeServicesStartNeeded;
    }

    public boolean isRuntimeServicesStartNeeded() {
        return runtimeServicesStartNeeded;
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }
}
