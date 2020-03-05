package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class CleanupFreeIpaEvent extends StackEvent {

    private final Set<String> hostNames;

    private final Set<String> ips;

    public CleanupFreeIpaEvent(Long stackId, Set<String> hostNames, Set<String> ips) {
        super(stackId);
        this.hostNames = hostNames;
        this.ips = ips;
    }

    public CleanupFreeIpaEvent(String selector, Long stackId, Set<String> hostNames, Set<String> ips) {
        super(selector, stackId);
        this.hostNames = hostNames;
        this.ips = ips;
    }

    public CleanupFreeIpaEvent(String selector, Long stackId, Promise<AcceptResult> accepted, Set<String> hostNames, Set<String> ips) {
        super(selector, stackId, accepted);
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
        return "CleanupFreeIpaEvent{" +
                "{super=" + super.toString() + '}' +
                "hostNames=" + hostNames +
                "ips=" + ips +
                '}';
    }
}
