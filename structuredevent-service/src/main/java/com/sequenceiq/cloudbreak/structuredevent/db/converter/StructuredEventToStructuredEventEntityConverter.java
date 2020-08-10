package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.structuredevent.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventToStructuredEventEntityConverter extends AbstractConversionServiceAwareConverter<StructuredEvent, StructuredEventEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventToStructuredEventEntityConverter.class);

    @Override
    public StructuredEventEntity convert(StructuredEvent source) {
        try {
            StructuredEventEntity structuredEventEntity = new StructuredEventEntity();
            structuredEventEntity.setStructuredEventJson(new Json(source));
            OperationDetails operationDetails = source.getOperation();
            structuredEventEntity.setEventType(operationDetails.getEventType());
            structuredEventEntity.setResourceType(operationDetails.getResourceType());
            structuredEventEntity.setResourceId(operationDetails.getResourceId());
            structuredEventEntity.setResourceCrn(operationDetails.getResourceCrn());
            structuredEventEntity.setTimestamp(operationDetails.getTimestamp());

            return structuredEventEntity;
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to parse structured event JSON", e);
            return null;
        }
    }
}
