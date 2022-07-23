package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class CreateBindUserFailureEvent extends CreateBindUserEvent {

    private final String failureMessage;

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    @JsonCreator
    public CreateBindUserFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("accountId") String accountId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("suffix") String suffix,
            @JsonProperty("environmentCrn") String environmentCrn,
            @JsonProperty("failureMessage") String failureMessage,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, accountId, operationId, suffix, environmentCrn);
        this.failureMessage = failureMessage;
        this.exception = exception;
    }

    public CreateBindUserFailureEvent(String selector, CreateBindUserEvent event, String failureMessage, Exception exception) {
        super(selector, event.getResourceId(), event.getAccountId(), event.getOperationId(), event.getSuffix(), event.getEnvironmentCrn());
        this.failureMessage = failureMessage;
        this.exception = exception;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return super.toString() + ' ' +
                "CreateBindUserFailureEvent{" +
                "failureMessage='" + failureMessage + '\'' +
                ", exception=" + exception +
                '}';
    }
}
