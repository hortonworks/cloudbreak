package com.sequenceiq.datalake.flow.dr.datalakeinfo.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SubmitDatalakeDataInfoRequest extends SdxEvent {
    private final String operationId;

    private final String dataInfoJSON;

    @JsonCreator
    public SubmitDatalakeDataInfoRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("dataInfoJSON") String dataInfoJSON) {
        super(sdxId, userId);
        this.operationId = operationId;
        this.dataInfoJSON = dataInfoJSON;
    }

    public static SubmitDatalakeDataInfoRequest from(SdxContext context, String operationId, String dataInfoJSON) {
        return new SubmitDatalakeDataInfoRequest(context.getSdxId(), context.getUserId(), operationId, dataInfoJSON);
    }

    public String getOperationId() {
        return operationId;
    }

    public String getDataInfoJSON() {
        return dataInfoJSON;
    }
}
