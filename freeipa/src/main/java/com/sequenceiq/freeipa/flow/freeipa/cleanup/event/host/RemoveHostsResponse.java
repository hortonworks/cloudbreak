package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.host;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveHostsResponse extends AbstractCleanupEvent {

    private final Set<String> hostCleanupSuccess;

    private final Map<String, String> hostCleanupFailed;

    protected RemoveHostsResponse(Long stackId) {
        super(stackId);
        hostCleanupSuccess = null;
        hostCleanupFailed = null;
    }

    public RemoveHostsResponse(CleanupEvent cleanupEvent, Set<String> hostCleanupSuccess, Map<String, String> hostCleanupFailed) {
        super(cleanupEvent);
        this.hostCleanupSuccess = hostCleanupSuccess;
        this.hostCleanupFailed = hostCleanupFailed;
    }

    @JsonCreator
    public RemoveHostsResponse(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent,
            @JsonProperty("hostCleanupSuccess") Set<String> hostCleanupSuccess,
            @JsonProperty("hostCleanupFailed") Map<String, String> hostCleanupFailed) {
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
