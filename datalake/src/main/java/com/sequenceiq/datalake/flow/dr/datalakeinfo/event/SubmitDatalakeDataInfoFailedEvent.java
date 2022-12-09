package com.sequenceiq.datalake.flow.dr.datalakeinfo.event;

import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_FAILED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SubmitDatalakeDataInfoFailedEvent extends SdxEvent {
    private final Exception ex;

    @JsonCreator
    public SubmitDatalakeDataInfoFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception ex) {
        super(SUBMIT_DATALAKE_DATA_INFO_FAILED_EVENT.event(), sdxId, userId);
        this.ex = ex;
    }

    public Exception getException() {
        return ex;
    }
}
