package com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = EnvironmentEnableSeLinuxFailedEvent.Builder.class)
public class EnvironmentEnableSeLinuxFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvironmentEnableSeLinuxFailedEvent(
            @JsonProperty("enableSeLinuxEvent") EnvironmentEnableSeLinuxEvent enableSeLinuxEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

        super(EnvironmentEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_FREEIPA_EVENT.name(), enableSeLinuxEvent.getResourceId(),
                enableSeLinuxEvent.getResourceName(), enableSeLinuxEvent.getResourceCrn(), exception);
        this.environmentStatus = environmentStatus;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private EnvironmentEnableSeLinuxEvent enableSeLinuxEvent;

        private EnvironmentStatus environmentStatus;

        private Exception exception;

        private Builder() {
        }

        public Builder withEnableSeLinuxEvent(EnvironmentEnableSeLinuxEvent enableSeLinuxEvent) {
            this.enableSeLinuxEvent = enableSeLinuxEvent;
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

        public EnvironmentEnableSeLinuxFailedEvent build() {
            return new EnvironmentEnableSeLinuxFailedEvent(enableSeLinuxEvent, exception, environmentStatus);
        }
    }
}
