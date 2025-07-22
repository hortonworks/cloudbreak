package com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum CoreProviderTemplateUpdateState implements FlowState {

    INIT_STATE,
    CORE_PROVIDER_TEMPLATE_VALIDATION_STATE,
    CORE_PROVIDER_TEMPLATE_UPDATE_STATE,
    CORE_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE,
    CORE_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }

}
