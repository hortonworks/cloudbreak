package com.sequenceiq.freeipa.flow.freeipa.downscale.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class DownscaleFailureEvent extends StackEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    private final String failedPhase;

    private final Set<String> success;

    private final Map<String, String> failureDetails;

    public DownscaleFailureEvent(Long stackId, String failedPhase, Set<String> success, Map<String, String> failureDetails,
            Exception exception) {
        this(null, stackId, failedPhase, success, failureDetails, exception);
    }

    @JsonCreator
    public DownscaleFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("success") Set<String> success,
            @JsonProperty("failureDetails") Map<String, String> failureDetails,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId);
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
