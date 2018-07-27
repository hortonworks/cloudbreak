package com.sequenceiq.cloudbreak.converter.audit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.audit.AuditEvent;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.db.converter.StructuredEventEntityToStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventEntityToAuditEventConverter extends AbstractConversionServiceAwareConverter<StructuredEventEntity, AuditEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventEntityToAuditEventConverter.class);

    @Inject
    private StructuredEventEntityToStructuredEventConverter structuredEventEntityToStructuredEventConverter;

    @Override
    public AuditEvent convert(StructuredEventEntity source) {
        StructuredEvent structuredEvent = structuredEventEntityToStructuredEventConverter.convert(source);
        return new AuditEvent(source.getId(), structuredEvent);
    }
}
