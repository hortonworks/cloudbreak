package com.sequenceiq.datalake.flow.salt.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SaltUpdateRequest extends SdxEvent {

    private final boolean skipHighstate;

    public SaltUpdateRequest(Long sdxId, String userId) {
        super(sdxId, userId);
        this.skipHighstate = false;
    }

    @JsonCreator
    public SaltUpdateRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("skipHighstate") boolean skipHighstate) {
        super(sdxId, userId);
        this.skipHighstate = skipHighstate;
    }

    public boolean isSkipHighstate() {
        return skipHighstate;
    }

    @Override
    public String toString() {
        return "SaltUpdateRequest{" +
                "skipHighstate=" + skipHighstate +
                "} " + super.toString();
    }
}
