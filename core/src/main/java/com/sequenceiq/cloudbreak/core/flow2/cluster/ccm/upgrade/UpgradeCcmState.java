package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum UpgradeCcmState implements FlowState {
    INIT_STATE,
    UPGRADE_CCM_FAILED_STATE,
    UPGRADE_CCM_TUNNEL_UPDATE_STATE,
    UPGRADE_CCM_PUSH_SALT_STATES_STATE,
    UPGRADE_CCM_RECONFIGURE_NGINX_STATE,
    UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE,
    UPGRADE_CCM_HEALTH_CHECK_STATE,
    UPGRADE_CCM_REMOVE_AGENT_STATE,
    UPGRADE_CCM_DEREGISTER_AGENT_STATE,
    UPGRADE_CCM_FINISHED_STATE,

    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    UpgradeCcmState() {

    }

    UpgradeCcmState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
