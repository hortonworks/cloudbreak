package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;

public class DnsUpdateFinished extends ClusterPlatformRequest {
    public DnsUpdateFinished(Long stackId) {
        super(stackId);
    }
}
