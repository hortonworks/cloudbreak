package com.sequenceiq.datalake.flow.verticalscale.event;

import static com.sequenceiq.datalake.flow.verticalscale.event.DatalakeVerticalScaleStateSelectors.FAILED_VERTICAL_SCALING_DATALAKE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@JsonDeserialize(builder = DatalakeVerticalScaleFailedEvent.Builder.class)
public class DatalakeVerticalScaleFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final DatalakeVerticalScaleEvent dataLakeVerticalScaleEvent;

    private final DatalakeStatusEnum datalakeStatus;

    @JsonCreator
    public DatalakeVerticalScaleFailedEvent(
            @JsonProperty("dataLakeVerticalScaleEvent") DatalakeVerticalScaleEvent dataLakeVerticalScaleEvent,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("datalakeStatus") DatalakeStatusEnum datalakeStatus) {
        super(FAILED_VERTICAL_SCALING_DATALAKE_EVENT.name(),
                dataLakeVerticalScaleEvent.getResourceId(),
                null,
                dataLakeVerticalScaleEvent.getResourceName(),
                dataLakeVerticalScaleEvent.getResourceCrn(),
                exception);
        this.dataLakeVerticalScaleEvent = dataLakeVerticalScaleEvent;
        this.datalakeStatus = datalakeStatus;
    }

    public DatalakeVerticalScaleEvent getDataLakeVerticalScaleEvent() {
        return dataLakeVerticalScaleEvent;
    }

    public DatalakeStatusEnum getDatalakeStatus() {
        return datalakeStatus;
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DatalakeVerticalScaleEvent dataLakeVerticalScaleEvent;

        private DatalakeStatusEnum datalakeStatus;

        private Exception exception;

        private Builder() {
        }

        public static Builder anBuilder() {
            return new Builder();
        }

        public Builder withDataLakeVerticalScaleEvent(DatalakeVerticalScaleEvent dataLakeVerticalScaleEvent) {
            this.dataLakeVerticalScaleEvent = dataLakeVerticalScaleEvent;
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

        public DatalakeVerticalScaleFailedEvent build() {
            return new DatalakeVerticalScaleFailedEvent(dataLakeVerticalScaleEvent, exception, datalakeStatus);
        }
    }
}
