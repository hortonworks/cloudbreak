package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExecutePreRecipesResult extends AbstractClusterScaleResult<ExecutePreRecipesRequest> {

    public ExecutePreRecipesResult(ExecutePreRecipesRequest request) {
        super(request);
    }

    public ExecutePreRecipesResult(String statusReason, Exception errorDetails, ExecutePreRecipesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
