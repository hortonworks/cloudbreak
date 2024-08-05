package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerMetadataCollectionSuccess extends StackEvent {
    @JsonCreator
    public LoadBalancerMetadataCollectionSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
