package com.sequenceiq.datalake.flow.cert.rotation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class SdxCertRotationWaitEvent extends SdxEvent {
    public SdxCertRotationWaitEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public SdxCertRotationWaitEvent(SdxContext context) {
        super(context);
    }

    public SdxCertRotationWaitEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    public SdxCertRotationWaitEvent(String selector, SdxContext context) {
        super(selector, context);
    }

    @JsonCreator
    public SdxCertRotationWaitEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
    }
}
