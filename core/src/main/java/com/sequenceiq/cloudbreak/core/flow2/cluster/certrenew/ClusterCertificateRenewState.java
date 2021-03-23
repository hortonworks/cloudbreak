package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum  ClusterCertificateRenewState implements FlowState {
    INIT_STATE,
    CLUSTER_CERTIFICATE_REISSUE_STATE,
    CLUSTER_CERTIFICATE_REDEPLOY_STATE,
    CLUSTER_CERTIFICATE_RENEWAL_FINISHED_STATE,
    CLUSTER_CERTIFICATE_RENEW_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
