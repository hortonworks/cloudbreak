package com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ClusterProxyReRegistrationState implements FlowState {
    INIT_STATE,
    CLUSTER_PROXY_CCMV1_REMAP_STATE,
    CLUSTER_PROXY_RE_REGISTRATION_STATE,
    CLUSTER_PROXY_RE_REGISTRATION_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
