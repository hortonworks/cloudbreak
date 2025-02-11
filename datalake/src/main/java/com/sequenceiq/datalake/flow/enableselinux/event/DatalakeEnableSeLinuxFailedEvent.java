package com.sequenceiq.datalake.flow.enableselinux.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = DatalakeEnableSeLinuxFailedEvent.Builder.class)
public class DatalakeEnableSeLinuxFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final DatalakeStatusEnum status;

    @JsonCreator
    public DatalakeEnableSeLinuxFailedEvent(
            @JsonProperty("enableSeLinuxEvent") DatalakeEnableSeLinuxEvent enableSeLinuxEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("status") DatalakeStatusEnum status) {
        super(DatalakeEnableSeLinuxStateSelectors.FAILED_ENABLE_SELINUX_DATALAKE_EVENT.name(), enableSeLinuxEvent.getResourceId(),
                enableSeLinuxEvent.getResourceName(), enableSeLinuxEvent.getResourceCrn(), exception);
        this.status = status;
    }

    public DatalakeStatusEnum getStatus() {
        return status;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DatalakeEnableSeLinuxEvent enableSeLinuxEvent;

        private Exception exception;

        private DatalakeStatusEnum status;

        private Builder() {
        }

        public Builder withEnableSeLinuxEvent(DatalakeEnableSeLinuxEvent enableSeLinuxEvent) {
            this.enableSeLinuxEvent = enableSeLinuxEvent;
            return this;
        }

        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public Builder withStatus(DatalakeStatusEnum status) {
            this.status = status;
            return this;
        }

        public DatalakeEnableSeLinuxFailedEvent build() {
            return new DatalakeEnableSeLinuxFailedEvent(enableSeLinuxEvent, exception, status);
        }
    }
}
