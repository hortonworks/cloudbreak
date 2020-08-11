package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPStructuredNotificationEvent extends CDPStructuredEvent {

    private CDPStructuredNotificationDetails notificationDetails;

    public CDPStructuredNotificationEvent() {
        super(CDPStructuredNotificationEvent.class.getSimpleName());
    }

    public CDPStructuredNotificationEvent(CDPOperationDetails operation, CDPStructuredNotificationDetails notificationDetails) {
        super(CDPStructuredNotificationEvent.class.getSimpleName(), operation);
        this.notificationDetails = notificationDetails;
    }

    @Override
    public String getStatus() {
        return SENT;
    }

    @Override
    public Long getDuration() {
        return ZERO;
    }

    public CDPStructuredNotificationDetails getNotificationDetails() {
        return notificationDetails;
    }

    public void setNotificationDetails(CDPStructuredNotificationDetails notificationDetails) {
        this.notificationDetails = notificationDetails;
    }
}
