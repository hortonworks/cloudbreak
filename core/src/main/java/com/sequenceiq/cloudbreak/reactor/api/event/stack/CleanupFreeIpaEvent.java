package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CleanupFreeIpaEvent extends StackEvent {

    private final Set<String> hostNames;

    private final Set<String> ips;

    private final boolean recover;

    public CleanupFreeIpaEvent(Long stackId, Set<String> hostNames, Set<String> ips, boolean recover) {
        super(stackId);
        this.hostNames = hostNames;
        this.ips = ips;
        this.recover = recover;
    }

    public CleanupFreeIpaEvent(String selector, Long stackId, Set<String> hostNames, Set<String> ips, boolean recover) {
        super(selector, stackId);
        this.hostNames = hostNames;
        this.ips = ips;
        this.recover = recover;
    }

    @JsonCreator
    public CleanupFreeIpaEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("hostNames") Set<String> hostNames,
            @JsonProperty("ips") Set<String> ips,
            @JsonProperty("recover") boolean recover) {
        super(selector, stackId, accepted);
        this.hostNames = hostNames;
        this.ips = ips;
        this.recover = recover;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public Set<String> getIps() {
        return ips;
    }

    public boolean isRecover() {
        return recover;
    }

    @Override
    public String toString() {
        return "CleanupFreeIpaEvent{" +
                "hostNames=" + hostNames +
                ", ips=" + ips +
                ", recover=" + recover +
                '}';
    }
}
