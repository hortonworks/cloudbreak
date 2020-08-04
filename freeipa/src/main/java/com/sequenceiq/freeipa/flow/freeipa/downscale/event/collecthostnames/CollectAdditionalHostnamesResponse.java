package com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

import java.util.Set;

public class CollectAdditionalHostnamesResponse extends StackEvent {

    private Set<String> hostnames;

    public CollectAdditionalHostnamesResponse(Long stackId, Set<String> hostnames) {
        super(stackId);
        this.hostnames = hostnames;
    }

    public Set<String> getHostnames() {
        return hostnames;
    }
}
