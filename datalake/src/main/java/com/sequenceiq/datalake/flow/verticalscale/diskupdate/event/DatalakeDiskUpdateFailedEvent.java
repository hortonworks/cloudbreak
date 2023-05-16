package com.sequenceiq.datalake.flow.verticalscale.diskupdate.event;

import static com.sequenceiq.datalake.flow.verticalscale.diskupdate.event.DatalakeDiskUpdateStateSelectors.FAILED_DATALAKE_DISK_UPDATE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = DatalakeDiskUpdateFailedEvent.Builder.class)
public class DatalakeDiskUpdateFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final DatalakeDiskUpdateEvent datalakeDiskUpdateEvent;

    private final DatalakeStatusEnum datalakeStatus;

    @JsonCreator
    public DatalakeDiskUpdateFailedEvent(
            @JsonProperty("datalakeDiskUpdateEvent") DatalakeDiskUpdateEvent datalakeDiskUpdateEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("datalakeStatus") DatalakeStatusEnum datalakeStatus) {
        super(FAILED_DATALAKE_DISK_UPDATE_EVENT.name(),
                datalakeDiskUpdateEvent.getResourceId(),
                null,
                datalakeDiskUpdateEvent.getResourceName(),
                datalakeDiskUpdateEvent.getResourceCrn(),
                exception);
        this.datalakeDiskUpdateEvent = datalakeDiskUpdateEvent;
        this.datalakeStatus = datalakeStatus;
    }

    public DatalakeDiskUpdateEvent getDatalakeDiskUpdateEvent() {
        return datalakeDiskUpdateEvent;
    }

    public DatalakeStatusEnum getDatalakeStatus() {
        return datalakeStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DatalakeDiskUpdateEvent datalakeDiskUpdateEvent;

        private DatalakeStatusEnum datalakeStatus;

        private Exception exception;

        private Builder() {
        }

        public static Builder anBuilder() {
            return new Builder();
        }

        public Builder withDatalakeDiskUpdateEvent(DatalakeDiskUpdateEvent datalakeDiskUpdateEvent) {
            this.datalakeDiskUpdateEvent = datalakeDiskUpdateEvent;
            return this;
        }

        public Builder withDatalakeStatus(DatalakeStatusEnum datalakeStatus) {
            this.datalakeStatus = datalakeStatus;
            return this;
        }

        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public DatalakeDiskUpdateFailedEvent build() {
            return new DatalakeDiskUpdateFailedEvent(datalakeDiskUpdateEvent, exception, datalakeStatus);
        }
    }
}
