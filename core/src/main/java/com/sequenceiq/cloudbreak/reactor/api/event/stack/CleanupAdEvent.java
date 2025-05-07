package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CleanupAdEvent extends StackEvent {

    private final Set<String> hostNames;

    private final Set<String> ips;

    @JsonCreator
    public CleanupAdEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("hostNames")  Set<String> hostNames, @JsonProperty("ips") Set<String> ips) {
        super(stackId);
        this.hostNames = hostNames;
        this.ips = ips;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public Set<String> getIps() {
        return ips;
    }

    @Override
    public String toString() {
        return "CleanupAdEvent{" +
                "hostNames=" + hostNames +
                ", ips=" + ips +
                "} " + super.toString();
    }
}
