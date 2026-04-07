package com.sequenceiq.freeipa.flow.stack.modify.tags.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ModifyUserDefinedTagsStateSelectors implements FlowEvent {
    MODIFY_USER_DEFINED_TAGS_FREEIPA_START_EVENT,
    MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT,
    FINISH_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT,
    FINALIZE_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT,
    HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT,
    FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;

    @Override
    public String event() {
        return name();
    }
}
