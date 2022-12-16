package com.sequenceiq.freeipa.flow.stack.modify.proxy;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.action.ModifyProxyConfigAction;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.action.ModifyProxyConfigFailedAction;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.action.ModifyProxyConfigFinishedAction;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.action.ModifyProxyConfigSaltStateApplyAction;

public enum ModifyProxyConfigState implements FlowState  {

    INIT_STATE,
    MODIFY_PROXY_SALT_STATE_APPLY_STATE(ModifyProxyConfigSaltStateApplyAction.class),
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
