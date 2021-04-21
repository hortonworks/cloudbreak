package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CheckFreeIpaExistsEvent extends StackEvent {
    public CheckFreeIpaExistsEvent(Long stackId) {
        super(stackId);
    }
}
