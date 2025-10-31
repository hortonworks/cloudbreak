package com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdatePublicDnsEntriesInPemRequest extends StackEvent {

    @JsonCreator
    public UpdatePublicDnsEntriesInPemRequest(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
