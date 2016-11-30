package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;

public class ClusterTerminationResult extends ClusterPlatformResult<ClusterTerminationRequest> {

    private final Boolean operationAllowed;

    public ClusterTerminationResult(ClusterTerminationRequest request, Boolean operationAllowed) {
        super(request);
        this.operationAllowed = operationAllowed;
    }

    public ClusterTerminationResult(String statusReason, Exception errorDetails, ClusterTerminationRequest request) {
        super(statusReason, errorDetails, request);
        this.operationAllowed = Boolean.TRUE;
    }

    public Boolean isOperationAllowed() {
        return operationAllowed;
    }
}
