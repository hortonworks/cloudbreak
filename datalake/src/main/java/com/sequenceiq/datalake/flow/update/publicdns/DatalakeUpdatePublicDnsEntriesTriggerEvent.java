package com.sequenceiq.datalake.flow.update.publicdns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpdatePublicDnsEntriesTriggerEvent extends SdxEvent {

    public DatalakeUpdatePublicDnsEntriesTriggerEvent(String selector, Long sdxId, String userId) {
        super(selector, sdxId, userId);
    }

    @JsonCreator
    public DatalakeUpdatePublicDnsEntriesTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
    }

    @Override
    public String toString() {
        return "DatalakeUpdatePublicDnsEntriesTriggerEvent{" + super.toString() + "}";
    }
}
