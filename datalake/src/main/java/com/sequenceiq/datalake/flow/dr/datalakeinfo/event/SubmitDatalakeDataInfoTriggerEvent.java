package com.sequenceiq.datalake.flow.dr.datalakeinfo.event;

import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SubmitDatalakeDataInfoTriggerEvent extends SdxEvent {
    private final String operationId;

    private final String dataInfoJSON;

    @JsonCreator
    public SubmitDatalakeDataInfoTriggerEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("dataInfoJSON") String dataInfoJSON) {
        super(SUBMIT_DATALAKE_DATA_INFO_EVENT.event(), sdxId, userId);
        this.operationId = operationId;
        this.dataInfoJSON = dataInfoJSON;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getDataInfoJSON() {
        return dataInfoJSON;
    }
}
