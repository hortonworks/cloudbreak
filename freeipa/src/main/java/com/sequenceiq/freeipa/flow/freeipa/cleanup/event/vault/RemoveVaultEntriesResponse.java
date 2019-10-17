package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.vault;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveVaultEntriesResponse extends AbstractCleanupEvent {

    private final Set<String> vaultCleanupSuccess;

    private final Map<String, String> vaultCleanupFailed;

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
}
