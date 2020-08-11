package com.sequenceiq.cloudbreak.structuredevent.event.cdp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsNotificationDetails;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPStructuredNotificationEvent extends CDPStructuredEvent {

    private LdapNotificationDetails ldapNotificationDetails;

    private RdsNotificationDetails rdsNotificationDetails;

    public CDPStructuredNotificationEvent() {
        super(CDPStructuredNotificationEvent.class.getSimpleName());
    }

    public CDPStructuredNotificationEvent(CDPOperationDetails operation, NotificationDetails notificationDetails) {
        super(CDPStructuredNotificationEvent.class.getSimpleName(), operation);
        ldapNotificationDetails = null;
        rdsNotificationDetails = null;
    }

    public CDPStructuredNotificationEvent(CDPOperationDetails operation, LdapNotificationDetails ldapNotificationDetails) {
        super(CDPStructuredNotificationEvent.class.getSimpleName(), operation);
        this.ldapNotificationDetails = ldapNotificationDetails;
        rdsNotificationDetails = null;
    }

    public CDPStructuredNotificationEvent(CDPOperationDetails operation, RdsNotificationDetails rdsNotificationDetails) {
        super(CDPStructuredNotificationEvent.class.getSimpleName(), operation);
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
