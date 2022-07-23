package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ChangePrimaryGatewayFailureEvent extends StackEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    private final String failedPhase;

    private final Set<String> success;

    private final Map<String, String> failureDetails;

    @JsonCreator
    public ChangePrimaryGatewayFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("success") Set<String> success,
            @JsonProperty("failureDetails") Map<String, String> failureDetails,
            @JsonProperty("exception") Exception exception) {
        super(null, stackId);
        this.exception = exception;
        this.failedPhase = failedPhase;
        this.success = success;
        this.failureDetails = failureDetails;
    }

    public Exception getException() {
        return exception;
    }

    public String getFailedPhase() {
        return failedPhase;
    }

    public Set<String> getSuccess() {
        return success;
    }

    public Map<String, String> getFailureDetails() {
        return failureDetails;
    }

}
