package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapNotificationDetails implements Serializable {
    private String notificationType;

    private String notification;

    private LdapDetails ldapDetails;

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public LdapDetails getLdapDetails() {
        return ldapDetails;
    }

    public void setLdapDetails(LdapDetails ldapDetails) {
        this.ldapDetails = ldapDetails;
    }
}
