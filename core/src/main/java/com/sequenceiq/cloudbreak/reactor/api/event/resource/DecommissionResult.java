package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class DecommissionResult extends AbstractClusterScaleResult<DecommissionRequest> implements FlowPayload {

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

    @JsonCreator
    public DecommissionResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") DecommissionRequest request,
            @JsonProperty("hostNames") Set<String> hostNames,
            @JsonProperty("errorPhase") String errorPhase) {
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
