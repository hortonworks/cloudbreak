package com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdatePublicDnsEntriesInPemFinished extends StackEvent {

    @JsonCreator
    public UpdatePublicDnsEntriesInPemFinished(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
