package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RemoveHostsSuccess extends StackEvent {

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private String hostGroupName;

    private Set<String> hostGroups;

    private Set<String> hostNames;

    public RemoveHostsSuccess(Long stackId, Set<String> hostGroups, Set<String> hostNames) {
        super(stackId);
        this.hostGroups = hostGroups;
        this.hostNames = hostNames;
    }

    public RemoveHostsSuccess(String selector, Long stackId, Set<String> hostGroups, Set<String> hostNames) {
        super(selector, stackId);
        this.hostGroups = hostGroups;
        this.hostNames = hostNames;
    }

    public Set<String> getHostGroups() {
        if (hostGroups == null && hostGroupName != null) {
            hostGroups = Set.of(hostGroupName);
        }
        return hostGroups;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
