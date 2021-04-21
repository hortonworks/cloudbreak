package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum KerberosConfigValidationState implements FlowState {

    INIT_STATE,
    CHECK_FREEIPA_EXISTS_STATE,
    CREATE_BIND_USER_STATE,
    POLL_BIND_USER_CREATION_STATE,
    VALIDATE_KERBEROS_CONFIG_STATE,
    VALIDATE_KERBEROS_CONFIG_FAILED_STATE,
    VALIDATE_KERBEROS_CONFIG_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
