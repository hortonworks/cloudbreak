package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@Component
public class StructuredNotificationEventToCloudbreakEventJsonConverter
        extends AbstractConversionServiceAwareConverter<StructuredNotificationEvent, CloudbreakEventsJson> {

    @Inject
    private StackApiViewService stackApiViewService;

    @Override
    public CloudbreakEventsJson convert(StructuredNotificationEvent source) {
        NotificationDetails notificationDetails = source.getNotificationDetails();
        OperationDetails operationDetails = source.getOperation();
        CloudbreakEventsJson cloudbreakEvent = getCloudbreakEventsJson(notificationDetails, operationDetails);
        cloudbreakEvent.setStackView(stackApiViewService.retrieveById(notificationDetails.getStackId()));
        return cloudbreakEvent;
    }

    private CloudbreakEventsJson getCloudbreakEventsJson(NotificationDetails notificationDetails, OperationDetails operationDetails) {
        CloudbreakEventsJson cloudbreakEvent = new CloudbreakEventsJson();
        cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
        cloudbreakEvent.setEventTimestamp(operationDetails.getTimestamp());
        cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
        cloudbreakEvent.setUserIdV3(operationDetails.getUserIdV3());
        cloudbreakEvent.setWorkspaceId(operationDetails.getWorkspaceId());
        cloudbreakEvent.setAccount(operationDetails.getAccount());
        cloudbreakEvent.setCloud(notificationDetails.getCloud());
        cloudbreakEvent.setRegion(notificationDetails.getRegion());
        cloudbreakEvent.setAvailabilityZone(notificationDetails.getAvailabiltyZone());
        cloudbreakEvent.setBlueprintId(notificationDetails.getBlueprintId());
        cloudbreakEvent.setBlueprintName(notificationDetails.getBlueprintName());
        cloudbreakEvent.setClusterId(notificationDetails.getClusterId());
        cloudbreakEvent.setClusterName(notificationDetails.getClusterName());
        cloudbreakEvent.setStackId(notificationDetails.getStackId());
        cloudbreakEvent.setStackName(notificationDetails.getStackName());
        cloudbreakEvent.setStackStatus(Status.valueOf(notificationDetails.getStackStatus()));
        cloudbreakEvent.setNodeCount(notificationDetails.getNodeCount());
        cloudbreakEvent.setInstanceGroup(notificationDetails.getInstanceGroup());
        if (notificationDetails.getClusterStatus() != null) {
            cloudbreakEvent.setClusterStatus(Status.valueOf(notificationDetails.getClusterStatus()));
        }
        return cloudbreakEvent;
    }

    public List<CloudbreakEventsJson> convertAllForSameStack(List<StructuredNotificationEvent> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        } else {
            Long stackId = events.get(0).getNotificationDetails().getStackId();
            StackViewResponse stackViewResponse = stackApiViewService.retrieveById(stackId);
            return events.stream()
                    .map(event -> getCloudbreakEventsJson(event.getNotificationDetails(), event.getOperation()))
                    .map(event -> {
                        event.setStackView(stackViewResponse);
                        return event;
                    }).collect(Collectors.toList());
        }
    }
}
