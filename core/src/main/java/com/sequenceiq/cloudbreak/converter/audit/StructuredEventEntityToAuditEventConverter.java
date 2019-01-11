package com.sequenceiq.cloudbreak.converter.audit;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.db.converter.StructuredEventEntityToStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventEntityToAuditEventConverter extends AbstractConversionServiceAwareConverter<StructuredEventEntity, AuditEventV4Response> {

    @Inject
    private StructuredEventEntityToStructuredEventConverter structuredEventEntityToStructuredEventConverter;

    @Override
    public AuditEventV4Response convert(StructuredEventEntity source) {
        StructuredEvent structuredEvent = structuredEventEntityToStructuredEventConverter.convert(source);
        return new AuditEventV4Response(source.getId(), structuredEvent);
    }
}
