package com.sequenceiq.freeipa.entity.projection;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;

public record StackUserSyncView(Long id, String resourceCrn, String name, String environmentCrn, String accountId, String cloudPlatform, Status status) {
    public boolean isAvailable() {
        return AVAILABLE.equals(status);
    }
}
