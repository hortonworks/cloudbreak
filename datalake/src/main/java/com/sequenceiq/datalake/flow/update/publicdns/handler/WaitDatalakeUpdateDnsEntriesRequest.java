package com.sequenceiq.datalake.flow.update.publicdns.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class WaitDatalakeUpdateDnsEntriesRequest extends SdxEvent {

    @JsonCreator
    public WaitDatalakeUpdateDnsEntriesRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
