package com.sequenceiq.environment.environment.flow.modify.tags.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvTagsModificationHandlerSelectors implements FlowEvent {
    MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT,
    MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT,
    MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT;

    @Override
    public String event() {
        return name();
    }
}