package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RemoveHostsFailed extends StackFailureEvent {

    private String hostGroupName;

    private Set<String> hostGroups;

    private Set<String> failedHostNames;

    public RemoveHostsFailed(Long stackId, Exception exception, Set<String> hostGroups, Set<String> failedHostNames) {
        super(stackId, exception);
        this.failedHostNames = failedHostNames;
        this.hostGroups = hostGroups;
    }

    public RemoveHostsFailed(String selector, Long stackId, Exception exception, Set<String> hostGroups, Set<String> failedHostNames) {
        super(selector, stackId, exception);
        this.failedHostNames = failedHostNames;
        this.hostGroups = hostGroups;
    }

    public Set<String> getFailedHostNames() {
        return failedHostNames;
    }

    public Set<String> getHostGroups() {
        if (hostGroups == null && hostGroupName != null) {
            hostGroups = Set.of(hostGroupName);
        }
        return hostGroups;
    }

}
