package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartClusterServicesRequest extends StackEvent {
    private boolean withMgmtServices;

    private String failureSelector;

    public RestartClusterServicesRequest(Long stackId, boolean withMgmtServices, String failureSelector) {
        super(stackId);
        this.withMgmtServices = withMgmtServices;
        this.failureSelector = failureSelector;
    }

    public boolean isWithMgmtServices() {
        return withMgmtServices;
    }

    public String getFailureSelector() {
        return failureSelector;
    }
}
