package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@Component
public class StructuredNotificationEventToCloudbreakEventJsonConverter
        extends AbstractConversionServiceAwareConverter<StructuredNotificationEvent, CloudbreakEventsJson> {

    @Override
    public CloudbreakEventsJson convert(StructuredNotificationEvent source) {
        NotificationDetails notificationDetails = source.getNotificationDetails();
        OperationDetails operationDetails = source.getOperation();
        return getCloudbreakEventsJson(notificationDetails, operationDetails);
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

    public Page<CloudbreakEventsJson> convertAllForSameStack(Page<StructuredNotificationEvent> events) {
            return events.map(event -> getCloudbreakEventsJson(event.getNotificationDetails(), event.getOperation()));
    }
}
