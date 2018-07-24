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

    @Override
    public String getStatus() {
        return "SENT";
    }

    @Override
    public Long getDuration() {
        return null;
    }

    public void setNotificationDetails(NotificationDetails notificationDetails) {
        this.notificationDetails = notificationDetails;
    }

    public NotificationDetails getNotificationDetails() {
        return notificationDetails;
    }
}
