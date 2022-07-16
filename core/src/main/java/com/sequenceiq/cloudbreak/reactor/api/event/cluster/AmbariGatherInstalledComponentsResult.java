package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariGatherInstalledComponentsResult
        extends AbstractClusterScaleResult<AmbariGatherInstalledComponentsRequest> implements FlowPayload {

    private final Map<String, String> foundInstalledComponents;

    public AmbariGatherInstalledComponentsResult(AmbariGatherInstalledComponentsRequest request, Map<String, String> foundInstalledComponents) {
        super(request);
        this.foundInstalledComponents = foundInstalledComponents;
    }

    @JsonCreator
    public AmbariGatherInstalledComponentsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") AmbariGatherInstalledComponentsRequest request) {
        super(statusReason, errorDetails, request);
        this.foundInstalledComponents = null;
    }

    public Map<String, String> getFoundInstalledComponents() {
        return foundInstalledComponents;
    }
}
