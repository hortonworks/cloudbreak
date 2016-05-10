package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallFsRecipesResult extends AbstractClusterUpscaleResult<InstallFsRecipesRequest> {

    public InstallFsRecipesResult(InstallFsRecipesRequest request) {
        super(request);
    }

    public InstallFsRecipesResult(String statusReason, Exception errorDetails, InstallFsRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
