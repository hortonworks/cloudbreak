package com.sequenceiq.cloudbreak.structuredevent;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface StructuredEventSenderService {
    void create(StructuredEvent structuredEvent);

    boolean isEnabled();
}
