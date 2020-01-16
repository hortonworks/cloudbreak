package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.users;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveUsersResponse extends AbstractCleanupEvent {

    private final Set<String> userCleanupSuccess;

    private final Map<String, String> userCleanupFailed;

    public RemoveUsersResponse(CleanupEvent cleanupEvent, Set<String> userCleanupSuccess, Map<String, String> userCleanupFailed) {
        super(cleanupEvent);
        this.userCleanupSuccess = userCleanupSuccess;
        this.userCleanupFailed = userCleanupFailed;
    }

    public RemoveUsersResponse(String selector, CleanupEvent cleanupEvent, Set<String> userCleanupSuccess,
            Map<String, String> userCleanupFailed) {
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
