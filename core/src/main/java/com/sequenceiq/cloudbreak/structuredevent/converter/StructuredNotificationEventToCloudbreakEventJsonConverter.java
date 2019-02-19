package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

@Component
public class StructuredNotificationEventToCloudbreakEventJsonConverter
        extends AbstractConversionServiceAwareConverter<StructuredNotificationEvent, CloudbreakEventV4Response> {

    @Override
    public CloudbreakEventV4Response convert(StructuredNotificationEvent source) {
        CloudbreakEventV4Response cloudbreakEvent = new CloudbreakEventV4Response();
        if (source.getNotificationDetails() != null) {
            NotificationDetails notificationDetails = source.getNotificationDetails();
            setTypesAndMessage(cloudbreakEvent, notificationDetails);
            cloudbreakEvent.setCloud(notificationDetails.getCloud());
            cloudbreakEvent.setRegion(notificationDetails.getRegion());
            cloudbreakEvent.setAvailabilityZone(notificationDetails.getAvailabiltyZone());
            cloudbreakEvent.setClusterDefinitionId(notificationDetails.getClusterDefinitionId());
            cloudbreakEvent.setClusterDefinitionName(notificationDetails.getClusterDefinitionName());
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
        } else if (source.getLdapNotificationDetails() != null) {
            LdapNotificationDetails notificationDetails = source.getLdapNotificationDetails();
            cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
            cloudbreakEvent.setNotificationType(notificationDetails.getNotificationType());
            cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
            cloudbreakEvent.setLdapDetails(notificationDetails.getLdapDetails());
        } else if (source.getRdsNotificationDetails() != null) {
            RdsNotificationDetails notificationDetails = source.getRdsNotificationDetails();
            cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
            cloudbreakEvent.setNotificationType(notificationDetails.getNotificationType());
            cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
            cloudbreakEvent.setRdsDetails(notificationDetails.getRdsDetails());
        }

        OperationDetails operationDetails = source.getOperation();
        cloudbreakEvent.setEventTimestamp(operationDetails.getTimestamp());
        cloudbreakEvent.setUserId(operationDetails.getUserId());
        cloudbreakEvent.setWorkspaceId(operationDetails.getWorkspaceId());
        cloudbreakEvent.setWorkspaceId(operationDetails.getWorkspaceId());

        if (source.getLdapNotificationDetails() != null) {
            cloudbreakEvent.setLdapDetails(source.getLdapNotificationDetails().getLdapDetails());
        }
        if (source.getRdsNotificationDetails() != null) {
            cloudbreakEvent.setRdsDetails(source.getRdsNotificationDetails().getRdsDetails());
        }

        return cloudbreakEvent;
    }

    private void setTypesAndMessage(CloudbreakEventV4Response cloudbreakEvent, NotificationDetails notificationDetails) {
        cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
        cloudbreakEvent.setNotificationType(notificationDetails.getNotificationType());
        cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
    }
}
