package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum UpgradeCcmState implements FlowState {

    INIT_STATE,
    UPGRADE_CCM_CHECK_PREREQUISITES_STATE,
    UPGRADE_CCM_CHANGE_TUNNEL_STATE,
    UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE,
    UPGRADE_CCM_FAILED_STATE,
    UPGRADE_CCM_FINISHED_STATE,
    UPGRADE_CCM_PUSH_SALT_STATES_STATE,
    UPGRADE_CCM_UPGRADE_STATE,
    UPGRADE_CCM_RECONFIGURE_NGINX_STATE,
    UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE,
    UPGRADE_CCM_HEALTH_CHECK_STATE,
    UPGRADE_CCM_REMOVE_MINA_STATE,
    UPGRADE_CCM_DEREGISTER_MINA_STATE,
    UPGRADE_CCM_FINALIZING_STATE,
    UPGRADE_CCM_FINALIZE_FAILED_STATE,
    UPGRADE_CCM_REVERT_FAILURE_STATE,
    UPGRADE_CCM_REVERT_ALL_FAILURE_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }

}
