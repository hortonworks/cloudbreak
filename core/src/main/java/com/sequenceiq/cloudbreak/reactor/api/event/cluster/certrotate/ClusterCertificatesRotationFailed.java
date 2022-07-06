package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterCertificatesRotationFailed extends StackFailureEvent {
    public ClusterCertificatesRotationFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    @JsonCreator
    public ClusterCertificatesRotationFailed(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}
