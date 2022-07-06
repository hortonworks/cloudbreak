package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveServersResponse extends AbstractCleanupEvent {

    private final Set<String> serverCleanupSuccess;

    private final Map<String, String> serverCleanupFailed;

    protected RemoveServersResponse(Long stackId) {
        super(stackId);
        serverCleanupSuccess = null;
        serverCleanupFailed = null;
    }

    @JsonCreator
    public RemoveServersResponse(
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent,
            @JsonProperty("serverCleanupSuccess") Set<String> serverCleanupSuccess,
            @JsonProperty("serverCleanupFailed") Map<String, String> serverCleanupFailed) {
        super(cleanupEvent);
        this.serverCleanupSuccess = serverCleanupSuccess;
        this.serverCleanupFailed = serverCleanupFailed;
    }

    public Set<String> getServerCleanupSuccess() {
        return serverCleanupSuccess;
    }

    public Map<String, String> getServerCleanupFailed() {
        return serverCleanupFailed;
    }

    @Override
    public String toString() {
        return "RemoveServersResponse{" +
                "serverCleanupSuccess=" + serverCleanupSuccess +
                ", serverCleanupFailed=" + serverCleanupFailed +
                '}';
    }
}
