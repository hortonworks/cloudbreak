package com.sequenceiq.environment.environment.flow.modify.tags;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvTagsModificationState implements FlowState {
    INIT_STATE,
    USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE,
    USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE,
    USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE,
    USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE,
    USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}