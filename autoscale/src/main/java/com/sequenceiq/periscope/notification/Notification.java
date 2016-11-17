package com.sequenceiq.periscope.notification;

import java.util.Date;

import com.sequenceiq.periscope.domain.History;

public class Notification {

    private String eventType;

    private Date eventTimestamp;

    private History historyRecord;

    private String eventMessage;

    private String owner;

    public Notification() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Date eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public History getHistoryRecord() {
        return historyRecord;
    }

    public void setHistoryRecord(History historyRecord) {
        this.historyRecord = historyRecord;
    }

    public String getEventMessage() {
        return eventMessage;
    }

    public void setEventMessage(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
