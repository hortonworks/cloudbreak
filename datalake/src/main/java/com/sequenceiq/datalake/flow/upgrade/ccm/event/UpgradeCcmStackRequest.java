package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmStackRequest extends SdxEvent {

    @JsonCreator
    public UpgradeCcmStackRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static UpgradeCcmStackRequest from(SdxContext context) {
        return new UpgradeCcmStackRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return UpgradeCcmStackRequest.class.getSimpleName();
    }
}

