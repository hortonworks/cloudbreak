package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config;

import com.sequenceiq.flow.core.FlowState;

public enum KerberosConfigValidationState implements FlowState {

    INIT_STATE,
    VALIDATE_KERBEROS_CONFIG_STATE,
    VALIDATE_KERBEROS_CONFIG_FAILED_STATE,
    VALIDATE_KERBEROS_CONFIG_FINISHED_STATE,
    FINAL_STATE
}
