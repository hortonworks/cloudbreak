package com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CollectAdditionalHostnamesResponse extends StackEvent {

    private final Set<String> hostnames;

    @JsonCreator
    public CollectAdditionalHostnamesResponse(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostnames") Set<String> hostnames) {
        super(stackId);
        this.hostnames = hostnames;
    }

    public Set<String> getHostnames() {
        return hostnames;
    }
}
