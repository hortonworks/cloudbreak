package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartClusterServicesRequest extends StackEvent {
    private final boolean withMgmtServices;

    private final String failureSelector;

    @JsonCreator
    public RestartClusterServicesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("withMgmtServices") boolean withMgmtServices,
            @JsonProperty("failureSelector") String failureSelector) {
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
