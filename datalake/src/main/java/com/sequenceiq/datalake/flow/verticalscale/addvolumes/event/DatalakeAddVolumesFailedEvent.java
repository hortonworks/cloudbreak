package com.sequenceiq.datalake.flow.verticalscale.addvolumes.event;

import static com.sequenceiq.datalake.flow.verticalscale.addvolumes.event.DatalakeAddVolumesStateSelectors.FAILED_DATALAKE_ADD_VOLUMES_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeAddVolumesFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public DatalakeAddVolumesFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    @Override
    public String selector() {
        return FAILED_DATALAKE_ADD_VOLUMES_EVENT.name();
    }
}