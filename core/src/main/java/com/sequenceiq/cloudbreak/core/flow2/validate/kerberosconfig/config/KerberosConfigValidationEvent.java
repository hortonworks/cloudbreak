package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config;

import com.sequenceiq.flow.core.FlowEvent;

public enum KerberosConfigValidationEvent implements FlowEvent {
    VALIDATE_KERBEROS_CONFIG_EVENT,
    FREEIPA_EXISTS_EVENT,
    BIND_USER_CREATION_STARTED_EVENT,
    VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT,
    VALIDATE_KERBEROS_CONFIG_FAILED_EVENT,
    VALIDATE_KERBEROS_CONFIG_FAILURE_HANDLED_EVENT,
    VALIDATE_KERBEROS_CONFIG_FINISHED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
