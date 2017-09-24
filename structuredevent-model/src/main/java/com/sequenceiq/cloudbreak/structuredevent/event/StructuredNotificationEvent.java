package com.sequenceiq.cloudbreak.structuredevent.event;

public class StructuredNotificationEvent extends StructuredEvent {
    private NotificationDetails notificationDetails;

    private StructuredNotificationEvent() {
    }

    public StructuredNotificationEvent(OperationDetails operation, NotificationDetails notificationDetails) {
        super(StructuredNotificationEvent.class.getSimpleName(), operation);
        this.notificationDetails = notificationDetails;
    }

    public NotificationDetails getNotificationDetails() {
        return notificationDetails;
    }
}
