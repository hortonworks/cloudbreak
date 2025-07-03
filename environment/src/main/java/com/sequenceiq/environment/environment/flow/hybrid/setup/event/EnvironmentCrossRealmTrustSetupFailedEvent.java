package com.sequenceiq.environment.environment.flow.hybrid.setup.event;

import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FAILED_TRUST_SETUP_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = EnvironmentCrossRealmTrustSetupFailedEvent.Builder.class)
public class EnvironmentCrossRealmTrustSetupFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentCrossRealmTrustSetupEvent crossRealmTrustSetupFreeIPAEvent;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvironmentCrossRealmTrustSetupFailedEvent(
            @JsonProperty("crossRealmTrustSetupFreeIPAEvent") EnvironmentCrossRealmTrustSetupEvent crossRealmTrustSetupFreeIPAEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

        super(
                FAILED_TRUST_SETUP_EVENT.name(),
                crossRealmTrustSetupFreeIPAEvent.getResourceId(),
                crossRealmTrustSetupFreeIPAEvent.getResourceName(),
                crossRealmTrustSetupFreeIPAEvent.getResourceCrn(), exception);
        this.crossRealmTrustSetupFreeIPAEvent = crossRealmTrustSetupFreeIPAEvent;
        this.environmentStatus = environmentStatus;
    }

    public EnvironmentCrossRealmTrustSetupEvent getCrossRealmTrustSetupFreeIPAEvent() {
        return crossRealmTrustSetupFreeIPAEvent;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private EnvironmentCrossRealmTrustSetupEvent crossRealmTrustSetupFreeIPAEvent;

        private EnvironmentStatus environmentStatus;

        private Exception exception;

        private Builder() {
        }

        public Builder withCrossRealmTrustSetupFreeIPAEvent(EnvironmentCrossRealmTrustSetupEvent crossRealmTrustSetupFreeIPAEvent) {
            this.crossRealmTrustSetupFreeIPAEvent = crossRealmTrustSetupFreeIPAEvent;
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

        public EnvironmentCrossRealmTrustSetupFailedEvent build() {
            return new EnvironmentCrossRealmTrustSetupFailedEvent(crossRealmTrustSetupFreeIPAEvent, exception, environmentStatus);
        }
    }
}
