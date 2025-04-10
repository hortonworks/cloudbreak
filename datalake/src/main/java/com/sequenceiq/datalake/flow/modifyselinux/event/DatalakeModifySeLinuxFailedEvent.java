package com.sequenceiq.datalake.flow.modifyselinux.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = DatalakeModifySeLinuxFailedEvent.Builder.class)
public class DatalakeModifySeLinuxFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final DatalakeStatusEnum status;

    @JsonCreator
    public DatalakeModifySeLinuxFailedEvent(
            @JsonProperty("enableSeLinuxEvent") DatalakeModifySeLinuxEvent enableSeLinuxEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("status") DatalakeStatusEnum status) {
        super(enableSeLinuxEvent.getSelector(), enableSeLinuxEvent.getResourceId(),
                enableSeLinuxEvent.getResourceName(), enableSeLinuxEvent.getResourceCrn(), exception);
        this.status = status;
    }

    public DatalakeStatusEnum getStatus() {
        return status;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DatalakeModifySeLinuxEvent enableSeLinuxEvent;

        private Exception exception;

        private DatalakeStatusEnum status;

        private Builder() {
        }

        public Builder withEnableSeLinuxEvent(DatalakeModifySeLinuxEvent enableSeLinuxEvent) {
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

        public DatalakeModifySeLinuxFailedEvent build() {
            return new DatalakeModifySeLinuxFailedEvent(enableSeLinuxEvent, exception, status);
        }
    }
}
