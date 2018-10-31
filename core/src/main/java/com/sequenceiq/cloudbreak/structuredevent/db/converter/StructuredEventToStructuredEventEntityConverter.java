package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;

@Component
public class StructuredEventToStructuredEventEntityConverter extends AbstractConversionServiceAwareConverter<StructuredEvent, StructuredEventEntity> {

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
                structuredEventEntity.setUser(userService.getByUserId(operationDetails.getUserId()));
            }

            return structuredEventEntity;
        } catch (JsonProcessingException e) {
            // TODO What should we do in case of json processing error
            return null;
        }
    }
}
