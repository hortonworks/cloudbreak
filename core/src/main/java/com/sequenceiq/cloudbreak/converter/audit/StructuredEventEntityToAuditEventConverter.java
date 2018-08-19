package com.sequenceiq.cloudbreak.converter.audit;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.db.converter.StructuredEventEntityToStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventEntityToAuditEventConverter extends AbstractConversionServiceAwareConverter<StructuredEventEntity, AuditEvent> {

    @Inject
    private StructuredEventEntityToStructuredEventConverter structuredEventEntityToStructuredEventConverter;

    @Override
    public AuditEvent convert(StructuredEventEntity source) {
        StructuredEvent structuredEvent = structuredEventEntityToStructuredEventConverter.convert(source);
        return new AuditEvent(source.getId(), structuredEvent);
    }
}
