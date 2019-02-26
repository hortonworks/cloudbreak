package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredNotificationEvent extends StructuredEvent {
    private NotificationDetails notificationDetails;

    private LdapNotificationDetails ldapNotificationDetails;

    private RdsNotificationDetails rdsNotificationDetails;

    public StructuredNotificationEvent() {
        super(StructuredNotificationEvent.class.getSimpleName());
    }

    public StructuredNotificationEvent(OperationDetails operation, NotificationDetails notificationDetails) {
        super(StructuredNotificationEvent.class.getSimpleName(), operation);
        this.notificationDetails = notificationDetails;
        ldapNotificationDetails = null;
        rdsNotificationDetails = null;
    }

    public StructuredNotificationEvent(OperationDetails operation, LdapNotificationDetails ldapNotificationDetails) {
        super(StructuredNotificationEvent.class.getSimpleName(), operation);
        notificationDetails = null;
        this.ldapNotificationDetails = ldapNotificationDetails;
        rdsNotificationDetails = null;
    }

    public StructuredNotificationEvent(OperationDetails operation, RdsNotificationDetails rdsNotificationDetails) {
        super(StructuredNotificationEvent.class.getSimpleName(), operation);
        notificationDetails = null;
        ldapNotificationDetails = null;
        this.rdsNotificationDetails = rdsNotificationDetails;
    }

    @Override
    public String getStatus() {
        return "SENT";
    }

    @Override
    public Long getDuration() {
        return 0L;
    }

    public void setNotificationDetails(NotificationDetails notificationDetails) {
        this.notificationDetails = notificationDetails;
    }

    public NotificationDetails getNotificationDetails() {
        return notificationDetails;
    }

    public LdapNotificationDetails getLdapNotificationDetails() {
        return ldapNotificationDetails;
    }

    public void setLdapNotificationDetails(LdapNotificationDetails ldapNotificationDetails) {
        this.ldapNotificationDetails = ldapNotificationDetails;
    }

    public RdsNotificationDetails getRdsNotificationDetails() {
        return rdsNotificationDetails;
    }

    public void setRdsNotificationDetails(RdsNotificationDetails rdsNotificationDetails) {
        this.rdsNotificationDetails = rdsNotificationDetails;
    }
}
