package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveHostsResponse extends AbstractCleanupEvent {

    private Set<String> hostCleanupSuccess;

    private Map<String, String> hostCleanupFailed;

    protected RemoveHostsResponse(Long stackId) {
        super(stackId);
    }

    public RemoveHostsResponse(CleanupEvent cleanupEvent, Set<String> hostCleanupSuccess, Map<String, String> hostCleanupFailed) {
        super(cleanupEvent);
        this.hostCleanupSuccess = hostCleanupSuccess;
        this.hostCleanupFailed = hostCleanupFailed;
    }

    public RemoveHostsResponse(String selector, CleanupEvent cleanupEvent, Set<String> hostCleanupSuccess,
            Map<String, String> hostCleanupFailed) {
        super(selector, cleanupEvent);
        this.hostCleanupSuccess = hostCleanupSuccess;
        this.hostCleanupFailed = hostCleanupFailed;
    }

    public Set<String> getHostCleanupSuccess() {
        return hostCleanupSuccess;
    }

    public Map<String, String> getHostCleanupFailed() {
        return hostCleanupFailed;
    }

    @Override
    public String toString() {
        return "RemoveHostsResponse{" +
                "hostCleanupSuccess=" + hostCleanupSuccess +
                ", hostCleanupFailed=" + hostCleanupFailed +
                '}';
    }
}
