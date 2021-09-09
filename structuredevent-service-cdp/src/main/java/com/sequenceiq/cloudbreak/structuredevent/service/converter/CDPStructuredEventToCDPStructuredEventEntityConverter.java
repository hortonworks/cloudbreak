package com.sequenceiq.cloudbreak.structuredevent.service.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;

@Component
public class CDPStructuredEventToCDPStructuredEventEntityConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredEventToCDPStructuredEventEntityConverter.class);

    public CDPStructuredEventEntity convert(CDPStructuredEvent source) {
        try {
            CDPStructuredEventEntity structuredEventEntity = new CDPStructuredEventEntity();
            structuredEventEntity.setStructuredEventJson(new Json(source));
            CDPOperationDetails operationDetails = source.getOperation();
            structuredEventEntity.setEventType(operationDetails.getEventType());
            structuredEventEntity.setResourceType(operationDetails.getResourceType());
            structuredEventEntity.setResourceCrn(operationDetails.getResourceCrn());
            structuredEventEntity.setTimestamp(operationDetails.getTimestamp());
            structuredEventEntity.setAccountId(source.getOperation().getAccountId());
            return structuredEventEntity;
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to parse structured event JSON, source: {}", source.getType(), e);
            return null;
        }
    }
}
