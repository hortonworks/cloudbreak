package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIpaProviderTemplateUpdateState implements FlowState {

    INIT_STATE,
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_STATE,
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_FINISHED_STATE,
    FREEIPA_PROVIDER_TEMPLATE_UPDATE_FAIL_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }

}
