package com.sequenceiq.freeipa.flow.freeipa.binduser.create;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.InitializeMDCContextRestartAction;

public enum CreateBindUserState implements FlowState {
    INIT_STATE,
    CREATE_KERBEROS_BIND_USER_STATE,
    CREATE_LDAP_BIND_USER_STATE,
    CREATE_BIND_USER_FAILED_STATE,
    CREATE_BIND_USER_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
