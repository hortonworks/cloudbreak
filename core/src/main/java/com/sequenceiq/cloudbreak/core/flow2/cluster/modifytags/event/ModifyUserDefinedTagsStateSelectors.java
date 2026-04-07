package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ModifyUserDefinedTagsStateSelectors implements FlowEvent {
    MODIFY_USER_DEFINED_TAGS_START_EVENT,
    MODIFY_USER_DEFINED_TAGS_STACK_EVENT,
    FINISH_MODIFY_USER_DEFINED_TAGS_EVENT,
    FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT,
    HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT,
    FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;

    @Override
    public String event() {
        return name();
    }
}
