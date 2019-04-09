package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
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
        Object payload = null;
        WorkspaceResource resource = null;
        NotificationEventType eventType = null;
        if (source.getNotificationDetails() != null) {
            NotificationDetails notificationDetails = source.getNotificationDetails();
            setTypesAndMessage(cloudbreakEvent, notificationDetails);
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
//            payload = notificationDetails;
//            resource = WorkspaceResource.ALL;
//            eventType = NotificationEventType.valueOf(notificationDetails.getNotificationType());
        } else if (source.getLdapNotificationDetails() != null) {
            LdapNotificationDetails notificationDetails = source.getLdapNotificationDetails();
            cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
            cloudbreakEvent.setNotificationType(notificationDetails.getNotificationType());
            cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
            cloudbreakEvent.setLdapDetails(notificationDetails.getLdapDetails());

//            payload = notificationDetails;
//            resource = WorkspaceResource.LDAP;
//            eventType = NotificationEventType.valueOf(notificationDetails.getNotificationType());
        } else if (source.getRdsNotificationDetails() != null) {
            RdsNotificationDetails notificationDetails = source.getRdsNotificationDetails();
            cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
            cloudbreakEvent.setNotificationType(notificationDetails.getNotificationType());
            cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
            cloudbreakEvent.setRdsDetails(notificationDetails.getRdsDetails());

//            payload = notificationDetails;
//            resource = WorkspaceResource.RDS;
//            eventType = NotificationEventType.valueOf(notificationDetails.getNotificationType());
        }

        OperationDetails operationDetails = source.getOperation();
        cloudbreakEvent.setEventTimestamp(operationDetails.getTimestamp());
        cloudbreakEvent.setUserId(operationDetails.getUserId());
        cloudbreakEvent.setWorkspaceId(operationDetails.getWorkspaceId());
        cloudbreakEvent.setTenantName(operationDetails.getTenant());

        if (source.getLdapNotificationDetails() != null) {
            cloudbreakEvent.setLdapDetails(source.getLdapNotificationDetails().getLdapDetails());
        }
        if (source.getRdsNotificationDetails() != null) {
            cloudbreakEvent.setRdsDetails(source.getRdsNotificationDetails().getRdsDetails());
        }

        // we will return this
//        CloudbreakV4Event cloudbreakV4Event = NotificationAssemblingService.cloudbreakEvent(payload, eventType, resource);

        //TODO: remove notifiaction backward compatible
        return cloudbreakEvent;
    }

    private void setTypesAndMessage(CloudbreakEventV4Response cloudbreakEvent, NotificationDetails notificationDetails) {
        cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
        cloudbreakEvent.setNotificationType(notificationDetails.getNotificationType());
        cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
    }
}
