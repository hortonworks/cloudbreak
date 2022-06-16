package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedSaltFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedSaltSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum CheckAtlasUpdatedSaltEvent implements FlowEvent {
    CHECK_ATLAS_UPDATED_SALT_EVENT(),
    CHECK_ATLAS_UPDATED_SALT_SUCCESS_EVENT(CheckAtlasUpdatedSaltSuccessEvent.class),
    CHECK_ATLAS_UPDATED_SALT_FAILED_EVENT(CheckAtlasUpdatedSaltFailedEvent.class),
    CHECK_ATLAS_UPDATED_SALT_SUCCESS_HANDLED_EVENT(),
    CHECK_ATLAS_UPDATED_SALT_FAILURE_HANDLED_EVENT();

    private final String event;

    CheckAtlasUpdatedSaltEvent() {
        event = name();
    }

    CheckAtlasUpdatedSaltEvent(Class eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}
