package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import com.sequenceiq.flow.event.EventSelectorUtil;

public class CreateKerberosBindUserEvent extends CreateBindUserEvent {
    public CreateKerberosBindUserEvent(CreateBindUserEvent event) {
        super(EventSelectorUtil.selector(CreateKerberosBindUserEvent.class), event);
    }
}
