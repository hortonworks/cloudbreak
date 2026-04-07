package com.sequenceiq.environment.environment.flow.modify.tags.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvTagsModificationStateSelectors implements FlowEvent {
    START_MODIFY_ENVIRONMENT_TAGS_EVENT,
    START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT,
    START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT,
    START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT,
    FINISH_MODIFY_USER_DEFINED_TAGS_EVENT,
    FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT,
    FAILED_MODIFY_USER_DEFINED_TAGS_EVENT,
    HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;

    @Override
    public String event() {
        return name();
    }
}