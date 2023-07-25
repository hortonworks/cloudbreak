package com.sequenceiq.datalake.flow.salt.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SaltUpdateTriggerEvent extends SdxEvent {

    public SaltUpdateTriggerEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @JsonCreator
    public SaltUpdateTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
    }
}
