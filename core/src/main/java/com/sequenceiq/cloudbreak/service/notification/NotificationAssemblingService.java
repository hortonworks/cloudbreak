package com.sequenceiq.cloudbreak.service.notification;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakV4Event;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.notification.Notification;

@Component
public class NotificationAssemblingService<T> {
    public Notification<T> createNotification(T notification) {
        return new Notification(notification);
    }

    public static CloudbreakV4Event cloudbreakEvent(Object payload, NotificationEventType eventType, WorkspaceResource resource) {
        CloudbreakV4Event event = new CloudbreakV4Event();
        event.setEventType(eventType);
        event.setTimestamp(System.currentTimeMillis());
        event.setResourceName(resource.getShortName());
        event.setPayload(payload);
        return event;
    }
}
