package com.sequenceiq.periscope.notification;

import java.util.Date;

import com.sequenceiq.periscope.domain.History;

public class Notification {

    private Long workspaceId;

    private String eventType;

    private Date eventTimestamp;

    private History historyRecord;

    private String eventMessage;

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
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
}
