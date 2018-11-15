package com.sequenceiq.cloudbreak.structuredevent.converter;

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
        CloudbreakEventsJson cloudbreakEvent = new CloudbreakEventsJson();
        NotificationDetails notificationDetails = source.getNotificationDetails();
        OperationDetails operationDetails = source.getOperation();
        cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
        cloudbreakEvent.setEventTimestamp(operationDetails.getTimestamp());
        cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
        cloudbreakEvent.setUserId(operationDetails.getUserId());
        cloudbreakEvent.setWorkspaceId(operationDetails.getWorkspaceId());
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
}
