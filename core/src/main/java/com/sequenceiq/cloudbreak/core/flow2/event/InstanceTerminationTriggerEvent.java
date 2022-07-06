package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

/**
 * @deprecated Downscale flow is used instead of Instance termination flow
 */
@Deprecated
public class InstanceTerminationTriggerEvent extends StackEvent implements InstancePayload {
    private final Set<String> instanceIds;

    @JsonCreator
    public InstanceTerminationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") Set<String> instanceIds) {
        super(selector, stackId);
        this.instanceIds = instanceIds;
    }

    @Override
    public Set<String> getInstanceIds() {
        return instanceIds;
    }
}
