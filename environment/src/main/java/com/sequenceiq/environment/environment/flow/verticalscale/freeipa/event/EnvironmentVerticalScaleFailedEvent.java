package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event;

import static com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event.EnvironmentVerticalScaleStateSelectors.FAILED_VERTICAL_SCALING_FREEIPA_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = EnvironmentVerticalScaleFailedEvent.Builder.class)
public class EnvironmentVerticalScaleFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentVerticalScaleEvent verticalScaleFreeIPAEvent;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvironmentVerticalScaleFailedEvent(
            @JsonProperty("verticalScaleFreeIPAEvent") EnvironmentVerticalScaleEvent verticalScaleFreeIPAEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

        super(FAILED_VERTICAL_SCALING_FREEIPA_EVENT.name(), verticalScaleFreeIPAEvent.getResourceId(),
                verticalScaleFreeIPAEvent.getResourceName(), verticalScaleFreeIPAEvent.getResourceCrn(), exception);
        this.verticalScaleFreeIPAEvent = verticalScaleFreeIPAEvent;
        this.environmentStatus = environmentStatus;
    }

    public EnvironmentVerticalScaleEvent getVerticalScaleFreeIPAEvent() {
        return verticalScaleFreeIPAEvent;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private EnvironmentVerticalScaleEvent verticalScaleFreeIPAEvent;

        private EnvironmentStatus environmentStatus;

        private Exception exception;

        private Builder() {
        }

        public Builder withVerticalScaleFreeIPAEvent(EnvironmentVerticalScaleEvent verticalScaleFreeIPAEvent) {
            this.verticalScaleFreeIPAEvent = verticalScaleFreeIPAEvent;
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

        public EnvironmentVerticalScaleFailedEvent build() {
            return new EnvironmentVerticalScaleFailedEvent(verticalScaleFreeIPAEvent, exception, environmentStatus);
        }
    }
}
