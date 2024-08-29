package com.sequenceiq.datalake.flow.verticalscale.rootvolume.event;

import static com.sequenceiq.datalake.flow.verticalscale.rootvolume.event.DatalakeRootVolumeUpdateStateSelectors.FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = DatalakeRootVolumeUpdateFailedEvent.Builder.class)
public class DatalakeRootVolumeUpdateFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final DatalakeRootVolumeUpdateEvent datalakeRootVolumeUpdateEvent;

    private final DatalakeStatusEnum datalakeStatus;

    private DatalakeRootVolumeUpdateFailedEvent(
            @JsonProperty("datalakeDiskUpdateEvent") DatalakeRootVolumeUpdateEvent datalakeRootVolumeUpdateEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("datalakeStatus") DatalakeStatusEnum datalakeStatus) {
        super(FAILED_DATALAKE_ROOT_VOLUME_UPDATE_EVENT.name(),
                datalakeRootVolumeUpdateEvent.getResourceId(),
                datalakeRootVolumeUpdateEvent.getResourceName(),
                datalakeRootVolumeUpdateEvent.getResourceCrn(),
                exception);
        this.datalakeRootVolumeUpdateEvent = datalakeRootVolumeUpdateEvent;
        this.datalakeStatus = datalakeStatus;
    }

    public DatalakeRootVolumeUpdateEvent getDatalakeRootVolumeUpdateEvent() {
        return datalakeRootVolumeUpdateEvent;
    }

    public DatalakeStatusEnum getDatalakeStatus() {
        return datalakeStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DatalakeRootVolumeUpdateEvent datalakeRootVolumeUpdateEvent;

        private DatalakeStatusEnum datalakeStatus;

        private Exception exception;

        private Builder() {
        }

        public Builder withDatalakeRootVolumeUpdateEvent(DatalakeRootVolumeUpdateEvent datalakeRootVolumeUpdateEvent) {
            this.datalakeRootVolumeUpdateEvent = datalakeRootVolumeUpdateEvent;
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

        public DatalakeRootVolumeUpdateFailedEvent build() {
            return new DatalakeRootVolumeUpdateFailedEvent(datalakeRootVolumeUpdateEvent, exception, datalakeStatus);
        }
    }
}
