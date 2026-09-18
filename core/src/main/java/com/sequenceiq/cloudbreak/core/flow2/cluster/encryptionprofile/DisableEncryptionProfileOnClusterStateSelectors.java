package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import com.sequenceiq.flow.core.FlowEvent;

public enum DisableEncryptionProfileOnClusterStateSelectors implements FlowEvent {

    DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT,
    UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT,
    UNSET_ENCRYPTION_PROFILE_FINISHED_EVENT,
    FINALIZE_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT,
    HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT,
    FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;

    @Override
    public String event() {
        return name();
    }
}
