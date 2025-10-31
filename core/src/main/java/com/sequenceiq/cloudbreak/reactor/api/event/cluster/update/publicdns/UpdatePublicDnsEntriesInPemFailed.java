package com.sequenceiq.cloudbreak.reactor.api.event.cluster.update.publicdns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpdatePublicDnsEntriesInPemFailed extends StackFailureEvent {

    public UpdatePublicDnsEntriesInPemFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    @JsonCreator
    public UpdatePublicDnsEntriesInPemFailed(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}
