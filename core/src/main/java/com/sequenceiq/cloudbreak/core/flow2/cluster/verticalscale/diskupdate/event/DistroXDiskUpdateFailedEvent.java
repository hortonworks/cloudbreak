package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = DistroXDiskUpdateFailedEvent.Builder.class)
public class DistroXDiskUpdateFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final DistroXDiskUpdateEvent diskUpdateEvent;

    private final Status status;

    public DistroXDiskUpdateFailedEvent(
            @JsonProperty("diskUpdateEvent") DistroXDiskUpdateEvent diskUpdateEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("status") Status status) {
        super(FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), diskUpdateEvent.getResourceId(), null, null,
                diskUpdateEvent.getResourceCrn(), exception);
        this.diskUpdateEvent = diskUpdateEvent;
        this.status = status;
    }

    public DistroXDiskUpdateEvent getDiskUpdateEvent() {
        return diskUpdateEvent;
    }

    public Status getStatus() {
        return status;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DistroXDiskUpdateEvent diskUpdateEvent;

        private Status status;

        private Exception exception;

        private Builder() {
        }

        public Builder withDiskUpdateEvent(DistroXDiskUpdateEvent diskUpdateEvent) {
            this.diskUpdateEvent = diskUpdateEvent;
            return this;
        }

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public DistroXDiskUpdateFailedEvent build() {
            return new DistroXDiskUpdateFailedEvent(diskUpdateEvent, exception, status);
        }
    }
}
