package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.roles;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveRolesResponse extends AbstractCleanupEvent {

    private final Set<String> roleCleanupSuccess;

    private final Map<String, String> roleCleanupFailed;

    protected RemoveRolesResponse(Long stackId) {
        super(stackId);
        roleCleanupSuccess = null;
        roleCleanupFailed = null;
    }

    public RemoveRolesResponse(CleanupEvent cleanupEvent, Set<String> roleCleanupSuccess, Map<String, String> roleCleanupFailed) {
        super(cleanupEvent);
        this.roleCleanupSuccess = roleCleanupSuccess;
        this.roleCleanupFailed = roleCleanupFailed;
    }

    @JsonCreator
    public RemoveRolesResponse(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent,
            @JsonProperty("roleCleanupSuccess") Set<String> roleCleanupSuccess,
            @JsonProperty("roleCleanupFailed") Map<String, String> roleCleanupFailed) {
        super(selector, cleanupEvent);
        this.roleCleanupSuccess = roleCleanupSuccess;
        this.roleCleanupFailed = roleCleanupFailed;
    }

    public Set<String> getRoleCleanupSuccess() {
        return roleCleanupSuccess;
    }

    public Map<String, String> getRoleCleanupFailed() {
        return roleCleanupFailed;
    }

    @Override
    public String toString() {
        return "RemoveRolesResponse{" +
                "roleCleanupSuccess=" + roleCleanupSuccess +
                ", roleCleanupFailed=" + roleCleanupFailed +
                '}';
    }
}
