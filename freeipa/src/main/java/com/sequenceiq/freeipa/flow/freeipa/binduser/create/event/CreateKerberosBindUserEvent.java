package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class CreateKerberosBindUserEvent extends CreateBindUserEvent {
    @JsonCreator
    public CreateKerberosBindUserEvent(
            @JsonProperty("event") CreateBindUserEvent event) {
        super(EventSelectorUtil.selector(CreateKerberosBindUserEvent.class), event);
    }
}
