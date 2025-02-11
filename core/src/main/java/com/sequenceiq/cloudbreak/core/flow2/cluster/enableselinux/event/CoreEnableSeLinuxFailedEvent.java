package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = CoreEnableSeLinuxFailedEvent.Builder.class)
public class CoreEnableSeLinuxFailedEvent extends BaseFailedFlowEvent implements Selectable {

    @JsonCreator
    public CoreEnableSeLinuxFailedEvent(
            @JsonProperty("enableSeLinuxEvent") CoreEnableSeLinuxEvent enableSeLinuxEvent,
            @JsonProperty("exception") Exception exception) {
        super(CoreEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_CORE_EVENT.name(), enableSeLinuxEvent.getResourceId(),
                enableSeLinuxEvent.getResourceName(), enableSeLinuxEvent.getResourceCrn(), exception);
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private CoreEnableSeLinuxEvent enableSeLinuxEvent;

        private Exception exception;

        private Builder() {
        }

        public Builder withEnableSeLinuxEvent(CoreEnableSeLinuxEvent enableSeLinuxEvent) {
            this.enableSeLinuxEvent = enableSeLinuxEvent;
            return this;
        }

        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public CoreEnableSeLinuxFailedEvent build() {
            return new CoreEnableSeLinuxFailedEvent(enableSeLinuxEvent, exception);
        }
    }
}
