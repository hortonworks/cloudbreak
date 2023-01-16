package com.sequenceiq.datalake.flow.modifyproxy.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class ModifyProxyConfigSuccessResponse extends SdxEvent {
    @JsonCreator
    public ModifyProxyConfigSuccessResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
