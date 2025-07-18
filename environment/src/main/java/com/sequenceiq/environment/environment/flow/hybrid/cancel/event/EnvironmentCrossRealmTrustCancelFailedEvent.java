package com.sequenceiq.environment.environment.flow.hybrid.cancel.event;

import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FAILED_TRUST_CANCEL_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = EnvironmentCrossRealmTrustCancelFailedEvent.Builder.class)
public class EnvironmentCrossRealmTrustCancelFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentCrossRealmTrustCancelEvent crossRealmTrustCancelFreeIPAEvent;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvironmentCrossRealmTrustCancelFailedEvent(
            @JsonProperty("crossRealmTrustCancelFreeIPAEvent") EnvironmentCrossRealmTrustCancelEvent crossRealmTrustCancelFreeIPAEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

        super(
                FAILED_TRUST_CANCEL_EVENT.name(),
                crossRealmTrustCancelFreeIPAEvent.getResourceId(),
                crossRealmTrustCancelFreeIPAEvent.getResourceName(),
                crossRealmTrustCancelFreeIPAEvent.getResourceCrn(), exception);
        this.crossRealmTrustCancelFreeIPAEvent = crossRealmTrustCancelFreeIPAEvent;
        this.environmentStatus = environmentStatus;
    }

    public EnvironmentCrossRealmTrustCancelEvent getCrossRealmTrustCancelFreeIPAEvent() {
        return crossRealmTrustCancelFreeIPAEvent;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private EnvironmentCrossRealmTrustCancelEvent crossRealmTrustCancelFreeIPAEvent;

        private EnvironmentStatus environmentStatus;

        private Exception exception;

        private Builder() {
        }

        public Builder withCrossRealmTrustCancelFreeIPAEvent(EnvironmentCrossRealmTrustCancelEvent crossRealmTrustCancelFreeIPAEvent) {
            this.crossRealmTrustCancelFreeIPAEvent = crossRealmTrustCancelFreeIPAEvent;
            return this;
        }

        public Builder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public EnvironmentCrossRealmTrustCancelFailedEvent build() {
            return new EnvironmentCrossRealmTrustCancelFailedEvent(crossRealmTrustCancelFreeIPAEvent, exception, environmentStatus);
        }
    }
}
