package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Response;

@Component
public class StructuredNotificationEventToDistroXV1EventResponseConverter
        extends AbstractConversionServiceAwareConverter<StructuredNotificationEvent, DistroXEventV1Response> {

    @Override
    public DistroXEventV1Response convert(StructuredNotificationEvent source) {
        DistroXEventV1Response distroxEvent = new DistroXEventV1Response();
        if (source.getNotificationDetails() != null) {
            NotificationDetails notificationDetails = source.getNotificationDetails();
            setTypesAndMessage(distroxEvent, notificationDetails);
            distroxEvent.setCloud(notificationDetails.getCloud());
            distroxEvent.setRegion(notificationDetails.getRegion());
            distroxEvent.setAvailabilityZone(notificationDetails.getAvailabiltyZone());
            distroxEvent.setBlueprintId(notificationDetails.getBlueprintId());
            distroxEvent.setBlueprintName(notificationDetails.getBlueprintName());
            distroxEvent.setClusterId(notificationDetails.getClusterId());
            distroxEvent.setClusterName(notificationDetails.getClusterName());
            distroxEvent.setStackCrn(notificationDetails.getStackCrn());
            distroxEvent.setStackName(notificationDetails.getStackName());
            distroxEvent.setStackStatus(Status.valueOf(notificationDetails.getStackStatus()));
            distroxEvent.setNodeCount(notificationDetails.getNodeCount());
            distroxEvent.setInstanceGroup(notificationDetails.getInstanceGroup());
            if (notificationDetails.getClusterStatus() != null) {
                distroxEvent.setClusterStatus(Status.valueOf(notificationDetails.getClusterStatus()));
            }
        } else if (source.getLdapNotificationDetails() != null) {
            LdapNotificationDetails notificationDetails = source.getLdapNotificationDetails();
            distroxEvent.setEventType(notificationDetails.getNotificationType());
            distroxEvent.setNotificationType(notificationDetails.getNotificationType());
            distroxEvent.setEventMessage(notificationDetails.getNotification());
            distroxEvent.setLdapDetails(notificationDetails.getLdapDetails());
        } else if (source.getRdsNotificationDetails() != null) {
            RdsNotificationDetails notificationDetails = source.getRdsNotificationDetails();
            distroxEvent.setEventType(notificationDetails.getNotificationType());
            distroxEvent.setNotificationType(notificationDetails.getNotificationType());
            distroxEvent.setEventMessage(notificationDetails.getNotification());
            distroxEvent.setRdsDetails(notificationDetails.getRdsDetails());
        }

        OperationDetails operationDetails = source.getOperation();
        distroxEvent.setEventTimestamp(operationDetails.getTimestamp());
        distroxEvent.setUserId(operationDetails.getUserId());
        distroxEvent.setWorkspaceId(operationDetails.getWorkspaceId());
        distroxEvent.setTenantName(operationDetails.getTenant());

        if (source.getLdapNotificationDetails() != null) {
            distroxEvent.setLdapDetails(source.getLdapNotificationDetails().getLdapDetails());
        }
        if (source.getRdsNotificationDetails() != null) {
            distroxEvent.setRdsDetails(source.getRdsNotificationDetails().getRdsDetails());
        }

        return distroxEvent;
    }

    private void setTypesAndMessage(DistroXEventV1Response cloudbreakEvent, NotificationDetails notificationDetails) {
        cloudbreakEvent.setEventType(notificationDetails.getNotificationType());
        cloudbreakEvent.setNotificationType(notificationDetails.getNotificationType());
        cloudbreakEvent.setEventMessage(notificationDetails.getNotification());
    }
}
