package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveVaultEntriesResponse extends AbstractCleanupEvent {

    private Set<String> vaultCleanupSuccess;

    private Map<String, String> vaultCleanupFailed;

    protected RemoveVaultEntriesResponse(Long stackId) {
        super(stackId);
    }

    public RemoveVaultEntriesResponse(CleanupEvent cleanupEvent, Set<String> vaultCleanupSuccess,
            Map<String, String> vaultCleanupFailed) {
        super(cleanupEvent);
        this.vaultCleanupSuccess = vaultCleanupSuccess;
        this.vaultCleanupFailed = vaultCleanupFailed;
    }

    public RemoveVaultEntriesResponse(String selector, CleanupEvent cleanupEvent, Set<String> vaultCleanupSuccess,
            Map<String, String> vaultCleanupFailed) {
        super(selector, cleanupEvent);
        this.vaultCleanupSuccess = vaultCleanupSuccess;
        this.vaultCleanupFailed = vaultCleanupFailed;
    }

    public Set<String> getVaultCleanupSuccess() {
        return vaultCleanupSuccess;
    }

    public Map<String, String> getVaultCleanupFailed() {
        return vaultCleanupFailed;
    }

    @Override
    public String toString() {
        return "RemoveVaultEntriesResponse{" +
                "vaultCleanupSuccess=" + vaultCleanupSuccess +
                ", vaultCleanupFailed=" + vaultCleanupFailed +
                '}';
    }
}
