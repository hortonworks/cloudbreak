package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import java.util.Collection;
import java.util.List;

public enum TrustStatus {
    UNKNOWN,
    TRUST_SETUP_REQUIRED,
    TRUST_SETUP_IN_PROGRESS,
    TRUST_SETUP_FAILED,
    TRUST_SETUP_FINISH_REQUIRED,
    TRUST_SETUP_FINISH_IN_PROGRESS,
    TRUST_SETUP_FINISH_FAILED,
    CANCEL_TRUST_SETUP_IN_PROGRESS,
    CANCEL_TRUST_SETUP_FAILED,
    TRUST_ACTIVE,
    TRUST_BROKEN;

    public static final Collection<TrustStatus> FREEIPA_CROSS_REALM_SETUP_FINISH_ENABLE_STATUSES = List.of(
            TRUST_SETUP_FINISH_REQUIRED,
            TRUST_SETUP_FINISH_FAILED);

    public boolean isCrossRealmFinishable() {
        return FREEIPA_CROSS_REALM_SETUP_FINISH_ENABLE_STATUSES.contains(this);
    }
}
