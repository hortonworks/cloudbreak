package com.sequenceiq.cloudbreak.converter.v4.audit;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.StructuredEventEntityToStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventEntityToAuditEventV4ResponseConverter {

    @Inject
    private StructuredEventEntityToStructuredEventConverter structuredEventEntityToStructuredEventConverter;

    public AuditEventV4Response convert(StructuredEventEntity source) {
        StructuredEvent structuredEvent = structuredEventEntityToStructuredEventConverter.convert(source);
        return new AuditEventV4Response(source.getId(), structuredEvent);
    }
}
