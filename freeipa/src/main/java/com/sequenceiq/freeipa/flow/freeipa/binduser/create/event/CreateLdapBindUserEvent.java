package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import com.sequenceiq.flow.event.EventSelectorUtil;

public class CreateLdapBindUserEvent extends CreateBindUserEvent {
    public CreateLdapBindUserEvent(CreateBindUserEvent event) {
        super(EventSelectorUtil.selector(CreateLdapBindUserEvent.class), event);
    }
}
