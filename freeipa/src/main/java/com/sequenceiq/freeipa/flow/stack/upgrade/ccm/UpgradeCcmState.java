package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public class UpgradeCcmState implements FlowState {
    public static final String UPGRADE_CCM_CHECK_PREREQUISITES_STATE_NAME = "UPGRADE_CCM_CHECK_PREREQUISITES_STATE";

    public static final String UPGRADE_CCM_CHANGE_TUNNEL_STATE_NAME = "UPGRADE_CCM_CHANGE_TUNNEL_STATE";

    public static final String UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE_NAME = "UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE";

    public static final String UPGRADE_CCM_FINISHED_STATE_NAME = "UPGRADE_CCM_FINISHED_STATE";

    public static final String UPGRADE_CCM_FAILED_STATE_NAME = "UPGRADE_CCM_FAILED_STATE";

    public static final String UPGRADE_CCM_FINALIZE_FAILED_STATE_NAME = "UPGRADE_CCM_FINALIZE_FAILED_STATE";

    public static final String UPGRADE_CCM_REVERT_FAILURE_STATE_NAME = "UPGRADE_CCM_REVERT_FAILURE_STATE";

    public static final String UPGRADE_CCM_REVERT_ALL_FAILURE_STATE_NAME = "UPGRADE_CCM_REVERT_ALL_FAILURE_STATE";

    public static final String UPGRADE_CCM_PUSH_SALT_STATES_STATE_NAME = "UPGRADE_CCM_PUSH_SALT_STATES_STATE";

    public static final String UPGRADE_CCM_UPGRADE_STATE_NAME = "UPGRADE_CCM_UPGRADE_STATE";

    public static final String UPGRADE_CCM_RECONFIGURE_NGINX_STATE_NAME = "UPGRADE_CCM_RECONFIGURE_NGINX_STATE";

    public static final String UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE_NAME = "UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE";

    public static final String UPGRADE_CCM_REMOVE_MINA_STATE_NAME = "UPGRADE_CCM_REMOVE_MINA_STATE";

    public static final String UPGRADE_CCM_DEREGISTER_MINA_STATE_NAME = "UPGRADE_CCM_DEREGISTER_MINA_STATE";

    public static final UpgradeCcmState INIT_STATE = new UpgradeCcmState("INIT_STATE");

    public static final UpgradeCcmState UPGRADE_CCM_CHECK_PREREQUISITES_STATE = new UpgradeCcmState(UPGRADE_CCM_CHECK_PREREQUISITES_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_CHANGE_TUNNEL_STATE = new UpgradeCcmState(UPGRADE_CCM_CHANGE_TUNNEL_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE = new UpgradeCcmState(UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_FAILED_STATE = new UpgradeCcmState(UPGRADE_CCM_FAILED_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_FINALIZE_FAILED_STATE = new UpgradeCcmState(UPGRADE_CCM_FINALIZE_FAILED_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_REVERT_FAILURE_STATE = new UpgradeCcmState(UPGRADE_CCM_REVERT_FAILURE_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_REVERT_ALL_FAILURE_STATE = new UpgradeCcmState(UPGRADE_CCM_REVERT_ALL_FAILURE_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_FINISHED_STATE = new UpgradeCcmState(UPGRADE_CCM_FINISHED_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_PUSH_SALT_STATES_STATE = new UpgradeCcmState(UPGRADE_CCM_PUSH_SALT_STATES_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_UPGRADE_STATE = new UpgradeCcmState(UPGRADE_CCM_UPGRADE_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_RECONFIGURE_NGINX_STATE = new UpgradeCcmState(UPGRADE_CCM_RECONFIGURE_NGINX_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE = new UpgradeCcmState(UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_REMOVE_MINA_STATE = new UpgradeCcmState(UPGRADE_CCM_REMOVE_MINA_STATE_NAME);

    public static final UpgradeCcmState UPGRADE_CCM_DEREGISTER_MINA_STATE = new UpgradeCcmState(UPGRADE_CCM_DEREGISTER_MINA_STATE_NAME);

    public static final UpgradeCcmState FINAL_STATE = new UpgradeCcmState("FINAL_STATE");

    private final String name;

    UpgradeCcmState(String name) {
        this.name = name;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }

    @Override
    public String name() {
        return name;
    }
}
