package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RemoveHostsSuccess extends StackEvent {
    private String hostGroupName;

    private Set<String> hostNames;

    public RemoveHostsSuccess(Long stackId, String hostGroupName, Set<String> hostNames) {
        super(stackId);
        this.hostGroupName = hostGroupName;
        this.hostNames = hostNames;
    }

    public RemoveHostsSuccess(String selector, Long stackId, String hostGroupName, Set<String> hostNames) {
        super(selector, stackId);
        this.hostGroupName = hostGroupName;
        this.hostNames = hostNames;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
