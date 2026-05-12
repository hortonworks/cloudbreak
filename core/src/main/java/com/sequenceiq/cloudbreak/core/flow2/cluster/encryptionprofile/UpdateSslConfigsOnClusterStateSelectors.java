package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import com.sequenceiq.flow.core.FlowEvent;

public enum UpdateSslConfigsOnClusterStateSelectors implements FlowEvent {

    UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT,
    SET_ENCRYPTION_PROFILE_HANDLER_EVENT,
    UPDATE_CM_POLICY_EVENT,
    UPDATE_CM_POLICY_HANDLER_EVENT,
    GENERATE_ALTERNATIVE_CERTIFICATE_EVENT,
    GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT,
    FINALIZE_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT,
    FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT,
    HANDLED_FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT,
    FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT;

    @Override
    public String event() {
        return name();
    }
}