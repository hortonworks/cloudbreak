package com.sequenceiq.freeipa.flow.freeipa.verticalscale.event;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaVerticalScaleFailureEvent extends StackEvent {

    private final Exception exception;

    private final String failedPhase;

    private final Set<String> success;

    private final FailureType failureType;

    private final Map<String, String> failureDetails;

    @JsonCreator
    public FreeIpaVerticalScaleFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedPhase") String failedPhase,
            @JsonProperty("success") Set<String> success,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("failureDetails") Map<String, String> failureDetails,
            @JsonProperty("exception") Exception exception) {
        super(stackId);
        this.exception = exception;
        this.failedPhase = failedPhase;
        this.success = success;
        this.failureDetails = failureDetails;
        this.failureType = failureType;
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

    public FailureType getFailureType() {
        return failureType == null ? ERROR : failureType;
    }

    @Override
    public String toString() {
        return super.toString() + "FreeIpaVerticalScaleFailureEvent{" +
                "exception=" + exception +
                ", failedPhase='" + failedPhase + '\'' +
                ", success=" + success +
                ", failureDetails=" + failureDetails +
                ", failureType=" + failureType +
                '}';
    }
}
