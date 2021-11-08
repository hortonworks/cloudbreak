package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;

public class DecommissionResult extends AbstractClusterScaleResult<DecommissionRequest> {

    public static final String UNKNOWN_ERROR_PHASE = "";

    private final Set<String> hostNames;

    private final String errorPhase;

    public DecommissionResult(DecommissionRequest request, Set<String> hostNames) {
        super(request);
        this.hostNames = hostNames;
        this.errorPhase = "";
    }

    public DecommissionResult(String statusReason, Exception errorDetails, DecommissionRequest request) {
        super(statusReason, errorDetails, request);
        hostNames = Collections.emptySet();
        errorPhase = null;
    }

    public DecommissionResult(String statusReason, Exception errorDetails, DecommissionRequest request, Set<String> hostNames, String errorPhase) {
        super(EventStatus.FAILED, statusReason, errorDetails, request);
        this.hostNames = hostNames;
        this.errorPhase = errorPhase;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public String getErrorPhase() {
        return errorPhase;
    }

    @Override
    public String toString() {
        return "DecommissionResult{"
                + "hostNames=" + hostNames
                + '}';
    }
}
