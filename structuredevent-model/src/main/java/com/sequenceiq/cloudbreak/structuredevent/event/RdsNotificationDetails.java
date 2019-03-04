package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RdsNotificationDetails implements Serializable {
    private String notificationType;

    private String notification;

    private RdsDetails rdsDetails;

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

    public RdsDetails getRdsDetails() {
        return rdsDetails;
    }

    public void setRdsDetails(RdsDetails rdsDetails) {
        this.rdsDetails = rdsDetails;
    }
}
