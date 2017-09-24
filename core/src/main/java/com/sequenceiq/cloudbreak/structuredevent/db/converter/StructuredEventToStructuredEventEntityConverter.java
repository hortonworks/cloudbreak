package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventToStructuredEventEntityConverter extends AbstractConversionServiceAwareConverter<StructuredEvent, StructuredEventEntity> {
    @Override
    public StructuredEventEntity convert(StructuredEvent source) {
        try {
            OperationDetails operationDetails = source.getOperation();
            StructuredEventEntity structuredEventEntity = new StructuredEventEntity();
            structuredEventEntity.setEventType(operationDetails.getEventType());
            structuredEventEntity.setResourceType(operationDetails.getResourceType());
            structuredEventEntity.setResourceId(operationDetails.getResourceId());
            structuredEventEntity.setTimestamp(operationDetails.getTimestamp());
            structuredEventEntity.setAccount(operationDetails.getAccount());
            structuredEventEntity.setUserId(operationDetails.getUserId());
            structuredEventEntity.setStructuredEventJson(new Json(source));
            return structuredEventEntity;
        } catch (JsonProcessingException e) {
            // TODO What should we do in case of json processing error
            return null;
        }
    }
}
