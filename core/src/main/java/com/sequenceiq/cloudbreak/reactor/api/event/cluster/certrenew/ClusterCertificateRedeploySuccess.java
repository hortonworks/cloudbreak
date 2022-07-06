package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrenew;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCertificateRedeploySuccess extends StackEvent {
    @JsonCreator
    public ClusterCertificateRedeploySuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
