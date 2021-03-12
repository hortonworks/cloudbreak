package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.InitializeMDCContextRestartAction;

public enum FreeIpaCleanupState implements FlowState {
    INIT_STATE,
    REVOKE_CERTS_STATE,
    REMOVE_HOSTS_STATE,
    REMOVE_DNS_ENTRIES_STATE,
    REMOVE_VAULT_ENTRIES_STATE,
    REMOVE_USERS_STATE,
    REMOVE_ROLES_STATE,
    CLEANUP_FINISHED_STATE,
    CLEANUP_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends RestartAction> restartAction = InitializeMDCContextRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
