package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredNotificationEvent extends StructuredEvent {
    private NotificationDetails notificationDetails;

    public StructuredNotificationEvent() {
    }

    public StructuredNotificationEvent(OperationDetails operation, NotificationDetails notificationDetails) {
        super(StructuredNotificationEvent.class.getSimpleName(), operation);
        this.notificationDetails = notificationDetails;
    }

    public void setNotificationDetails(NotificationDetails notificationDetails) {
        this.notificationDetails = notificationDetails;
    }

    public NotificationDetails getNotificationDetails() {
        return notificationDetails;
    }
}
