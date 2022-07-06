package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterHostCertificatesRotationSuccess extends StackEvent {
    @JsonCreator
    public ClusterHostCertificatesRotationSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
