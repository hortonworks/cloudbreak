package com.sequenceiq.cloudbreak.service.notification;

import javax.inject.Inject;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

@Component
public class NotificationAssemblingService {
    @Inject
    private MessageSource messageSource;

    public Notification createNotification(CloudbreakEvent cloudbreakEvent) {
        Notification notification = new Notification();
        notification.setEventType(cloudbreakEvent.getEventType());
        notification.setEventTimestamp(cloudbreakEvent.getEventTimestamp());
        notification.setEventMessage(cloudbreakEvent.getEventMessage());
        notification.setOwner(cloudbreakEvent.getOwner());
        notification.setAccount(cloudbreakEvent.getAccount());
        notification.setCloud(cloudbreakEvent.getCloud());
        notification.setRegion(cloudbreakEvent.getRegion());
        notification.setBlueprintName(cloudbreakEvent.getBlueprintName());
        notification.setBlueprintId(cloudbreakEvent.getBlueprintId());
        notification.setStackId(cloudbreakEvent.getStackId());
        notification.setStackName(cloudbreakEvent.getStackName());
        notification.setStackStatus(cloudbreakEvent.getStackStatus());
        notification.setNodeCount(cloudbreakEvent.getNodeCount());
        notification.setInstanceGroup(cloudbreakEvent.getInstanceGroup());
        notification.setClusterStatus(cloudbreakEvent.getClusterStatus());
        return notification;
    }

}
