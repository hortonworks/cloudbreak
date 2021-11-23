package com.sequenceiq.flow.domain;

public class RetryableStateResponse {

    private RetryableState state;

    private String lastKnownStateMessage;

    private String name;

    private FlowLog lastSuccessfulStateFlowLog;

    public RetryableState getState() {
        return state;
    }

    public String getLastKnownStateMessage() {
        return lastKnownStateMessage;
    }

    public String getName() {
        return name;
    }

    public FlowLog getLastSuccessfulStateFlowLog() {
        return lastSuccessfulStateFlowLog;
    }

    public static RetryableStateResponse flowPending() {
        RetryableStateResponse response = new RetryableStateResponse();
        response.state = RetryableState.FLOW_PENDING;
        return response;
    }

    public static RetryableStateResponse lastFlowNotFailedOrNotRetryable(String lastKnownStateMessage) {
        RetryableStateResponse response = new RetryableStateResponse();
        response.state = RetryableState.LAST_NOT_FAILED_OR_NOT_RETRYABLE;
        response.lastKnownStateMessage = lastKnownStateMessage;
        return response;
    }

    public static RetryableStateResponse retryable(String name, FlowLog lastSuccessfulStateFlowLog) {
        RetryableStateResponse response = new RetryableStateResponse();
        response.state = RetryableState.RETRYABLE;
        response.name = name;
        response.lastSuccessfulStateFlowLog = lastSuccessfulStateFlowLog;
        return response;
    }

    public static RetryableStateResponse noSuccessfulState() {
        RetryableStateResponse response = new RetryableStateResponse();
        response.state = RetryableState.NO_SUCCESSFUL_STATE;
        return response;
    }
}
