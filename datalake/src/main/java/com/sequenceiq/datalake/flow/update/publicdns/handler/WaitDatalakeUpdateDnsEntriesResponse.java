package com.sequenceiq.datalake.flow.update.publicdns.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class WaitDatalakeUpdateDnsEntriesResponse extends SdxEvent {

    @JsonCreator
    public WaitDatalakeUpdateDnsEntriesResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
