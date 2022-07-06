package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveUsersResponse extends AbstractCleanupEvent {

    private final Set<String> userCleanupSuccess;

    private final Map<String, String> userCleanupFailed;

    protected RemoveUsersResponse(Long stackId) {
        super(stackId);
        userCleanupSuccess = null;
        userCleanupFailed = null;
    }

    public RemoveUsersResponse(CleanupEvent cleanupEvent, Set<String> userCleanupSuccess, Map<String, String> userCleanupFailed) {
        super(cleanupEvent);
        this.userCleanupSuccess = userCleanupSuccess;
        this.userCleanupFailed = userCleanupFailed;
    }

    @JsonCreator
    public RemoveUsersResponse(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent,
            @JsonProperty("userCleanupSuccess") Set<String> userCleanupSuccess,
            @JsonProperty("userCleanupFailed") Map<String, String> userCleanupFailed) {
        super(selector, cleanupEvent);
        this.userCleanupSuccess = userCleanupSuccess;
        this.userCleanupFailed = userCleanupFailed;
    }

    public Set<String> getUserCleanupSuccess() {
        return userCleanupSuccess;
    }

    public Map<String, String> getUserCleanupFailed() {
        return userCleanupFailed;
    }

    @Override
    public String toString() {
        return "RemoveUsersResponse{" +
                "userCleanupSuccess=" + userCleanupSuccess +
                ", userCleanupFailed=" + userCleanupFailed +
                '}';
    }
}
