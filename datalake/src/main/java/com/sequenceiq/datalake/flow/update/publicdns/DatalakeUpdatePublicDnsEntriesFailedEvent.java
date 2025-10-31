package com.sequenceiq.datalake.flow.update.publicdns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatalakeUpdatePublicDnsEntriesFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public DatalakeUpdatePublicDnsEntriesFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }
}
