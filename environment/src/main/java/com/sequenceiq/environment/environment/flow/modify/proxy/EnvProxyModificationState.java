package com.sequenceiq.environment.environment.flow.modify.proxy;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.environment.environment.flow.modify.proxy.action.AbstractEnvProxyModificationAction;
import com.sequenceiq.environment.environment.flow.modify.proxy.action.ProxyConfigModificationFailedStateAction;
import com.sequenceiq.environment.environment.flow.modify.proxy.action.ProxyConfigModificationFinishedStateAction;
import com.sequenceiq.environment.environment.flow.modify.proxy.action.ProxyConfigModificationFreeipaStateAction;
import com.sequenceiq.environment.environment.flow.modify.proxy.action.ProxyConfigModificationStartStateAction;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvProxyModificationState implements FlowState {
    INIT_STATE,
    PROXY_CONFIG_MODIFICATION_START_STATE(ProxyConfigModificationStartStateAction.class),
    PROXY_CONFIG_MODIFICATION_FREEIPA_STATE(ProxyConfigModificationFreeipaStateAction.class),
    PROXY_CONFIG_MODIFICATION_FINISHED_STATE(ProxyConfigModificationFinishedStateAction.class),
    PROXY_CONFIG_MODIFICATION_FAILED_STATE(ProxyConfigModificationFailedStateAction.class),
    FINAL_STATE;

    private Class<? extends AbstractEnvProxyModificationAction<?>> action;

    EnvProxyModificationState() {
    }

    EnvProxyModificationState(Class<? extends AbstractEnvProxyModificationAction<?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
