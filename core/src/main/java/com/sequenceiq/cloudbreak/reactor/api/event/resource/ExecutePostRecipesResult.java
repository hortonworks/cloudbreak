package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExecutePostRecipesResult extends AbstractClusterScaleResult<ExecutePostRecipesRequest> {

    public ExecutePostRecipesResult(ExecutePostRecipesRequest request) {
        super(request);
    }

    public ExecutePostRecipesResult(String statusReason, Exception errorDetails, ExecutePostRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
