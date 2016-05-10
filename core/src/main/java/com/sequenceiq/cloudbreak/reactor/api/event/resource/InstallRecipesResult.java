package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallRecipesResult extends AbstractClusterUpscaleResult<InstallRecipesRequest> {

    public InstallRecipesResult(InstallRecipesRequest request) {
        super(request);
    }

    public InstallRecipesResult(String statusReason, Exception errorDetails, InstallRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
