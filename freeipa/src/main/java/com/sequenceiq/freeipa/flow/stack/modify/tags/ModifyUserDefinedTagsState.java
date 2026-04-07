package com.sequenceiq.freeipa.flow.stack.modify.tags;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum ModifyUserDefinedTagsState implements FlowState {
    INIT_STATE,
    MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE,
    MODIFY_USER_DEFINED_TAGS_STACK_STATE,
    MODIFY_USER_DEFINED_TAGS_FINISHED_STATE,
    MODIFY_USER_DEFINED_TAGS_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
