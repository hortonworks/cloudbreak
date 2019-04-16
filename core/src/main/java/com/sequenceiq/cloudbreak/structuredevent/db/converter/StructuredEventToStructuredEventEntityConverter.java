package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventToStructuredEventEntityConverter extends AbstractConversionServiceAwareConverter<StructuredEvent, StructuredEventEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventToStructuredEventEntityConverter.class);

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Override
    public StructuredEventEntity convert(StructuredEvent source) {
        try {
            StructuredEventEntity structuredEventEntity = new StructuredEventEntity();
            structuredEventEntity.setStructuredEventJson(new Json(source));
            OperationDetails operationDetails = source.getOperation();
            structuredEventEntity.setEventType(operationDetails.getEventType());
            structuredEventEntity.setResourceType(operationDetails.getResourceType());
            structuredEventEntity.setResourceId(operationDetails.getResourceId());
            structuredEventEntity.setTimestamp(operationDetails.getTimestamp());
            if (operationDetails.getWorkspaceId() != null) {
                structuredEventEntity.setWorkspace(workspaceService.getByIdWithoutAuth(operationDetails.getWorkspaceId()));
            }
            if (StringUtils.hasLength(operationDetails.getUserId())) {
                structuredEventEntity.setUser(userService.getByUserId(operationDetails.getUserId()).orElse(null));
            }

            return structuredEventEntity;
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse structured event JSON", e);
            return null;
        }
    }
}
