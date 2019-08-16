package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RemoveHostsFailed extends StackFailureEvent {
    private String hostGroupName;

    private Set<String> failedHostNames;

    public RemoveHostsFailed(Long stackId, Exception exception, String hostGroupName, Set<String> failedHostNames) {
        super(stackId, exception);
        this.failedHostNames = failedHostNames;
        this.hostGroupName = hostGroupName;
    }

    public RemoveHostsFailed(String selector, Long stackId, Exception exception, String hostGroupName, Set<String> failedHostNames) {
        super(selector, stackId, exception);
        this.failedHostNames = failedHostNames;
        this.hostGroupName = hostGroupName;
    }

    public Set<String> getFailedHostNames() {
        return failedHostNames;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }
}
