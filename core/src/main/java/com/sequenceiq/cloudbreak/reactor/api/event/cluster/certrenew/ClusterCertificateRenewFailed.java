package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterCertificateRenewFailed extends StackFailureEvent {
    public ClusterCertificateRenewFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    @JsonCreator
    public ClusterCertificateRenewFailed(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}
