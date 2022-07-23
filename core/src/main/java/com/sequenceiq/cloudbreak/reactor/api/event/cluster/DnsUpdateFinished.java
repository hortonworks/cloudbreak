package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class DnsUpdateFinished extends ClusterPlatformRequest implements FlowPayload {

    @JsonCreator
    public DnsUpdateFinished(@JsonProperty("stackId") Long stackId) {
        super(stackId);
    }
}
