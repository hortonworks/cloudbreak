package com.sequenceiq.cloudbreak.audit.converter;

import com.sequenceiq.cloudbreak.audit.model.AuditEventName;
import com.sequenceiq.cloudbreak.audit.model.EventData;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

public interface EventDataExtractor<T extends StructuredEvent> {

    EventData eventData(T structuredEvent);

    AuditEventName eventName(T structuredEvent);

    Crn.Service eventSource(T structuredEvent);

    String sourceIp(T structuredEvent);

    boolean shouldAudit(StructuredEvent structuredEvent);
}
