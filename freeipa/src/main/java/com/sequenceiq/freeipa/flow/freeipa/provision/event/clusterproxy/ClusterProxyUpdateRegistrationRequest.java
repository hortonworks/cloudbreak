package com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.instance.InstanceEvent;

public class ClusterProxyUpdateRegistrationRequest extends InstanceEvent {
    public ClusterProxyUpdateRegistrationRequest(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public ClusterProxyUpdateRegistrationRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(stackId, instanceIds);
    }
}
