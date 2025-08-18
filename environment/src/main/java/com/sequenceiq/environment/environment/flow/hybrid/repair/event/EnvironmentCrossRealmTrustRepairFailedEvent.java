package com.sequenceiq.environment.environment.flow.hybrid.repair.event;

import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.FAILED_TRUST_REPAIR_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = EnvironmentCrossRealmTrustRepairFailedEvent.Builder.class)
public class EnvironmentCrossRealmTrustRepairFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentCrossRealmTrustRepairEvent crossRealmTrustRepairFreeIPAEvent;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvironmentCrossRealmTrustRepairFailedEvent(
            @JsonProperty("crossRealmTrustRepairFreeIPAEvent") EnvironmentCrossRealmTrustRepairEvent crossRealmTrustRepairFreeIPAEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

        super(
                FAILED_TRUST_REPAIR_EVENT.name(),
                crossRealmTrustRepairFreeIPAEvent.getResourceId(),
                crossRealmTrustRepairFreeIPAEvent.getResourceName(),
                crossRealmTrustRepairFreeIPAEvent.getResourceCrn(), exception);
        this.crossRealmTrustRepairFreeIPAEvent = crossRealmTrustRepairFreeIPAEvent;
        this.environmentStatus = environmentStatus;
    }

    public EnvironmentCrossRealmTrustRepairEvent getCrossRealmTrustRepairFreeIPAEvent() {
        return crossRealmTrustRepairFreeIPAEvent;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private EnvironmentCrossRealmTrustRepairEvent crossRealmTrustRepairFreeIPAEvent;

        private EnvironmentStatus environmentStatus;

        private Exception exception;

        private Builder() {
        }

        public Builder withCrossRealmTrustRepairFreeIPAEvent(EnvironmentCrossRealmTrustRepairEvent crossRealmTrustRepairFreeIPAEvent) {
            this.crossRealmTrustRepairFreeIPAEvent = crossRealmTrustRepairFreeIPAEvent;
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

        public EnvironmentCrossRealmTrustRepairFailedEvent build() {
            return new EnvironmentCrossRealmTrustRepairFailedEvent(crossRealmTrustRepairFreeIPAEvent, exception, environmentStatus);
        }
    }
}
