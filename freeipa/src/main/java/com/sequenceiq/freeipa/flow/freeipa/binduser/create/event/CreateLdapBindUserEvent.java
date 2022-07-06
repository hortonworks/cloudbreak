package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class CreateLdapBindUserEvent extends CreateBindUserEvent {
    @JsonCreator
    public CreateLdapBindUserEvent(
            @JsonProperty("event") CreateBindUserEvent event) {
        super(EventSelectorUtil.selector(CreateLdapBindUserEvent.class), event);
    }
}
