package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Map;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class AmbariGatherInstalledComponentsResult extends AbstractClusterScaleResult<AmbariGatherInstalledComponentsRequest> {

    private Map<String, String> foundInstalledComponents;

    public AmbariGatherInstalledComponentsResult(AmbariGatherInstalledComponentsRequest request, Map<String, String> foundInstalledComponents) {
        super(request);
        this.foundInstalledComponents = foundInstalledComponents;
    }

    public AmbariGatherInstalledComponentsResult(String statusReason, Exception errorDetails, AmbariGatherInstalledComponentsRequest request) {
        super(statusReason, errorDetails, request);
    }

    public Map<String, String> getFoundInstalledComponents() {
        return foundInstalledComponents;
    }
}
