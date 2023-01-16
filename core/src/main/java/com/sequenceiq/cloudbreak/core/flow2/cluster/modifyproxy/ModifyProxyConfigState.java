package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy;

import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action.ModifyProxyConfigAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action.ModifyProxyConfigFailedAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action.ModifyProxyConfigFinishedAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action.ModifyProxyConfigOnCmAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action.ModifyProxyConfigSaltStateApplyAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ModifyProxyConfigState implements FlowState {

    INIT_STATE,
    MODIFY_PROXY_SALT_STATE_APPLY_STATE(ModifyProxyConfigSaltStateApplyAction.class),
    MODIFY_PROXY_ON_CM_STATE(ModifyProxyConfigOnCmAction.class),
    MODIFY_PROXY_FINISHED_STATE(ModifyProxyConfigFinishedAction.class),
    MODIFY_PROXY_FAILED_STATE(ModifyProxyConfigFailedAction.class),
    FINAL_STATE;

    private Class<? extends ModifyProxyConfigAction<?>> action;

    ModifyProxyConfigState() {
    }

    ModifyProxyConfigState(Class<? extends ModifyProxyConfigAction<?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
