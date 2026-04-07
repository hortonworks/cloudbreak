package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

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
