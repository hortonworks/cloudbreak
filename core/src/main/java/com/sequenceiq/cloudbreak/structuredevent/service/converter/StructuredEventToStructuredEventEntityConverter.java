package com.sequenceiq.cloudbreak.structuredevent.service.converter;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

@Component
public class StructuredEventToStructuredEventEntityConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventToStructuredEventEntityConverter.class);

    @Inject
    private WorkspaceService workspaceService;

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
            if (operationDetails.getWorkspaceId() != null) {
                structuredEventEntity.setWorkspace(workspaceService.getByIdWithoutAuth(operationDetails.getWorkspaceId()));
            }
            structuredEventEntity.setUserCrn(operationDetails.getUserCrn());

            return structuredEventEntity;
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to parse structured event JSON", e);
            return null;
        }
    }
}
