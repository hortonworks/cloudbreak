package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ModifyUserDefinedTagsStateSelectors implements FlowEvent {
    MODIFY_USER_DEFINED_TAGS_REDBEAMS_START_EVENT,
    MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT,
    FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT,
    FINALIZE_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT,
    HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT,
    FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;

    @Override
    public String event() {
        return name();
    }
}
