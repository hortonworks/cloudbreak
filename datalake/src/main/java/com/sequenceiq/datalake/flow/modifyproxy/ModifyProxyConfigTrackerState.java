package com.sequenceiq.datalake.flow.modifyproxy;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.datalake.flow.modifyproxy.action.ModifyProxyConfigAction;
import com.sequenceiq.datalake.flow.modifyproxy.action.ModifyProxyConfigFailureAction;
import com.sequenceiq.datalake.flow.modifyproxy.action.ModifyProxyConfigSuccessAction;
import com.sequenceiq.datalake.flow.modifyproxy.action.ModifyProxyConfigWaitAction;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ModifyProxyConfigTrackerState implements FlowState {

    INIT_STATE,
    MODIFY_PROXY_CONFIG_WAITING_STATE(ModifyProxyConfigWaitAction.class),
    MODIFY_PROXY_CONFIG_SUCCESS_STATE(ModifyProxyConfigSuccessAction.class),
    MODIFY_PROXY_CONFIG_FAILED_STATE(ModifyProxyConfigFailureAction.class),
    FINAL_STATE;

    private Class<? extends ModifyProxyConfigAction<?>> action;

    ModifyProxyConfigTrackerState() {
    }

    ModifyProxyConfigTrackerState(Class<? extends ModifyProxyConfigAction<?>> action) {
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
